package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "financials")
data class FinancialRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // The authoritative Firestore document ID. Never derive this from hashCode() —
    // String.hashCode() collapses to 32 bits and WILL collide at scale, silently
    // merging two different residents' payment records. Always store and use the
    // real Firestore doc ID string for any cross-reference.
    // @DocumentId auto-populates this from the document's own ID on toObject() reads
    // — Firestore doesn't store it as a regular field, so without this annotation
    // it would silently stay blank on every synced record.
    @DocumentId
    val firestoreId: String = "",
    val userId: String = "",
    val titleEn: String = "",
    val titleBn: String = "",
    val amount: Double = 0.0,
    val type: String = "Due", // "Due", "Paid", "Donation"
    val monthYear: String = "", // e.g., "July 2026"
    val date: String = "",
    val paymentGateway: String = "", // "bKash", "Nagad", "Rocket", "Bank Transfer"
    val transactionId: String = "",
    val status: String = "Pending" // "Pending", "Completed", "Failed"
)
