package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ActivityLogEntity
import com.example.data.model.ComplaintEntity
import com.example.ui.components.AdminDashboardOverview
import com.example.ui.components.VisualSpendingDashboard
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.AdminDashboardViewModel
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    viewModel: ClubViewModel,
    adminDashboardViewModel: AdminDashboardViewModel = viewModel()
) {
    val lang by viewModel.language.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allComplaints by viewModel.allComplaints.collectAsState()
    val allActivityLogs by viewModel.allActivityLogs.collectAsState()
    val allFinancials by viewModel.allFinancials.collectAsState()

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
            "৳ $rounded"
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    var showPublishNoticeDialog by remember { mutableStateOf(false) }
    var showFinancialAdjustmentDialog by remember { mutableStateOf(false) }
    var selectedLogFilter by remember { mutableStateOf("All") }

    val pendingUsers = allUsers.filter { it.membershipStatus == "Pending" }

    val filteredActivityLogs = when (selectedLogFilter) {
        "Notices" -> allActivityLogs.filter { it.actionType == "NOTICE_CREATION" }
        "Complaints" -> allActivityLogs.filter { it.actionType == "COMPLAINT_UPDATE" }
        "Financials" -> allActivityLogs.filter { it.actionType == "FINANCIAL_ADJUSTMENT" }
        "Members" -> allActivityLogs.filter { it.actionType == "MEMBER_APPROVAL" }
        else -> allActivityLogs
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppLanguage.admin(lang), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showFinancialAdjustmentDialog = true },
                        modifier = Modifier.testTag("admin_financial_adj_btn")
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Financial Adjustment", tint = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { showPublishNoticeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 12.dp).testTag("publish_notice_btn")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == Language.BN) "বিজ্ঞপ্তি প্রকাশ" else "Publish Notice", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (lang == Language.BN) "ড্যাশবোর্ড ওভারভিউ" else "Dashboard Overview", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (lang == Language.BN) "আবেদনসমূহ (${pendingUsers.size})" else "Pending (${pendingUsers.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(if (lang == Language.BN) "অভিযোগ সমাধান" else "Complaints", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text(if (lang == Language.BN) "অডিট লগসমূহ (${allActivityLogs.size})" else "Audit Logs (${allActivityLogs.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text(if (lang == Language.BN) "স্পেন্ডিং ড্যাশবোর্ড" else "Spending Analytics", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Aggregated Metrics & Overview from ActivityLogs collection
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AdminDashboardOverview(
                                adminViewModel = adminDashboardViewModel,
                                lang = lang,
                                onNavigateToComplaintsTab = { selectedTab = 2 },
                                onNavigateToAuditLogsTab = { selectedTab = 3 }
                            )
                        }
                    }
                }
                1 -> {
                    // Pending Member Approvals
                    if (pendingUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (lang == Language.BN) "কোন পেন্ডিং সদস্য আবেদন নেই" else "No pending member applications", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(pendingUsers) { u ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("pending_user_${u.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = if (lang == Language.BN) u.nameBn else u.nameEn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "${u.professionEn} • ${u.holding}, ${u.road}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Phone: ${u.phone} • NID: ${u.nidFrontUrl}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { viewModel.approveUserMembership(u.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                modifier = Modifier.weight(1f).testTag("approve_btn_${u.id}")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (lang == Language.BN) "অনুমোদন করুন" else "Approve Member", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Complaints Management
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(allComplaints) { complaint ->
                            AdminComplaintItem(complaint = complaint, viewModel = viewModel, lang = lang)
                        }
                    }
                }
                3 -> {
                    // Audit Logs / ActivityLogs Collection Stream
                    Column {
                        // Filter chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            listOf(
                                "All" to if (lang == Language.BN) "সকল" else "All",
                                "Notices" to if (lang == Language.BN) "বিজ্ঞপ্তি" else "Notices",
                                "Complaints" to if (lang == Language.BN) "অভিযোগ" else "Complaints",
                                "Financials" to if (lang == Language.BN) "আর্থিক" else "Financials",
                                "Members" to if (lang == Language.BN) "সদস্য পদ" else "Members"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedLogFilter == key,
                                    onClick = { selectedLogFilter = key },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        if (filteredActivityLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (lang == Language.BN) "কোন অডিট লগ রেকর্ড পাওয়া যায়নি" else "No audit logs recorded yet", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(filteredActivityLogs) { log ->
                                    ActivityLogCard(log = log, lang = lang)
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Spending & Dues Analytics Visual Dashboard
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            VisualSpendingDashboard(
                                financials = allFinancials,
                                lang = lang,
                                formatMoney = ::formatMoney
                            )
                        }
                    }
                }
            }
        }
    }

    // Publish Notice Modal
    if (showPublishNoticeDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Urgent Notice") }

        AlertDialog(
            onDismissRequest = { showPublishNoticeDialog = false },
            title = { Text(if (lang == Language.BN) "নতুন বুলেটিন প্রকাশ করুন" else "Publish Announcement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth().testTag("notice_title_input")
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("notice_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            viewModel.publishNotice(
                                titleEn = title,
                                titleBn = title,
                                descEn = description,
                                descBn = description,
                                categoryEn = category,
                                categoryBn = category,
                                priority = "High"
                            )
                            showPublishNoticeDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_notice_btn")
                ) {
                    Text("Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishNoticeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Financial Adjustment Modal
    if (showFinancialAdjustmentDialog) {
        var targetUser by remember { mutableStateOf("USR-101") }
        var title by remember { mutableStateOf("Quarterly Security Fee Waiver") }
        var amountStr by remember { mutableStateOf("500") }
        var adjustmentType by remember { mutableStateOf("Fee Waiver") }
        var note by remember { mutableStateOf("Approved discount for active community service.") }

        AlertDialog(
            onDismissRequest = { showFinancialAdjustmentDialog = false },
            title = { Text(if (lang == Language.BN) "আর্থিক সমন্বয় নথিবদ্ধকরণ" else "Record Financial Adjustment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (lang == Language.BN) "রেসিডেন্ট আইডি:" else "Target Resident ID:")
                    OutlinedTextField(
                        value = targetUser,
                        onValueChange = { targetUser = it },
                        label = { Text("Resident User ID (e.g. USR-101)") },
                        modifier = Modifier.fillMaxWidth().testTag("adj_user_input")
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Adjustment Title") },
                        modifier = Modifier.fillMaxWidth().testTag("adj_title_input")
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (৳)") },
                        modifier = Modifier.fillMaxWidth().testTag("adj_amount_input")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Admin Note / Explanation") },
                        modifier = Modifier.fillMaxWidth().testTag("adj_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (targetUser.isNotBlank() && title.isNotBlank()) {
                            viewModel.recordFinancialAdjustment(
                                targetUserId = targetUser,
                                titleEn = title,
                                titleBn = title,
                                amount = amt,
                                adjustmentType = adjustmentType,
                                noteEn = note,
                                noteBn = note
                            )
                            showFinancialAdjustmentDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_financial_adj_btn")
                ) {
                    Text(if (lang == Language.BN) "সংরক্ষণ ও অডিট লগ তৈরী" else "Save & Log to ActivityLogs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinancialAdjustmentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ActivityLogCard(log: ActivityLogEntity, lang: Language) {
    val (badgeColor, badgeText, icon) = when (log.actionType) {
        "NOTICE_CREATION" -> Triple(MaterialTheme.colorScheme.primary, if (lang == Language.BN) "বিজ্ঞপ্তি" else "Notice", Icons.Default.Campaign)
        "COMPLAINT_UPDATE" -> Triple(Color(0xFFE65100), if (lang == Language.BN) "অভিযোগ" else "Complaint", Icons.Default.Build)
        "FINANCIAL_ADJUSTMENT" -> Triple(Color(0xFF2E7D32), if (lang == Language.BN) "আর্থিক" else "Financial", Icons.Default.AccountBalanceWallet)
        "MEMBER_APPROVAL" -> Triple(Color(0xFF6A1B9A), if (lang == Language.BN) "সদস্য পদ" else "Membership", Icons.Default.VerifiedUser)
        else -> Triple(MaterialTheme.colorScheme.secondary, log.actionType, Icons.Default.Info)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("activity_log_card_${log.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Firestore: ActivityLogs",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (lang == Language.BN && log.titleBn.isNotBlank()) log.titleBn else log.titleEn,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            if (log.detailsEn.isNotBlank() || log.detailsBn.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == Language.BN && log.detailsBn.isNotBlank()) log.detailsBn else log.detailsEn,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "By: ${log.adminName} (${log.adminId})",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = log.timestamp,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AdminComplaintItem(complaint: ComplaintEntity, viewModel: ClubViewModel, lang: Language) {
    var showResolveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(if (lang == Language.BN) complaint.titleBn else complaint.titleEn, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${complaint.userNameEn} (${complaint.holdingNo})", fontSize = 11.sp, color = Color.Gray)
            Text(if (lang == Language.BN) complaint.descriptionBn else complaint.descriptionEn, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(complaint.status, fontSize = 10.sp) }
                )
                Button(
                    onClick = { showResolveDialog = true },
                    modifier = Modifier.testTag("admin_resolve_btn_${complaint.id}")
                ) {
                    Text("Update Status", fontSize = 11.sp)
                }
            }
        }
    }

    if (showResolveDialog) {
        var note by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Resolved") }

        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text("Update Complaint Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Status:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = status == "Under Review", onClick = { status = "Under Review" }, label = { Text("Under Review") })
                        FilterChip(selected = status == "Resolved", onClick = { status = "Resolved" }, label = { Text("Resolved") })
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Admin Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateComplaintStatus(complaint, status, note, note)
                    showResolveDialog = false
                }) {
                    Text("Save")
                }
            }
        )
    }
}
