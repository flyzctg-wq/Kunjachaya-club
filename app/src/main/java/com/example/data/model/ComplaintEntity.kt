package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Authoritative Firestore document ID — see note in FinancialRecordEntity.
    // Do not derive numeric IDs from hashCode() for cross-referencing Firestore.
    // @DocumentId auto-populates this from doc.id on toObject() reads.
    @DocumentId
    val firestoreId: String = "",
    val userId: String = "",
    val userNameEn: String = "",
    val userNameBn: String = "",
    val holdingNo: String = "",
    val titleEn: String = "",
    val titleBn: String = "",
    val categoryEn: String = "Maintenance", // "Security", "Cleanliness", "Water & Electricity", "Noise Complaint", "Other"
    val categoryBn: String = "রক্ষণাবেক্ষণ",
    val descriptionEn: String = "",
    val descriptionBn: String = "",
    val imageUrl: String = "",
    val status: String = "Pending", // "Pending", "Under Review", "Resolved"
    val adminNoteEn: String = "",
    val adminNoteBn: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
