package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "USR-101",
    // Exact link to the authenticated Firebase Auth account (auth.currentUser.uid).
    // This is the ONLY safe way to map a signed-in session to a resident profile —
    // never match on name substrings or phone/email string comparisons alone.
    val firebaseUid: String = "",
    val phone: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val dob: String = "",
    val bloodGroup: String = "",
    val professionEn: String = "",
    val professionBn: String = "",
    val road: String = "",
    val block: String = "",
    val floor: String = "",
    val holding: String = "",
    val primaryContact: String = "",
    val emergencyContact: String = "",
    val fatherOrSpouseNameEn: String = "",
    val fatherOrSpouseNameBn: String = "",
    val motherNameEn: String = "",
    val motherNameBn: String = "",
    val familyMembersCount: Int = 1,
    val membershipStatus: String = "Active", // "Active", "Pending", "Suspended"
    val role: String = Roles.NEW_MEMBER, // SUPER_ADMIN, ADMIN, ENTREPRENEURIAL_MEMBER, GENERAL_MEMBER, NEW_MEMBER
    val profilePicUrl: String = "",
    val nidFrontUrl: String = "",
    val nidBackUrl: String = "",
    val joinedDate: String = ""
)
