package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ClubRepository
import com.example.data.repository.FirebaseAuthRepository
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.FunctionsRepository
import com.example.ui.language.Language
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ClubViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ClubRepository(AppDatabase.getDatabase(application))
    private val firestoreRepository = FirestoreRepository()
    val firebaseAuthRepository = FirebaseAuthRepository()
    // Everything that used to be a direct, client-trusted Firestore write (roles,
    // payment status, complaint resolution) now goes through here instead — see
    // FunctionsRepository + functions/index.js for the server-side enforcement.
    private val functionsRepository = FunctionsRepository()

    val language = MutableStateFlow(Language.BN)
    val isDarkTheme = MutableStateFlow(false)
    val currentUser = MutableStateFlow<UserEntity?>(null)

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    private val _isUsersOfflineCached = MutableStateFlow(false)
    val isUsersOfflineCached: StateFlow<Boolean> = _isUsersOfflineCached.asStateFlow()
    
    val allUsers: StateFlow<List<UserEntity>> = combine(
        firestoreRepository.getPublicUsersStream().catch { emit(emptyList()) },
        repository.allUsers
    ) { firestoreUsers, roomUsers ->
        if (firestoreUsers.isNotEmpty()) {
            _isUsersOfflineCached.value = false
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.cacheUsers(firestoreUsers)
            }
            firestoreUsers
        } else {
            if (roomUsers.isNotEmpty()) {
                _isUsersOfflineCached.value = true
            }
            roomUsers
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAnnouncements: StateFlow<List<AnnouncementEntity>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<ActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allComplaints: StateFlow<List<ComplaintEntity>> = repository.allComplaints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFinancials: StateFlow<List<FinancialRecordEntity>> = combine(
        firestoreRepository.getAllFinancialsStream().catch { emit(emptyList()) },
        repository.allFinancials
    ) { firestoreFinancials, roomFinancials ->
        if (firestoreFinancials.isNotEmpty()) {
            val map = LinkedHashMap<Long, FinancialRecordEntity>()
            roomFinancials.forEach { map[it.id] = it }
            firestoreFinancials.forEach { map[it.id] = it }
            map.values.toList()
        } else {
            roomFinancials
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivityLogs: StateFlow<List<ActivityLogEntity>> = combine(
        firestoreRepository.getActivityLogsStream().catch { emit(emptyList()) },
        repository.allActivityLogs
    ) { firestoreLogs, roomLogs ->
        if (firestoreLogs.isNotEmpty()) firestoreLogs else roomLogs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Financials for Current User
    val userFinancials: StateFlow<List<FinancialRecordEntity>> = combine(allFinancials, currentUser) { list, user ->
        if (user == null) emptyList()
        else list.filter { it.userId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Complaints for Current User
    val userComplaints: StateFlow<List<ComplaintEntity>> = combine(allComplaints, currentUser) { list, user ->
        if (user == null) emptyList()
        else if (com.example.data.model.Roles.isAdminLevel(user.role)) list
        else list.filter { it.userId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Restore Firebase Auth session on cold start safely.
        try {
            val firebaseAuth = FirebaseAuth.getInstance()
            val existingFirebaseUser = firebaseAuth.currentUser
            if (existingFirebaseUser != null) {
                viewModelScope.launch {
                    val cachedUser = repository.getUserByFirebaseUid(existingFirebaseUser.uid)
                        ?: allUsers.value.firstOrNull { it.firebaseUid == existingFirebaseUser.uid }
                    if (cachedUser != null) {
                        currentUser.value = cachedUser
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleLanguage() {
        language.value = if (language.value == Language.EN) Language.BN else Language.EN
    }

    fun setLanguage(lang: Language) {
        language.value = lang
    }

    fun selectUser(user: UserEntity) {
        currentUser.value = user
    }

    /**
     * Called after Firebase Phone Auth succeeds on the client side.
     * Resolves the resident profile by Firebase UID from cache → in-memory StateFlow
     * → Firestore directly (for first logins before the snapshot loads).
     * This never fabricates a local user with mock data — if no resident profile
     * exists yet, the user is told to complete registration via registerResident.
     */
    fun loginWithFirebaseUser(
        firebaseUser: FirebaseUser,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val resolved = repository.getUserByFirebaseUid(firebaseUser.uid)
                ?: allUsers.value.firstOrNull { it.firebaseUid == firebaseUser.uid }
                ?: firestoreRepository.getUserByFirebaseUid(firebaseUser.uid)

            if (resolved != null) {
                currentUser.value = resolved
                repository.cacheUsers(listOf(resolved))
                onResult(true, null)
            } else {
                // Phone-authed Firebase account with no resident profile yet.
                // Direct to email registration to create a proper Firestore profile.
                onResult(false, "No resident profile found. Please register with email/password to complete your profile.")
            }
        }
    }

    fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            firebaseAuthRepository.signInWithCredential(credential)
                .onSuccess { fbUser ->
                    if (fbUser != null) {
                        loginWithFirebaseUser(fbUser, onResult)
                    } else {
                        onResult(false, "Phone authentication returned no user profile")
                    }
                }
                .onFailure { e ->
                    onResult(false, e.localizedMessage ?: "Phone authentication failed")
                }
        }
    }

    fun signInWithPhoneOtp(
        verificationId: String,
        otpCode: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            firebaseAuthRepository.signInWithOtp(verificationId, otpCode)
                .onSuccess { fbUser ->
                    if (fbUser != null) {
                        loginWithFirebaseUser(fbUser, onResult)
                    } else {
                        onResult(false, "Phone authentication returned no user profile")
                    }
                }
                .onFailure { e ->
                    onResult(false, e.localizedMessage ?: "Invalid or expired OTP code")
                }
        }
    }

    /**
     * Email/password login. The resident profile is resolved by an EXACT match on
     * Firebase UID (`firebaseUid`) — never by comparing names/emails as substrings,
     * which could silently attach a stranger's address/NID/phone to the wrong login.
     * Role/membershipStatus always come from whatever's already stored for that UID;
     * this function never sets or overrides them.
     */
    fun loginWithEmail(
        email: String,
        pass: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authResult = FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, pass)
                    .await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // 1. Try Room cache first (fast, offline-capable)
                    val existingUser = repository.getUserByFirebaseUid(firebaseUser.uid)
                        // 2. Try in-memory StateFlow (already loaded from Firestore snapshot)
                        ?: allUsers.value.firstOrNull { it.firebaseUid == firebaseUser.uid }
                        // 3. Query Firestore directly (necessary on first login before snapshot loads)
                        ?: firestoreRepository.getUserByFirebaseUid(firebaseUser.uid)

                    if (existingUser != null) {
                        currentUser.value = existingUser
                        // Cache to Room so future logins work offline
                        repository.cacheUsers(listOf(existingUser))
                        onResult(true, null)
                    } else {
                        // Signed in with Firebase Auth but no resident profile exists yet —
                        // this account needs to go through registerResident (server-side,
                        // always creates "New Member" / "Pending") rather than being silently
                        // synthesized here with a client-chosen role.
                        onResult(false, "No resident profile found for this account. Please complete registration.")
                    }
                } else {
                    onResult(false, "Authentication returned empty user profile")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Firebase Authentication failed")
            }
        }
    }

    /**
     * Registration is two steps: (1) create the Firebase Auth account client-side as
     * before, then (2) call the registerResident Cloud Function to create the resident
     * profile — that function hardcodes role="NEW_MEMBER" and membershipStatus="Pending"
     * server-side. There is no `selectedRole` parameter here anymore: it doesn't exist
     * client-side because it was the entire vulnerability (see functions/index.js).
     */
    fun registerWithEmail(
        email: String,
        pass: String,
        name: String,
        phone: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authResult = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, pass)
                    .await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val profileChange = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    firebaseUser.updateProfile(profileChange).await()

                    when (val outcome = functionsRepository.registerResident(
                        nameEn = name,
                        phone = phone.ifBlank { email },
                        holding = "",
                        road = "",
                        block = ""
                    )) {
                        is FunctionsRepository.Outcome.Success -> {
                            // Firestore listener (allUsers stream) will pick up the new
                            // profile shortly; poll the local cache once it's synced.
                            onResult(true, null)
                        }
                        is FunctionsRepository.Outcome.Failure -> {
                            onResult(false, outcome.message)
                        }
                    }
                } else {
                    onResult(false, "Failed to create Firebase user account")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentUser.value = null
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    /**
     * Creates a real Pending donation/payment record server-side (no more fabricated
     * "Completed" status or txId). Callers should immediately follow up with
     * initiatePipraPayCheckout(financialRecordId, ...) to actually collect the payment —
     * this function only creates the record, it never marks anything paid.
     */
    fun processPayment(
        titleEn: String,
        titleBn: String,
        amount: Double,
        purpose: String,
        onResult: (financialRecordId: String?, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val outcome = functionsRepository.createPendingDonation(titleEn, titleBn, amount, purpose)) {
                is FunctionsRepository.Outcome.Success -> onResult(outcome.value, null)
                is FunctionsRepository.Outcome.Failure -> onResult(null, outcome.message)
            }
        }
    }

    fun initiateBkashCheckout(
        financialRecordFirestoreId: String,
        callbackUrl: String,
        onResult: (bkashUrl: String?, paymentId: String?, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val outcome = functionsRepository.initiateBkashPayment(financialRecordFirestoreId, callbackUrl)) {
                is FunctionsRepository.Outcome.Success -> {
                    val (paymentId, bkashUrl) = outcome.value
                    onResult(bkashUrl, paymentId, null)
                }
                is FunctionsRepository.Outcome.Failure -> onResult(null, null, outcome.message)
            }
        }
    }

    fun confirmBkashCheckout(
        financialRecordFirestoreId: String,
        paymentId: String,
        onResult: (success: Boolean, statusOrError: String) -> Unit
    ) {
        viewModelScope.launch {
            when (val outcome = functionsRepository.executeBkashPayment(financialRecordFirestoreId, paymentId)) {
                is FunctionsRepository.Outcome.Success -> {
                    val (status, trxId) = outcome.value
                    onResult(status == "Completed", trxId ?: status ?: "Unknown")
                }
                is FunctionsRepository.Outcome.Failure -> onResult(false, outcome.message)
            }
        }
    }

    /**
     * PipraPay — recommended default gateway (self-hosted, no bKash merchant agreement
     * required). Opens a checkout session; the UI should launch checkoutUrl in a Custom
     * Tab / WebView. Completion is confirmed server-side via the webhook first, with
     * confirmPipraPayCheckout below as a redirect-flow fallback — never assume success
     * just because this call returned a URL.
     */
    fun initiatePipraPayCheckout(
        financialRecordFirestoreId: String,
        redirectUrl: String,
        cancelUrl: String,
        onResult: (checkoutUrl: String?, ppId: String?, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val outcome = functionsRepository.initiatePipraPayCharge(financialRecordFirestoreId, redirectUrl, cancelUrl)) {
                is FunctionsRepository.Outcome.Success -> {
                    val (checkoutUrl, ppId) = outcome.value
                    onResult(checkoutUrl, ppId, null)
                }
                is FunctionsRepository.Outcome.Failure -> onResult(null, null, outcome.message)
            }
        }
    }

    /**
     * Fallback confirmation for the PipraPay redirect flow. The webhook (server-to-server)
     * is the primary path and usually resolves first; this exists for when the resident's
     * connection drops before the webhook lands. Still independently re-verified with
     * PipraPay's own API server-side — see functions/index.js confirmPipraPayPayment.
     */
    fun confirmPipraPayCheckout(
        financialRecordFirestoreId: String,
        ppId: String,
        onResult: (success: Boolean, statusOrError: String) -> Unit
    ) {
        viewModelScope.launch {
            when (val outcome = functionsRepository.confirmPipraPayPayment(financialRecordFirestoreId, ppId)) {
                is FunctionsRepository.Outcome.Success -> {
                    val (success, status) = outcome.value
                    onResult(success, status ?: "Unknown")
                }
                is FunctionsRepository.Outcome.Failure -> onResult(false, outcome.message)
            }
        }
    }

    fun submitComplaint(
        titleEn: String,
        titleBn: String,
        categoryEn: String,
        categoryBn: String,
        descEn: String,
        descBn: String,
        imageUrl: String
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val complaint = ComplaintEntity(
                userId = user.id,
                userNameEn = user.nameEn,
                userNameBn = user.nameBn,
                holdingNo = "${user.holding}, ${user.road}",
                titleEn = titleEn,
                titleBn = titleBn,
                categoryEn = categoryEn,
                categoryBn = categoryBn,
                descriptionEn = descEn,
                descriptionBn = descBn,
                imageUrl = imageUrl,
                status = "Pending",
                adminNoteEn = "",
                adminNoteBn = "",
                createdAt = "2026-07-25 14:00",
                updatedAt = "2026-07-25 14:00"
            )
            repository.insertComplaint(complaint)
        }
    }

    fun approveUserMembership(userId: String) {
        viewModelScope.launch {
            val outcome = functionsRepository.approveMembership(userId, "Active")
            if (outcome is FunctionsRepository.Outcome.Failure) return@launch
            // Local cache mirrors the server result; not the source of truth.
            repository.updateMembershipStatus(userId, "Active")

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "MEMBER_APPROVAL",
                adminId = currentUser.value?.id ?: "ADM-001",
                adminName = currentUser.value?.nameEn ?: "Membership Committee",
                titleEn = "Approved Resident Member ($userId)",
                titleBn = "সদস্য আবেদন অনুমোদন প্রদান ($userId)",
                detailsEn = "Verified membership documents and activated full resident privileges.",
                detailsBn = "সদস্যপদের নথিপত্র যাচাইকরণ শেষে সক্রিয় নাগরিক সুবিধা চালু করা হয়েছে।",
                timestamp = "2026-07-25 16:05",
                targetId = userId
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }

    fun updateComplaintStatus(complaint: ComplaintEntity, status: String, noteEn: String, noteBn: String) {
        viewModelScope.launch {
            if (complaint.firestoreId.isBlank()) return@launch
            val now = "2026-07-25 16:05"
            val outcome = functionsRepository.updateComplaintStatus(complaint.firestoreId, status, noteEn, noteBn)
            if (outcome is FunctionsRepository.Outcome.Failure) return@launch
            repository.updateComplaintStatus(complaint.firestoreId, status, noteEn, noteBn, now)

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "COMPLAINT_UPDATE",
                adminId = currentUser.value?.id ?: "ADM-001",
                adminName = currentUser.value?.nameEn ?: "Maintenance Admin",
                titleEn = "Updated Complaint #${complaint.id} to $status",
                titleBn = "অভিযোগ #${complaint.id} এর স্ট্যাটাস $status করা হয়েছে",
                detailsEn = "Admin Remark: ${if (noteEn.isBlank()) "Status changed to $status" else noteEn}",
                detailsBn = "অ্যাডমিন নোট: ${if (noteBn.isBlank()) "স্ট্যাটাস পরিবর্তন: $status" else noteBn}",
                timestamp = now,
                targetId = "CMP-${complaint.id}"
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }

    fun publishNotice(
        titleEn: String,
        titleBn: String,
        descEn: String,
        descBn: String,
        categoryEn: String,
        categoryBn: String,
        priority: String
    ) {
        viewModelScope.launch {
            val now = "2026-07-25 16:05"
            val notice = AnnouncementEntity(
                titleEn = titleEn,
                titleBn = titleBn,
                descriptionEn = descEn,
                descriptionBn = descBn,
                categoryEn = categoryEn,
                categoryBn = categoryBn,
                date = "2026-07-25",
                priority = priority
            )
            repository.insertAnnouncement(notice)
            firestoreRepository.addAnnouncement(notice)

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "NOTICE_CREATION",
                adminId = currentUser.value?.id ?: "ADM-001",
                adminName = currentUser.value?.nameEn ?: "Club Executive Committee",
                titleEn = "Published Notice: $titleEn",
                titleBn = "বিজ্ঞপ্তি প্রকাশ: $titleBn",
                detailsEn = "Category: $categoryEn • Priority: $priority • Content: $descEn",
                detailsBn = "ক্যাটাগরি: $categoryBn • অগ্রাধিকার: $priority • বিষয়বস্তু: $descBn",
                timestamp = now,
                targetId = "NOTICE-${System.currentTimeMillis().toString().takeLast(4)}"
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
        }
    }

    fun recordFinancialAdjustment(
        targetUserId: String,
        titleEn: String,
        titleBn: String,
        amount: Double,
        adjustmentType: String,
        noteEn: String,
        noteBn: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val now = "2026-07-25 16:05"

            // financials/{} denies ALL direct client writes (see firestore.rules) —
            // this now goes through the admin-gated recordFinancialAdjustment Cloud
            // Function, which is the only thing allowed to create the doc.
            val outcome = functionsRepository.recordFinancialAdjustment(
                targetUserId = targetUserId,
                titleEn = titleEn,
                titleBn = titleBn,
                amount = amount,
                adjustmentType = adjustmentType,
                noteEn = noteEn,
                noteBn = noteBn
            )
            val recordId = when (outcome) {
                is FunctionsRepository.Outcome.Success -> outcome.value
                is FunctionsRepository.Outcome.Failure -> {
                    onResult(false, outcome.message)
                    return@launch
                }
            }

            // Mirror into the local cache for offline viewing; the Cloud Function
            // write above (Admin SDK) is the source of truth.
            val record = FinancialRecordEntity(
                firestoreId = recordId ?: "",
                userId = targetUserId,
                titleEn = titleEn,
                titleBn = titleBn,
                amount = amount,
                type = "Adjustment",
                monthYear = "July 2026",
                date = "2026-07-25",
                paymentGateway = "Admin Adjustment",
                transactionId = "ADJ${System.currentTimeMillis().toString().takeLast(6)}",
                status = "Completed"
            )
            repository.insertFinancialRecord(record)

            val log = ActivityLogEntity(
                id = "LOG-${System.currentTimeMillis()}",
                actionType = "FINANCIAL_ADJUSTMENT",
                adminId = currentUser.value?.id ?: "ADM-001",
                adminName = currentUser.value?.nameEn ?: "Club Treasurer",
                titleEn = "Financial Adjustment: $titleEn (৳ ${amount.toInt()})",
                titleBn = "আর্থিক সমন্বয়: $titleBn (৳ ${amount.toInt()})",
                detailsEn = "Adjustment Type: $adjustmentType • Admin Note: $noteEn",
                detailsBn = "সমন্বয়ের ধরন: $adjustmentType • বিবরণ: $noteBn",
                timestamp = now,
                targetId = targetUserId
            )
            repository.insertActivityLog(log)
            firestoreRepository.addActivityLog(log)
            onResult(true, null)
        }
    }

    /**
     * Sign in using a PhoneAuthCredential (e.g. from instant SIM auto-verification).
     * On success resolves the resident profile by Firebase UID.
     */
    fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = firebaseAuthRepository.signInWithCredential(credential)
                val firebaseUser = result.getOrNull()
                if (firebaseUser == null) {
                    onResult(false, result.exceptionOrNull()?.message ?: "Sign-in failed")
                    return@launch
                }
                val profile = repository.getUserByFirebaseUid(firebaseUser.uid)
                    ?: allUsers.value.firstOrNull { it.firebaseUid == firebaseUser.uid }
                if (profile != null) {
                    currentUser.value = profile
                    onResult(true, null)
                } else {
                    onResult(false, "No resident profile found for this phone number.")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Sign-in failed")
            }
        }
    }

    /**
     * Sign in using the verificationId + 6-digit OTP code the user typed.
     * On success resolves the resident profile by Firebase UID.
     */
    fun signInWithPhoneOtp(
        verificationId: String,
        code: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = firebaseAuthRepository.signInWithOtp(verificationId, code)
                val firebaseUser = result.getOrNull()
                if (firebaseUser == null) {
                    onResult(false, result.exceptionOrNull()?.message ?: "OTP sign-in failed")
                    return@launch
                }
                val profile = repository.getUserByFirebaseUid(firebaseUser.uid)
                    ?: allUsers.value.firstOrNull { it.firebaseUid == firebaseUser.uid }
                if (profile != null) {
                    currentUser.value = profile
                    onResult(true, null)
                } else {
                    onResult(false, "No resident profile found for this phone number.")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "OTP sign-in failed")
            }
        }
    }
}
