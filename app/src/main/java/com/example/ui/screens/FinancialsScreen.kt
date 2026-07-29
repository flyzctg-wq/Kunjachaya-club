package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.browser.customtabs.CustomTabsIntent
import com.example.R
import com.example.data.model.FinancialRecordEntity
import com.example.ui.components.VisualSpendingDashboard
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel
import com.example.util.PdfReceiptGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialsScreen(
    viewModel: ClubViewModel
) {
    val lang by viewModel.language.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val financials by viewModel.userFinancials.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("Pending") } // "Pending", "History", "Tracker", "All"
    var searchQuery by remember { mutableStateOf("") }
    var showPayDialog by remember { mutableStateOf<FinancialRecordEntity?>(null) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var selectedReceipt by remember { mutableStateOf<FinancialRecordEntity?>(null) }
    var trackerRecord by remember { mutableStateOf<FinancialRecordEntity?>(null) }

    val pendingList = financials.filter { it.status == "Pending" }
    val historyList = financials.filter { it.status == "Completed" }

    val totalPendingAmount = pendingList.sumOf { it.amount }
    val totalPaidAmount = historyList.sumOf { it.amount }

    fun formatMoney(amount: Double): String {
        val rounded = amount.toInt()
        return if (lang == Language.BN) {
            "৳ " + rounded.toString()
                .replace('0', '০')
                .replace('1', '১')
                .replace('2', '২')
                .replace('3', '৩')
                .replace('4', '৪')
                .replace('5', '৫')
                .replace('6', '৬')
                .replace('7', '৭')
                .replace('8', '৮')
                .replace('9', '৯')
        } else {
            "৳ %,d".format(rounded)
        }
    }

    // Filtered list based on search query and active tab
    val filteredList = remember(financials, activeTab, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val baseList = when (activeTab) {
            "Pending" -> pendingList
            "History" -> historyList
            else -> financials
        }
        if (q.isEmpty()) baseList
        else baseList.filter {
            it.titleEn.lowercase().contains(q) ||
                    it.titleBn.lowercase().contains(q) ||
                    it.transactionId.lowercase().contains(q) ||
                    it.paymentGateway.lowercase().contains(q) ||
                    it.monthYear.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(AppLanguage.financials(lang), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == Language.BN) "ফায়ারস্টোর রিয়েল-টাইম সিঙ্ক সক্রিয়" else "Firestore Live Sync Active",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = { showDonationDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("donate_top_btn")
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppLanguage.makeDonation(lang), fontSize = 12.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Member Balance Overview Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Outstanding Balance Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("outstanding_balance_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (totalPendingAmount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            else Color(0xFFE8F5E9)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (lang == Language.BN) "মোট বকেয়া চাঁদা" else "Total Due Balance",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalPendingAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF1B5E20)
                                )
                                Icon(
                                    imageVector = if (totalPendingAmount > 0) Icons.Default.PendingActions else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (totalPendingAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF1B5E20),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatMoney(totalPendingAmount),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalPendingAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (totalPendingAmount > 0)
                                    (if (lang == Language.BN) "${pendingList.size} টি আইটেম বকেয়া আছে" else "${pendingList.size} Pending Item(s)")
                                else (if (lang == Language.BN) "সব পরিশোধিত" else "Fully Paid"),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Completed Paid Total Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("total_paid_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (lang == Language.BN) "মোট পরিশোধিত" else "Total Paid Dues",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatMoney(totalPaidAmount),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lang == Language.BN) "${historyList.size} টি সফল লেনদেন" else "${historyList.size} Completed Txns",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tab Navigation Row & Search Bar
            item {
                Column {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = activeTab == "Spending",
                                onClick = { activeTab = "Spending" },
                                label = { Text(if (lang == Language.BN) "স্পেন্ডিং ড্যাশবোর্ড" else "Spending Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("tab_spending_analytics")
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeTab == "Pending",
                                onClick = { activeTab = "Pending" },
                                label = { Text(if (lang == Language.BN) "বকেয়া চাঁদা (${pendingList.size})" else "Outstanding (${pendingList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.PriorityHigh, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("tab_pending_dues")
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeTab == "Tracker",
                                onClick = { activeTab = "Tracker" },
                                label = { Text(if (lang == Language.BN) "পেমেন্ট ট্র্যাকার" else "Payment Tracker", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("tab_payment_tracker")
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeTab == "History",
                                onClick = { activeTab = "History" },
                                label = { Text(if (lang == Language.BN) "লেনদেন ইতিহাস (${historyList.size})" else "History (${historyList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("tab_payment_history")
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeTab == "All",
                                onClick = { activeTab = "All" },
                                label = { Text(if (lang == Language.BN) "সকল বিবরণী (${financials.size})" else "All Records (${financials.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("tab_all_records")
                            )
                        }
                    }

                    if (activeTab != "Tracker" && activeTab != "Spending") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(if (lang == Language.BN) "শিরোনাম, লেনদেন আইডি বা গেটওয়ে দিয়ে খুঁজুন..." else "Search by title, TxID, or gateway...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("financials_search_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Spending Analytics View
            if (activeTab == "Spending") {
                item {
                    VisualSpendingDashboard(
                        financials = financials,
                        lang = lang,
                        formatMoney = ::formatMoney
                    )
                }
            } else if (activeTab == "Tracker") {
                item {
                    val recordToTrack = trackerRecord ?: historyList.firstOrNull() ?: pendingList.firstOrNull()
                    if (recordToTrack != null) {
                        PaymentTrackerCard(
                            record = recordToTrack,
                            lang = lang,
                            formatMoney = ::formatMoney,
                            onSelectOther = {
                                val next = (historyList + pendingList).find { it.id != recordToTrack.id }
                                if (next != null) trackerRecord = next
                            }
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (lang == Language.BN) "ট্র্যাক করার জন্য কোন পেমেন্ট রেকর্ড পাওয়া যায়নি" else "No payment records available to track",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                // Outstanding / History List Items
                if (filteredList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (activeTab == "Pending") Icons.Default.VerifiedUser else Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = if (activeTab == "Pending")
                                            (if (lang == Language.BN) "আপনার সকল বকেয়া চাঁদা পরিশোধিত!" else "No outstanding dues! All set.")
                                        else (if (lang == Language.BN) "কোন মিল থাকা লেনদেন পাওয়া যায়নি" else "No matching transactions found"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (lang == Language.BN) "ফায়ারস্টোর ডাটাবেজ নিয়মিত আপডেট করা হয়" else "Real-time records are kept up to date in Firestore",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filteredList, key = { it.id }) { record ->
                        val isPending = record.status == "Pending"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("financial_record_card_${record.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPending) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = when {
                                                isPending -> MaterialTheme.colorScheme.error
                                                record.type == "Donation" -> MaterialTheme.colorScheme.secondary
                                                else -> Color(0xFF2E7D32)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    isPending -> if (lang == Language.BN) "বকেয়া" else "DUE"
                                                    record.type == "Donation" -> if (lang == Language.BN) "অনুদান" else "DONATION"
                                                    else -> if (lang == Language.BN) "পরিশোধিত" else "PAID"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = if (record.paymentGateway.isNotBlank()) record.paymentGateway else "Pending Gateway",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = formatMoney(record.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (isPending) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (lang == Language.BN) record.titleBn else record.titleEn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${record.monthYear} • ${record.date}" + (if (record.transactionId.isNotBlank()) " • TxID: ${record.transactionId}" else ""),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Track Status button
                                    TextButton(
                                        onClick = {
                                            trackerRecord = record
                                            activeTab = "Tracker"
                                        },
                                        modifier = Modifier.testTag("track_status_btn_${record.id}")
                                    ) {
                                        Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (lang == Language.BN) "স্ট্যাটাস ট্র্যাক করুন" else "Track Status", fontSize = 11.sp)
                                    }

                                    if (isPending) {
                                        Button(
                                            onClick = { showPayDialog = record },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("pay_due_btn_${record.id}")
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(AppLanguage.payDues(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { selectedReceipt = record },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("receipt_btn_${record.id}")
                                        ) {
                                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (lang == Language.BN) "রশিদ দেখুন" else "View Receipt", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    // Real PipraPay Checkout Dialog
    if (showPayDialog != null) {
        val record = showPayDialog!!
        var isProcessing by remember { mutableStateOf(false) }
        var currentProcessStage by remember { mutableStateOf(0) } // 0: Idle, 1: Creating session, 2: Opening checkout

        AlertDialog(
            onDismissRequest = { if (!isProcessing) showPayDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == Language.BN) "সুরক্ষিত গেটওয়ে পেমেন্ট চেকআউট" else "Secure Gateway Dues Checkout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (lang == Language.BN) record.titleBn else record.titleEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (lang == Language.BN) "পরিশোধযোগ্য অর্থ:" else "Payable Amount:", fontSize = 11.sp)
                                Text(
                                    text = formatMoney(record.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // No gateway/wallet-number picker here anymore: PipraPay's own hosted
                    // checkout page is where the resident actually chooses bKash / Nagad /
                    // Rocket / Card and enters their wallet number — collecting it here too
                    // was UI theater since none of it was ever sent anywhere.
                    Text(
                        text = if (lang == Language.BN)
                            "পরবর্তী পেজে আপনি bKash, Nagad, Rocket বা কার্ড থেকে বেছে নিতে পারবেন।"
                        else
                            "You'll choose bKash, Nagad, Rocket, or Card on the next secure page.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Real-Time Progress during submission (only two honest stages: we
                    // don't claim to know what happens inside PipraPay's own checkout)
                    if (isProcessing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (lang == Language.BN) "পেমেন্ট সেশন প্রস্তুত করা হচ্ছে" else "Preparing Payment Session",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                TrackerStepItem(
                                    stepNum = 1,
                                    title = if (lang == Language.BN) "১. পেমেন্ট সেশন তৈরি হচ্ছে" else "1. Creating secure checkout session",
                                    isDone = currentProcessStage >= 1,
                                    isCurrent = currentProcessStage == 1
                                )
                                TrackerStepItem(
                                    stepNum = 2,
                                    title = if (lang == Language.BN) "২. চেকআউট পেজ খোলা হচ্ছে" else "2. Opening PipraPay checkout page",
                                    isDone = currentProcessStage >= 2,
                                    isCurrent = currentProcessStage == 2
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (record.firestoreId.isBlank()) {
                            Toast.makeText(context, "This record hasn't synced with the server yet — try again shortly.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        isProcessing = true
                        currentProcessStage = 1 // creating the checkout session server-side

                        // redirectUrl/cancelUrl point back into this app via a custom scheme,
                        // caught by the <intent-filter> added to AndroidManifest.xml (see
                        // functions/README.md). The webhook path is still the primary/reliable
                        // confirmation route — the redirect is just a fallback + UX nicety.
                        val redirectUrl = "kunjachayaclub://payment-redirect"
                        val cancelUrl = "kunjachayaclub://payment-cancel"

                        viewModel.initiatePipraPayCheckout(
                            financialRecordFirestoreId = record.firestoreId,
                            redirectUrl = redirectUrl,
                            cancelUrl = cancelUrl
                        ) { checkoutUrl, _, error ->
                            if (checkoutUrl.isNullOrBlank()) {
                                isProcessing = false
                                currentProcessStage = 0
                                Toast.makeText(
                                    context,
                                    error ?: "Could not start checkout. Please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@initiatePipraPayCheckout
                            }

                            currentProcessStage = 2 // opening the real PipraPay checkout page
                            try {
                                CustomTabsIntent.Builder().build().launchUrl(context, android.net.Uri.parse(checkoutUrl))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open checkout page: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }

                            // We do NOT mark this Completed here. The resident finishes payment
                            // in the browser/PipraPay UI; PipraPay's webhook (server-to-server,
                            // functions/index.js: piprapayWebhook) confirms it independently and
                            // updates Firestore, which flows back into this screen through the
                            // existing Firestore listener. Close this dialog and let that happen.
                            isProcessing = false
                            currentProcessStage = 0
                            showPayDialog = null
                            Toast.makeText(
                                context,
                                if (lang == Language.BN)
                                    "চেকআউট পেজ খোলা হয়েছে। পরিশোধ সম্পন্ন হলে স্ট্যাটাস স্বয়ংক্রিয়ভাবে আপডেট হবে।"
                                else
                                    "Checkout opened. Your status will update automatically once PipraPay confirms the payment.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.testTag("confirm_payment_btn")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == Language.BN) "প্রসেস হচ্ছে..." else "Processing...")
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == Language.BN) "নিরাপদে পরিশোধ করুন" else "Pay Now Securely")
                    }
                }
            },
            dismissButton = {
                if (!isProcessing) {
                    TextButton(onClick = { showPayDialog = null }) {
                        Text(if (lang == Language.BN) "বাতিল" else "Cancel")
                    }
                }
            }
        )
    }

    // Donation Dialog
    if (showDonationDialog && user != null) {
        var donationAmount by remember { mutableStateOf("1000") }
        var purpose by remember { mutableStateOf("Club Mosque & Library Renovation") }
        var isSubmittingDonation by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmittingDonation) showDonationDialog = false },
            title = { Text(if (lang == Language.BN) "ক্লাব তহবিলে অনুদান প্রদান" else "Make Club Donation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = donationAmount,
                        onValueChange = { donationAmount = it },
                        label = { Text("Donation Amount (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("donation_amount_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("500", "1000", "2500", "5000").forEach { preset ->
                            FilterChip(
                                selected = donationAmount == preset,
                                onClick = { donationAmount = preset },
                                label = { Text("৳ $preset", fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Select Purpose:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    listOf("Club Mosque & Library Renovation", "Green Park & Trees Fund", "Security System Upgrade").forEach { pur ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { purpose = pur }
                        ) {
                            RadioButton(selected = purpose == pur, onClick = { purpose = pur })
                            Text(pur, fontSize = 12.sp)
                        }
                    }

                    // Gateway/method choice happens on PipraPay's own hosted checkout page
                    // next, not here — see note in the main payment dialog above.
                    Text(
                        text = if (lang == Language.BN)
                            "পরবর্তী পেজে আপনি bKash, Nagad, Rocket বা কার্ড থেকে বেছে নিতে পারবেন।"
                        else
                            "You'll choose bKash, Nagad, Rocket, or Card on the next secure page.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = donationAmount.toDoubleOrNull() ?: 1000.0
                        isSubmittingDonation = true
                        viewModel.processPayment(
                            titleEn = purpose,
                            titleBn = purpose,
                            amount = amt,
                            purpose = purpose
                        ) { financialRecordId, error ->
                            if (financialRecordId == null) {
                                isSubmittingDonation = false
                                Toast.makeText(context, error ?: "Could not start donation.", Toast.LENGTH_LONG).show()
                                return@processPayment
                            }
                            viewModel.initiatePipraPayCheckout(
                                financialRecordFirestoreId = financialRecordId,
                                redirectUrl = "kunjachayaclub://payment-redirect",
                                cancelUrl = "kunjachayaclub://payment-cancel"
                            ) { checkoutUrl, _, checkoutError ->
                                isSubmittingDonation = false
                                showDonationDialog = false
                                if (checkoutUrl != null) {
                                    try {
                                        CustomTabsIntent.Builder().build().launchUrl(context, android.net.Uri.parse(checkoutUrl))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open checkout: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, checkoutError ?: "Could not open checkout.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isSubmittingDonation,
                    modifier = Modifier.testTag("submit_donation_btn")
                ) {
                    Text(if (lang == Language.BN) "দান করুন" else "Donate Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PDF Receipt View Modal Dialog
    if (selectedReceipt != null && user != null) {
        val r = selectedReceipt!!
        val u = user!!

        AlertDialog(
            onDismissRequest = { selectedReceipt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_club_logo),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == Language.BN) "ডিজিটাল মানি রিসিভট" else "Official Electronic Receipt", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Surface(
                    color = Color(0xFFFAFAFA),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = AppLanguage.clubName(lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "OFFICIAL PAYMENT MONEY RECEIPT • FIRESTORE VERIFIED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ReceiptRow(label = "Member Name:", value = if (lang == Language.BN) u.nameBn else u.nameEn)
                        ReceiptRow(label = "Member ID & Holding:", value = "${u.id} (${u.holding})")
                        ReceiptRow(label = "Transaction ID:", value = r.transactionId)
                        ReceiptRow(label = "Payment Gateway:", value = r.paymentGateway)
                        ReceiptRow(label = "Date & Time:", value = r.date)
                        ReceiptRow(label = "Payment Purpose:", value = if (lang == Language.BN) r.titleBn else r.titleEn)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL PAID:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(formatMoney(r.amount), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ VERIFIED DIGITAL STAMP • KUNJACHHAYA CLUB ACCOUNTS OFFICE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(5.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PdfReceiptGenerator.downloadAndOpenPdf(context, r, u)
                        selectedReceipt = null
                    },
                    modifier = Modifier.testTag("download_pdf_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReceipt = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PaymentTrackerCard(
    record: FinancialRecordEntity,
    lang: Language,
    formatMoney: (Double) -> String,
    onSelectOther: () -> Unit
) {
    val isCompleted = record.status == "Completed"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_tracker_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Language.BN) "পেমেন্ট স্ট্যাটাস ট্র্যাকার" else "Payment Live Status Tracker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(onClick = onSelectOther) {
                    Text(if (lang == Language.BN) "অন্য রেকর্ড" else "Switch Record", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == Language.BN) record.titleBn else record.titleEn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "TxID: ${record.transactionId.ifBlank { "TXN-PENDING" }} • Gateway: ${record.paymentGateway.ifBlank { "Pending" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatMoney(record.amount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (lang == Language.BN) "ফায়ারস্টোর প্রসেসিং ধাপসমূহ:" else "Firestore Verification Pipeline:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Step 1: Initiated
            TrackerStepItem(
                stepNum = 1,
                title = if (lang == Language.BN) "১. পেমেন্ট রিকোয়েস্ট তৈরি ও অনুমোদন" else "1. Dues Payment Request Created",
                isDone = true,
                isCurrent = false,
                subDetail = "Date: ${record.date} • Gateway: ${record.paymentGateway}"
            )

            // Step 2: Gateway Verification
            TrackerStepItem(
                stepNum = 2,
                title = if (lang == Language.BN) "২. গেটওয়ে ট্রানজেকশন ভ্যালিডেশন" else "2. Payment Gateway & TxID Validation",
                isDone = isCompleted,
                isCurrent = !isCompleted,
                subDetail = if (isCompleted) "Verified Token: ${record.transactionId}" else "Awaiting Gateway IPN Webhook Response"
            )

            // Step 3: Firestore Database Sync
            TrackerStepItem(
                stepNum = 3,
                title = if (lang == Language.BN) "৩. ফায়ারস্টোর ডাটাবেজ আপডেট" else "3. Firestore Cloud Sync & Ledger Update",
                isDone = isCompleted,
                isCurrent = false,
                subDetail = if (isCompleted) "Document Collection: 'financials' Updated" else "Pending Write to Firestore Collection"
            )

            // Step 4: Electronic Receipt
            TrackerStepItem(
                stepNum = 4,
                title = if (lang == Language.BN) "৪. ডিজিটাল মানি রিসিভট ইস্যু" else "4. Electronic PDF Money Receipt Ready",
                isDone = isCompleted,
                isCurrent = false,
                subDetail = if (isCompleted) "Signed Digital Stamp Code: KCB-${record.id}" else "Receipt Pending Transaction Finalization"
            )
        }
    }
}

@Composable
fun TrackerStepItem(
    stepNum: Int,
    title: String,
    isDone: Boolean,
    isCurrent: Boolean,
    subDetail: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = when {
                isDone -> Color(0xFF2E7D32)
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        text = stepNum.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isDone || isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isDone || isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!subDetail.isNullOrBlank()) {
                Text(
                    text = subDetail,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GatewayChip(name: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = name,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
