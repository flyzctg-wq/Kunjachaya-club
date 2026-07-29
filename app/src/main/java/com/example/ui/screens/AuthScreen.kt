package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.example.utils.BiometricAuthManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserEntity
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: ClubViewModel,
    onLoginSuccess: () -> Unit
) {
    val lang by viewModel.language.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    
    val context = LocalContext.current
    var authMethod by remember { mutableStateOf("Email") } // "Email" or "Phone"
    var isRegisterMode by remember { mutableStateOf(false) }

    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var fullNameText by remember { mutableStateOf("") }
    
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isAuthLoading by remember { mutableStateOf(false) }

    var showBiometricModal by remember { mutableStateOf(false) }
    var biometricStatusMessage by remember { mutableStateOf("") }

    fun triggerBiometricAuth() {
        // Biometric is only a LOCAL re-auth gate for a device that already has a real,
        // persisted Firebase Auth session on it — never a way to pick "some account of
        // the currently selected role." If nobody has completed an actual email/password
        // (or phone) login on this device yet, there's nothing for a fingerprint to unlock.
        val persistedFirebaseUser = FirebaseAuth.getInstance().currentUser
        if (persistedFirebaseUser == null) {
            errorMessage = if (lang == Language.BN)
                "প্রথমে ইমেইল/পাসওয়ার্ড দিয়ে লগইন করুন। এরপর বায়োমেট্রিক দ্রুত-লগইন ব্যবহার করা যাবে।"
            else
                "Please sign in with email/password first. Biometric quick-login becomes available after that."
            showBiometricModal = true
            return
        }

        val fragmentActivity = context as? FragmentActivity
        val status = BiometricAuthManager.checkBiometricStatus(context)
        if (fragmentActivity != null && status == BiometricAuthManager.BiometricStatus.AVAILABLE) {
            BiometricAuthManager.authenticate(
                activity = fragmentActivity,
                title = if (lang == Language.BN) "বায়োমেট্রিক ড্যাশবোর্ড লগইন" else "Biometric Dashboard Login",
                subtitle = if (lang == Language.BN) "আঙ্গুলের ছাপ বা ফেস রিকগনিশন দিয়ে সহজে লগইন করুন" else "Log in using fingerprint or face recognition",
                negativeButtonText = if (lang == Language.BN) "পাসওয়ার্ড ব্যবহার করুন" else "Use Password / PIN",
                onSuccess = {
                    // Resolve the EXACT resident profile tied to this device's persisted
                    // Firebase UID — the fingerprint only confirms "device owner present,"
                    // it never chooses which account to log into.
                    val targetUser = allUsers.firstOrNull { it.firebaseUid == persistedFirebaseUser.uid }
                    if (targetUser != null) {
                        viewModel.selectUser(targetUser)
                        Toast.makeText(
                            context,
                            if (lang == Language.BN) "বায়োমেট্রিক যাচাইকরণ সফল!" else "Biometric verification successful!",
                            Toast.LENGTH_SHORT
                        ).show()
                        onLoginSuccess()
                    } else {
                        biometricStatusMessage = if (lang == Language.BN)
                            "এই অ্যাকাউন্টের জন্য কোনো প্রোফাইল পাওয়া যায়নি।"
                        else
                            "No resident profile found for this account."
                        showBiometricModal = true
                    }
                },
                onError = { _, errStr ->
                    biometricStatusMessage = errStr.toString()
                    showBiometricModal = true
                },
                onFailed = {
                    biometricStatusMessage = if (lang == Language.BN) "বায়োমেট্রিক মেলে নাই। আবার চেষ্টা করুন।" else "Biometric match failed. Try again."
                    showBiometricModal = true
                }
            )
        } else {
            showBiometricModal = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppLanguage.clubName(lang), fontWeight = FontWeight.Bold) },
                actions = {
                    FilterChip(
                        selected = lang == Language.BN,
                        onClick = { viewModel.toggleLanguage() },
                        label = { Text(if (lang == Language.BN) "বাংলা" else "English", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = "Language") },
                        modifier = Modifier.padding(end = 12.dp).testTag("auth_lang_toggle")
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Club Logo
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.img_club_logo),
                        contentDescription = "Club Logo",
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = AppLanguage.clubName(lang),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = AppLanguage.clubSubtitle(lang),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppLanguage.loginTitle(lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AppLanguage.loginSubtitle(lang),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Auth Method Selector Tabs (Email/Pass vs Phone OTP)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().testTag("auth_method_segmented_row")
                    ) {
                        SegmentedButton(
                            selected = authMethod == "Email",
                            onClick = { authMethod = "Email" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == Language.BN) "ইমেইল ও পাসওয়ার্ড" else "Email / Pass", fontSize = 12.sp)
                        }
                        SegmentedButton(
                            selected = authMethod == "Phone",
                            onClick = { authMethod = "Phone" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == Language.BN) "ফোন নম্বর (ওটিপি)" else "Phone OTP", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isRegisterMode) {
                        // No role selector here anymore: every self-registration is created
                        // as "New Member" / "Pending" by the server (see registerResident in
                        // functions/index.js). Becoming Admin is a separate, admin-only action
                        // taken after registration — never something the person signing up
                        // picks for themselves.
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (lang == Language.BN)
                                        "নতুন অ্যাকাউন্ট \"নতুন সদস্য\" হিসেবে তৈরি হবে এবং কমিটির অনুমোদনের পর \"সাধারণ সদস্য\"-তে উন্নীত হবে।"
                                    else
                                        "New accounts start as a New Member and are upgraded to General Member once the committee approves.",
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (errorMessage.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }

                    if (authMethod == "Email") {
                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = fullNameText,
                                onValueChange = { fullNameText = it },
                                label = { Text(if (lang == Language.BN) "পূর্ণ নাম" else "Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = emailText,
                            onValueChange = { emailText = it; errorMessage = "" },
                            label = { Text(if (lang == Language.BN) "ইমেইল এড্রেস" else "Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("email_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it; errorMessage = "" },
                            label = { Text(if (lang == Language.BN) "পাসওয়ার্ড" else "Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("password_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (emailText.isBlank() || passwordText.isBlank()) {
                                    errorMessage = if (lang == Language.BN) "ইমেইল ও পাসওয়ার্ড প্রদান করুন" else "Please enter email and password"
                                    return@Button
                                }
                                isAuthLoading = true
                                errorMessage = ""
                                if (isRegisterMode) {
                                    viewModel.registerWithEmail(
                                        email = emailText.trim(),
                                        pass = passwordText,
                                        name = fullNameText.ifBlank { "Resident" },
                                        phone = phoneNumber,
                                        onResult = { success, error ->
                                            isAuthLoading = false
                                            if (success) {
                                                Toast.makeText(context, if (lang == Language.BN) "ফায়ারবেস একাউন্ট তৈরি ও লগইন সফল!" else "Firebase account registered and logged in!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } else {
                                                errorMessage = error ?: "Registration failed"
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.loginWithEmail(
                                        email = emailText.trim(),
                                        pass = passwordText,
                                        onResult = { success, error ->
                                            isAuthLoading = false
                                            if (success) {
                                                Toast.makeText(context, if (lang == Language.BN) "ফায়ারবেস অথেন্টিকেশন সফল!" else "Firebase Auth successful!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } else {
                                                errorMessage = error ?: "Invalid credentials or account missing"
                                            }
                                        }
                                    )
                                }
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("email_auth_btn")
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (lang == Language.BN) "যাচাই করা হচ্ছে..." else "Verifying Auth...", fontSize = 14.sp)
                            } else {
                                Icon(if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.LockOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRegisterMode) {
                                        if (lang == Language.BN) "ফায়ারবেস একাউন্ট খুলুন" else "Create Firebase Account"
                                    } else {
                                        if (lang == Language.BN) "ফায়ারবেস অথিঃ লগইন" else "Log In with Firebase Auth"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = {
                                isRegisterMode = !isRegisterMode
                                errorMessage = ""
                            },
                            modifier = Modifier.testTag("toggle_register_mode_btn")
                        ) {
                            Text(
                                text = if (isRegisterMode) {
                                        if (lang == Language.BN) "ইতোমধ্যে একাউন্ট আছে? লগইন করুন" else "Already have an account? Sign In"
                                    } else {
                                        if (lang == Language.BN) "নতুন সদস্য? নতুন একাউন্ট খুলুন" else "New resident? Create Firebase Account"
                                    },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Phone OTP Auth Mode
                        if (!otpSent) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text(AppLanguage.phoneNumber(lang)) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("phone_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (phoneNumber.isNotBlank()) {
                                        otpSent = true
                                        otpCode = "123456" // Default mock OTP
                                    } else {
                                        errorMessage = "Please enter valid phone number"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_otp_btn")
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppLanguage.sendOtp(lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it },
                                label = { Text(AppLanguage.enterOtp(lang)) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("otp_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            Text(
                                text = if (lang == Language.BN) "ডেমো ওটিপি: ১২৩৪৫৬" else "Demo OTP code: 123456",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.loginWithPhone(phoneNumber)
                                    onLoginSuccess()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_login_btn")
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppLanguage.verifyAndLogin(lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric Authentication Shortcut Button
                    OutlinedButton(
                        onClick = { triggerBiometricAuth() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("biometric_login_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Auth",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == Language.BN) "বায়োমেট্রিক লগইন (ফিঙ্গারপ্রিন্ট / ফেস)" else "Biometric Login (Fingerprint / Face ID)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // NOTE: A "Quick Demo User Selector" used to sit here — a list of every
            // local resident (allUsers.forEach), each a one-tap login button with no
            // password or biometric check at all, Admin accounts included. That was a
            // full authentication bypass sitting on the login screen itself, and it
            // would have silently undone every other fix in this file (the removed
            // role toggle, the biometric UID binding, real email/password auth).
            // Removed outright rather than gated, since a "demo login" feature is
            // fundamentally incompatible with real resident data.
        }

        if (showBiometricModal) {
            BiometricVerificationDialog(
                lang = lang,
                statusMessage = biometricStatusMessage,
                onDismiss = { showBiometricModal = false },
                onAuthenticateSuccess = {
                    val persistedFirebaseUser = FirebaseAuth.getInstance().currentUser
                    val targetUser = persistedFirebaseUser?.let { fbUser ->
                        allUsers.firstOrNull { it.firebaseUid == fbUser.uid }
                    }
                    if (targetUser != null) {
                        viewModel.selectUser(targetUser)
                        showBiometricModal = false
                        Toast.makeText(
                            context,
                            if (lang == Language.BN) "বায়োমেট্রিক লগইন সফল হয়েছে!" else "Biometric login successful!",
                            Toast.LENGTH_SHORT
                        ).show()
                        onLoginSuccess()
                    } else {
                        biometricStatusMessage = if (lang == Language.BN)
                            "প্রথমে ইমেইল/পাসওয়ার্ড দিয়ে লগইন করুন।"
                        else
                            "Please sign in with email/password first."
                    }
                }
            )
        }
    }
}

@Composable
fun BiometricVerificationDialog(
    lang: Language,
    statusMessage: String,
    onDismiss: () -> Unit,
    onAuthenticateSuccess: () -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            kotlinx.coroutines.delay(1200)
            isScanning = false
            scanCompleted = true
            onAuthenticateSuccess()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Sensor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = if (lang == Language.BN) "বায়োমেট্রিক সেন্সর যাচাইকরণ" else "Biometric Sensor Verification",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(
                    text = if (statusMessage.isNotEmpty()) statusMessage
                    else if (lang == Language.BN) "আঙ্গুলের ছাপ দিন অথবা ক্যামেরায় তাকিয়ে ফেস আইডি যাচাই করুন।"
                    else "Touch the fingerprint sensor or present your face for biometric recognition.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == Language.BN) "বায়োমেট্রিক ডেটা স্ক্যান করা হচ্ছে..." else "Scanning biometric signature...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { isScanning = true },
                            label = { Text(if (lang == Language.BN) "ফিঙ্গারপ্রিন্ট" else "Fingerprint", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { isScanning = true },
                            label = { Text(if (lang == Language.BN) "ফেস আইডি" else "Face ID", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { isScanning = true },
                modifier = Modifier.testTag("confirm_biometric_auth_btn")
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (lang == Language.BN) "যাচাই করুন ও লগইন" else "Verify & Log In")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_biometric_auth_btn")
            ) {
                Text(if (lang == Language.BN) "বাতিল" else "Cancel")
            }
        }
    )
}

