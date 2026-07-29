package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Real Firestore document ID — see the same note in FinancialRecordEntity on why
    // this replaces hashCode()-derived IDs (collision risk) for anything that needs
    // to reference a specific announcement (e.g. deleteAnnouncement).
    @DocumentId
    val firestoreId: String = "",
    val titleEn: String = "",
    val titleBn: String = "",
    val descriptionEn: String = "",
    val descriptionBn: String = "",
    val categoryEn: String = "", // "Urgent Notice", "General News", "Upcoming Event", "Maintenance"
    val categoryBn: String = "",
    val date: String = "",
    val priority: String = "Medium", // "High", "Medium", "Low"
    val author: String = "Club Management Committee"
)
