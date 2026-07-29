package com.example.data.db

import androidx.room.*
import com.example.data.model.ComplaintEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY id DESC")
    fun getAllComplaints(): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE userId = :userId ORDER BY id DESC")
    fun getComplaintsByUserId(userId: String): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity): Long

    // Admin-only status changes are written server-side (Cloud Function) and mirrored
    // here by the Firestore listener, keyed on the real document ID.
    @Query("UPDATE complaints SET status = :status, adminNoteEn = :noteEn, adminNoteBn = :noteBn, updatedAt = :updatedAt WHERE firestoreId = :firestoreId")
    suspend fun updateComplaintStatusByFirestoreId(firestoreId: String, status: String, noteEn: String, noteBn: String, updatedAt: String)
}
