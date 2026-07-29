package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ClubRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val financialDao = db.financialDao()
    private val announcementDao = db.announcementDao()
    private val complaintDao = db.complaintDao()
    private val activityDao = db.activityDao()
    private val activityLogDao = db.activityLogDao()
    private val eventDao = db.eventDao()

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allAnnouncements: Flow<List<AnnouncementEntity>> = announcementDao.getAllAnnouncements()
    val allActivities: Flow<List<ActivityEntity>> = activityDao.getAllActivities()
    val allComplaints: Flow<List<ComplaintEntity>> = complaintDao.getAllComplaints()
    val allFinancials: Flow<List<FinancialRecordEntity>> = financialDao.getAllFinancials()
    val allActivityLogs: Flow<List<ActivityLogEntity>> = activityLogDao.getAllActivityLogs()
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    fun getUserById(userId: String): Flow<UserEntity?> = userDao.getUserById(userId)
    fun getComplaintsByUserId(userId: String): Flow<List<ComplaintEntity>> = complaintDao.getComplaintsByUserId(userId)
    fun getFinancialsByUserId(userId: String): Flow<List<FinancialRecordEntity>> = financialDao.getFinancialsByUserId(userId)

    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)
    suspend fun getUserByFirebaseUid(uid: String): UserEntity? = userDao.getUserByFirebaseUid(uid)

    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun cacheUsers(users: List<UserEntity>) = userDao.insertUsers(users)
    suspend fun updateMembershipStatus(userId: String, status: String) = userDao.updateMembershipStatus(userId, status)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    suspend fun insertFinancialRecord(record: FinancialRecordEntity) = financialDao.insertFinancialRecord(record)
    // Local cache mirror only — the real payment confirmation happens server-side via
    // executeBkashPayment (Cloud Function), after bKash itself confirms the transaction.
    suspend fun updatePaymentStatus(firestoreId: String, status: String, txId: String, gateway: String) =
        financialDao.updatePaymentStatusByFirestoreId(firestoreId, status, txId, gateway)

    suspend fun insertAnnouncement(announcement: AnnouncementEntity) = announcementDao.insertAnnouncement(announcement)
    suspend fun cacheAnnouncements(announcements: List<AnnouncementEntity>) = announcementDao.insertAnnouncements(announcements)
    suspend fun deleteAnnouncement(id: Long) = announcementDao.deleteAnnouncement(id)

    suspend fun insertComplaint(complaint: ComplaintEntity) = complaintDao.insertComplaint(complaint)
    // Local cache mirror only — the real update happens server-side via the
    // updateComplaintStatus Cloud Function; see ComplaintsViewModel.
    suspend fun updateComplaintStatus(firestoreId: String, status: String, noteEn: String, noteBn: String, updatedAt: String) =
        complaintDao.updateComplaintStatusByFirestoreId(firestoreId, status, noteEn, noteBn, updatedAt)

    suspend fun insertActivity(activity: ActivityEntity) = activityDao.insertActivity(activity)

    suspend fun insertActivityLog(log: ActivityLogEntity) = activityLogDao.insert(log)

    suspend fun insertEvent(event: EventEntity) = eventDao.insert(event)
    suspend fun toggleEventReminder(id: String, isSet: Boolean) = eventDao.updateReminderStatus(id, isSet)

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingUsers = userDao.getAllUsers().firstOrNull()
        if (existingUsers.isNullOrEmpty()) {
            // Seed Users
            userDao.insertUser(
                UserEntity(
                    id = "USR-101",
                    phone = "+8801712345678",
                    nameEn = "Md. Rafiqul Islam",
                    nameBn = "মোঃ রফিকুল ইসলাম",
                    dob = "1982-08-14",
                    bloodGroup = "O+",
                    professionEn = "Civil Engineer",
                    professionBn = "সিভিল ইঞ্জিনিয়ার",
                    road = "Road 04",
                    block = "Block B",
                    floor = "3rd Floor",
                    holding = "Holding 42/A",
                    primaryContact = "+8801712345678",
                    emergencyContact = "+8801799887766",
                    fatherOrSpouseNameEn = "Late Alhajj Nurul Islam",
                    fatherOrSpouseNameBn = "মরহুম আলহাজ্ব নুরুল ইসলাম",
                    motherNameEn = "Rokeya Begum",
                    motherNameBn = "রোকেয়া বেগম",
                    familyMembersCount = 4,
                    membershipStatus = "Active",
                    role = Roles.GENERAL_MEMBER,
                    profilePicUrl = "",
                    nidFrontUrl = "NID-829104829104",
                    nidBackUrl = "NID-BACK-829104829104",
                    joinedDate = "2021-03-15"
                )
            )

            userDao.insertUser(
                UserEntity(
                    id = "USR-102",
                    phone = "+8801819876543",
                    nameEn = "Adv. Farhana Yasmin",
                    nameBn = "এডভোকেট ফারহানা ইয়াসমিন",
                    dob = "1985-11-22",
                    bloodGroup = "B+",
                    professionEn = "Supreme Court Advocate & Club Vice-President",
                    professionBn = "সুপ্রিম কোর্ট আইনজীবী ও ক্লাব সহ-সভাপতি",
                    road = "Road 02",
                    block = "Block A",
                    floor = "5th Floor",
                    holding = "Holding 12",
                    primaryContact = "+8801819876543",
                    emergencyContact = "+8801811002233",
                    fatherOrSpouseNameEn = "Engr. Mahmud Hassan",
                    fatherOrSpouseNameBn = "ইঞ্জি: মাহমুদ হাসান",
                    motherNameEn = "Suraiya Hassan",
                    motherNameBn = "সুরাইয়া হাসান",
                    familyMembersCount = 3,
                    membershipStatus = "Active",
                    role = Roles.ADMIN,
                    profilePicUrl = "",
                    nidFrontUrl = "NID-198273910283",
                    nidBackUrl = "NID-BACK-198273910283",
                    joinedDate = "2019-01-10"
                )
            )

            userDao.insertUser(
                UserEntity(
                    id = "USR-103",
                    phone = "+8801911223344",
                    nameEn = "Eng. Tanvir Ahmed",
                    nameBn = "প্রকৌশলী তানভীর আহমেদ",
                    dob = "1990-04-05",
                    bloodGroup = "A+",
                    professionEn = "Software Architect",
                    professionBn = "সফটওয়্যার আর্কিটেক্ট",
                    road = "Road 06",
                    block = "Block C",
                    floor = "1st Floor",
                    holding = "Holding 88/B",
                    primaryContact = "+8801911223344",
                    emergencyContact = "+8801922334455",
                    fatherOrSpouseNameEn = "Dr. Jalal Uddin",
                    fatherOrSpouseNameBn = "ড. জালাল উদ্দিন",
                    motherNameEn = "Fatema Begum",
                    motherNameBn = "ফাতেমা বেগম",
                    familyMembersCount = 2,
                    membershipStatus = "Pending",
                    role = Roles.NEW_MEMBER,
                    profilePicUrl = "",
                    nidFrontUrl = "NID-392019284019",
                    nidBackUrl = "NID-BACK-392019284019",
                    joinedDate = "2026-07-20"
                )
            )

            // Seed Financials for USR-101
            financialDao.insertFinancialRecord(
                FinancialRecordEntity(
                    userId = "USR-101",
                    titleEn = "Monthly Service & Security Dues (July 2026)",
                    titleBn = "মাসিক সার্ভিস ও সিকিউরিটি চার্জ (জুলাই ২০২৬)",
                    amount = 1500.0,
                    type = "Due",
                    monthYear = "July 2026",
                    date = "2026-07-01",
                    status = "Pending"
                )
            )
            financialDao.insertFinancialRecord(
                FinancialRecordEntity(
                    userId = "USR-101",
                    titleEn = "Annual Cultural Event Contribution",
                    titleBn = "বার্ষিক সাংস্কৃতিক অনুষ্ঠান চাঁদা",
                    amount = 1000.0,
                    type = "Due",
                    monthYear = "July 2026",
                    date = "2026-07-10",
                    status = "Pending"
                )
            )
            financialDao.insertFinancialRecord(
                FinancialRecordEntity(
                    userId = "USR-101",
                    titleEn = "Monthly Service Charge (June 2026)",
                    titleBn = "মাসিক সার্ভিস চার্জ (জুন ২০২৬)",
                    amount = 1500.0,
                    type = "Paid",
                    monthYear = "June 2026",
                    date = "2026-06-05",
                    paymentGateway = "bKash",
                    transactionId = "BK8192039102",
                    status = "Completed"
                )
            )
            financialDao.insertFinancialRecord(
                FinancialRecordEntity(
                    userId = "USR-101",
                    titleEn = "Community Park Green Initiative Donation",
                    titleBn = "কমিউনিটি পার্ক সবুজায়ন অনুদান",
                    amount = 5000.0,
                    type = "Donation",
                    monthYear = "May 2026",
                    date = "2026-05-18",
                    paymentGateway = "Nagad",
                    transactionId = "NG77281903",
                    status = "Completed"
                )
            )

            // Seed Announcements
            announcementDao.insertAnnouncement(
                AnnouncementEntity(
                    titleEn = "Tree Plantation Drive & Resident Meet 2026",
                    titleBn = "বৃক্ষরোপণ অভিযান ও আবাসিক মিলনমেলা ২০২৬",
                    descriptionEn = "All members are warmly invited to join the greening initiative at Kunjachaya Central Park this Friday at 9:00 AM.",
                    descriptionBn = "আগামী শুক্রবার সকাল ৯:০০ টায় কুঞ্জছায়া সেন্ট্রাল পার্কে সবুজায়ন উদ্যোগে সকল সদস্যকে সাদর আমন্ত্রণ জানানো হচ্ছে।",
                    categoryEn = "Urgent Notice",
                    categoryBn = "জরুরী নোটিশ",
                    date = "2026-07-24",
                    priority = "High"
                )
            )
            announcementDao.insertAnnouncement(
                AnnouncementEntity(
                    titleEn = "Security RFID Card Renewal at Gate 1 & Gate 2",
                    titleBn = "গেটস ১ ও ২ এ সিকিউরিটি আরএফআইডি কার্ড নবায়ন",
                    descriptionEn = "Please update your resident smart RFID cards from the office by July 31st to avoid gate delays.",
                    descriptionBn = "গেট বিলম্ব এড়াতে আগামী ৩১ জুলাইয়ের মধ্যে ক্লাব অফিস থেকে ডিজিটাল স্মার্ট আরএফআইডি কার্ড সংগ্রহ করুন।",
                    categoryEn = "General Notice",
                    categoryBn = "সাধারণ বিজ্ঞপ্তি",
                    date = "2026-07-20",
                    priority = "Medium"
                )
            )

            // Seed Complaints
            complaintDao.insertComplaint(
                ComplaintEntity(
                    userId = "USR-101",
                    userNameEn = "Md. Rafiqul Islam",
                    userNameBn = "মোঃ রফিকুল ইসলাম",
                    holdingNo = "Holding 42/A, Road 04",
                    titleEn = "Street Light Outage near Road 04 Corner",
                    titleBn = "৪ নং রোড কর্নারের স্ট্রিট লাইট বিকল",
                    categoryEn = "Water & Electricity",
                    categoryBn = "পানি ও বিদ্যুৎ",
                    descriptionEn = "The street light fixture on Road 04 near plot 42/A has been flickering and completely dark for 2 days.",
                    descriptionBn = "রোড ০৪-এ প্লট ৪২/এ সংলগ্ন স্ট্রিট লাইটটি ২ দিন ধরে নষ্ট অবস্থায় আছে।",
                    imageUrl = "img_club_banner",
                    status = "Resolved",
                    adminNoteEn = "Electrician replaced LED fixture on July 23.",
                    adminNoteBn = "২৩ জুলাই ইলেকট্রিশিয়ান নতুন এলইডি সেট যুক্ত করেছে।",
                    createdAt = "2026-07-22 18:30",
                    updatedAt = "2026-07-23 10:15"
                )
            )
            complaintDao.insertComplaint(
                ComplaintEntity(
                    userId = "USR-101",
                    userNameEn = "Md. Rafiqul Islam",
                    userNameBn = "মোঃ রফিকুল ইসলাম",
                    holdingNo = "Holding 42/A, Road 04",
                    titleEn = "Late Night Heavy Construction Noise at Block B",
                    titleBn = "ব্লক বি-তে গভীর রাতে নির্মাণকাজের বিকট শব্দ",
                    categoryEn = "Noise Pollution",
                    categoryBn = "শব্দ দূষণ",
                    descriptionEn = "Unregulated drill noise past 11:00 PM near plot 30 is causing disturbance for elderly residents.",
                    descriptionBn = "প্লট ৩০ সংলগ্ন স্থানে রাত ১১টার পর ড্রিল মেশিনের বিকট শব্দে বয়স্ক বাসিন্দাদের ঘুম ব্যাঘাত ঘটছে।",
                    imageUrl = "img_club_logo",
                    status = "Under Review",
                    adminNoteEn = "Security Guard dispatched to issue warning to contractor.",
                    adminNoteBn = "ঠিকাদারকে সতর্কবার্তা জারি করার জন্য নিরাপত্তা কর্মী পাঠানো হয়েছে।",
                    createdAt = "2026-07-24 23:10",
                    updatedAt = "2026-07-25 09:00"
                )
            )

            // Seed Activities
            activityDao.insertActivity(
                ActivityEntity(
                    titleEn = "Grand Eid Reunion & Cultural Night",
                    titleBn = "গ্র্যান্ড ঈদ পুনর্মিলনী ও সাংস্কৃতিক সন্ধ্যা",
                    date = "2026-06-20",
                    locationEn = "Kunjachaya Club Main Auditorium",
                    locationBn = "কুঞ্জছায়া ক্লাব প্রধান অডিটোরিয়াম",
                    summaryEn = "Community dinner, musical program by youth club, and awards ceremony for distinguished residents.",
                    summaryBn = "কমিউনিটি নৈশভোজ, তরুণ ক্লাবের পরিবেশনায় মনোজ্ঞ সংগীত ও কৃতি গুণীজন সংবর্ধনা।",
                    imageUrl = "",
                    participantsCount = 180
                )
            )
            activityDao.insertActivity(
                ActivityEntity(
                    titleEn = "Free Health Checkup & Blood Donation Camp",
                    titleBn = "বিনামূল্যে হেলথ চেকআপ ও রক্তদান ক্যাম্প",
                    date = "2026-05-10",
                    locationEn = "Central Lawn & Medical Booth",
                    locationBn = "সেন্ট্রাল লন ও মেডিকেল বুথ",
                    summaryEn = "Specialist doctors provided free consultations for diabetes, eye check, and blood pressure to 250 residents.",
                    summaryBn = "বিশেষজ্ঞ ডাক্তারগণ ২৫০ জন নিবাসী সদস্যকে বিনামূল্যে ডায়াবেটিস ও প্রেশার পরীক্ষা সেবা দেন।",
                    imageUrl = "",
                    participantsCount = 250
                )
            )

            // Seed Activity Audit Logs
            activityLogDao.insertAll(
                listOf(
                    ActivityLogEntity(
                        id = "LOG-1001",
                        actionType = "NOTICE_CREATION",
                        adminId = "ADM-001",
                        adminName = "Club Executive Committee",
                        titleEn = "Published Notice: Annual General Meeting 2026 Scheduled",
                        titleBn = "বিজ্ঞপ্তি প্রকাশ: বার্ষিক সাধারণ সভা ২০২৬ এর সময়সূচি",
                        detailsEn = "Created urgent community bulletin regarding AGM on July 30, 2026 at Club Auditorium.",
                        detailsBn = "৩০ জুলাই ২০২৬ অডিটোরিয়ামে অনুষ্ঠিতব্য বার্ষিক সাধারণ সভার নতুন বিজ্ঞপ্তি প্রকাশ করা হয়েছে।",
                        timestamp = "2026-07-25 10:15",
                        targetId = "NOTICE-101"
                    ),
                    ActivityLogEntity(
                        id = "LOG-1002",
                        actionType = "COMPLAINT_UPDATE",
                        adminId = "ADM-002",
                        adminName = "Maintenance Secretary",
                        titleEn = "Updated Complaint #102 Status to Resolved",
                        titleBn = "অভিযোগ #১০২ এর স্ট্যাটাস 'সমাধান করা হয়েছে' এ হালনাগাদ",
                        detailsEn = "Replaced flickering street light fixture on Road 04 near plot 42/A.",
                        detailsBn = "রোড ০৪ এর প্লট ৪২/এ সংলগ্ন নষ্ট স্ট্রিট লাইট পরিবর্তন করে নতুন লাইট লাগানো হয়েছে।",
                        timestamp = "2026-07-23 15:40",
                        targetId = "CMP-102"
                    ),
                    ActivityLogEntity(
                        id = "LOG-1003",
                        actionType = "FINANCIAL_ADJUSTMENT",
                        adminId = "ADM-001",
                        adminName = "Treasurer (Md. Shamsul Alam)",
                        titleEn = "Financial Adjustment: Annual Maintenance Fee Schedule Updated",
                        titleBn = "আর্থিক সমন্বয়: বার্ষিক রক্ষণাবেক্ষণ সার্ভিস ফি পুনর্নির্ধারণ",
                        detailsEn = "Adjusted monthly security & waste management levy to ৳ 1,500 for Q3 2026.",
                        detailsBn = "২০২৬ সালের ৩য় প্রান্তিকের জন্য মাসিক নিরাপত্তা ও বর্জ্য ব্যবস্থাপনা ফি ১,৫০০ টাকায় সমন্বয় করা হয়েছে।",
                        timestamp = "2026-07-20 11:30",
                        targetId = "FIN-2026-Q3"
                    ),
                    ActivityLogEntity(
                        id = "LOG-1004",
                        actionType = "MEMBER_APPROVAL",
                        adminId = "ADM-001",
                        adminName = "Membership Committee",
                        titleEn = "Approved Member: Engr. Selim Chowdhury",
                        titleBn = "সদস্য পদ অনুমোদন: প্রকৌশলী সেলিম চৌধুরী",
                        detailsEn = "Verified NID documents and approved active resident membership for Holding 12, Road 03.",
                        detailsBn = "জাতীয় পরিচয়পত্র যাচাইকরণ শেষে হোল্ডিং ১২, রোড ০৩ এর নতুন সদস্যপদ অনুমোদন দেওয়া হয়েছে।",
                        timestamp = "2026-07-18 09:20",
                        targetId = "USR-102"
                    )
                )
            )

            // Seed Calendar Events (Club Events, Meetings, Payment Deadlines)
            eventDao.insertAll(
                listOf(
                    EventEntity(
                        id = "EVT-101",
                        titleEn = "Annual General Cultural Night 2026",
                        titleBn = "বার্ষিক সাধারণ সাংস্কৃতিক সন্ধ্যা ২০২৬",
                        descriptionEn = "Grand musical and drama performance by neighborhood youth and local artists followed by dinner buffet.",
                        descriptionBn = "মহল্লার তরুণ এবং স্থানীয় শিল্পীদের অংশগ্রহণে বর্ণাঢ্য গান ও নাট্যানুষ্ঠান এবং নৈশভোজ।",
                        eventType = "EVENT",
                        date = "2026-07-28",
                        time = "18:30",
                        locationEn = "Kunjachaya Club Main Auditorium",
                        locationBn = "কুঞ্জছায়া ক্লাব প্রধান অডিটোরিয়াম",
                        amount = 0.0,
                        isReminderSet = true
                    ),
                    EventEntity(
                        id = "EVT-102",
                        titleEn = "Executive Committee Monthly Strategy Meeting",
                        titleBn = "কার্যনির্বাহী কমিটির মাসিক নীতিনির্ধারণী সভা",
                        descriptionEn = "Board review of security CCTV upgrades, road drainage repair budget, and Q3 audit log presentation.",
                        descriptionBn = "সিসিটিভি ক্যামেরা সচল রাখা, রাস্তা ড্রেনেজ সংস্কার বাজেট এবং ৩য় প্রান্তিকের হিসাব নিরীক্ষা পর্যালোচনা।",
                        eventType = "MEETING",
                        date = "2026-07-30",
                        time = "19:00",
                        locationEn = "Committee Board Room & Google Meet",
                        locationBn = "কমিটি বোর্ড রুম ও অনলাইন গুগল মিট",
                        amount = 0.0,
                        isReminderSet = false
                    ),
                    EventEntity(
                        id = "EVT-103",
                        titleEn = "July Monthly Maintenance & Security Fee Due Date",
                        titleBn = "জুলাই মাসের নিরাপত্তা ও সার্ভিস ফি জমা দেওয়ার শেষ তারিখ",
                        descriptionEn = "Mandatory monthly resident contribution (৳ 1,500) deadline to avoid late surcharges.",
                        descriptionBn = "বিলম্বিত ফি এড়াতে নির্ধারিত সময়ের মধ্যে জুলাই মাসের সার্ভিস ফি পরিশোধ করুন।",
                        eventType = "PAYMENT_DEADLINE",
                        date = "2026-07-31",
                        time = "23:59",
                        locationEn = "Online Payment Portal / Club Office",
                        locationBn = "অনলাইন পেমেন্ট গেটওয়ে / ক্লাব ক্যাশ কাউন্টার",
                        amount = 1500.0,
                        isReminderSet = true
                    ),
                    EventEntity(
                        id = "EVT-104",
                        titleEn = "Neighborhood Greenery & Tree Plantation Drive",
                        titleBn = "পরিবেশ সুরক্ষা ও বৃক্ষরোপণ অভিযান ২০২৬",
                        descriptionEn = "Community volunteer drive planting 500 fruit and herbal trees along Road 1 to 5.",
                        descriptionBn = "রোড ১ থেকে ৫ পর্যন্ত ৫০০টি ফলজ ও ভেষজ গাছ লাগানোর যৌথ উদ্যোগ।",
                        eventType = "EVENT",
                        date = "2026-08-05",
                        time = "08:00",
                        locationEn = "Central Park Plaza",
                        locationBn = "সেন্ট্রাল পার্ক প্লাজা",
                        amount = 0.0,
                        isReminderSet = false
                    ),
                    EventEntity(
                        id = "EVT-105",
                        titleEn = "Sub-Committee Infrastructure & Security Meeting",
                        titleBn = "অবকাঠামো ও সিকিউরিটি উপকমিটির জরুরী বৈঠক",
                        descriptionEn = "Reviewing night guard shift protocols and gate RFID barrier installation.",
                        descriptionBn = "রাত্রিকালীন প্রহারী পরিবর্তন এবং গেটে আরএফআইডি ডিজিটাল বেরিয়ার স্থাপন আলোচনা।",
                        eventType = "MEETING",
                        date = "2026-08-08",
                        time = "20:00",
                        locationEn = "Club Conference Hall",
                        locationBn = "ক্লাব কনফারেন্স হল",
                        amount = 0.0,
                        isReminderSet = false
                    ),
                    EventEntity(
                        id = "EVT-106",
                        titleEn = "Q3 Special Drainage & Infrastructure Fund Deadline",
                        titleBn = "৩য় প্রান্তিক ড্রেনেজ উন্নয়ন বিশেষ ফান্ডের শেষ সময়",
                        descriptionEn = "One-time contribution for monsoon flood prevention drainage overhaul.",
                        descriptionBn = "বর্ষায় জলবদ্ধতা নিরসনে নতুন ড্রেন নির্মাণের জন্য এককালীন সদস্য অনুদান।",
                        eventType = "PAYMENT_DEADLINE",
                        date = "2026-08-15",
                        time = "23:59",
                        locationEn = "Bkash / Nagad / Bank Transfer",
                        locationBn = "বিকাশ / নগদ / ব্যাংক ট্রান্সফার",
                        amount = 2000.0,
                        isReminderSet = true
                    )
                )
            )
        }
    }
}
