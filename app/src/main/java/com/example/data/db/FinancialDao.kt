package com.example.data.db

import androidx.room.*
import com.example.data.model.FinancialRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financials WHERE userId = :userId ORDER BY id DESC")
    fun getFinancialsByUserId(userId: String): Flow<List<FinancialRecordEntity>>

    @Query("SELECT * FROM financials ORDER BY id DESC")
    fun getAllFinancials(): Flow<List<FinancialRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialRecord(record: FinancialRecordEntity): Long

    // Local cache is READ-ONLY for payment status. The row is written once by the
    // Firestore listener after the server (Cloud Function, via Admin SDK) confirms
    // the payment — never directly by client-side "mark as paid" logic. Keyed on
    // firestoreId, not a hashCode()-derived Long, to avoid collisions.
    @Query("UPDATE financials SET status = :status, transactionId = :txId, paymentGateway = :gateway WHERE firestoreId = :firestoreId")
    suspend fun updatePaymentStatusByFirestoreId(firestoreId: String, status: String, txId: String, gateway: String)
}
