package com.example.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

/**
 * Calls into the server-side Cloud Functions in /functions/index.js.
 *
 * This is the replacement for the old pattern of the client writing directly to
 * Firestore for anything that matters (payment status, roles, complaint resolution).
 * Those Firestore fields are now write-protected by firestore.rules, so these calls
 * are the only way those fields change at all.
 */
class FunctionsRepository {
    private val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance("asia-southeast1")
    }

    sealed class Outcome<out T> {
        data class Success<T>(val value: T) : Outcome<T>()
        data class Failure(val message: String) : Outcome<Nothing>()
    }

    private suspend fun <T> call(name: String, data: Map<String, Any?>, extract: (Map<*, *>) -> T): Outcome<T> {
        return try {
            val result = functions.getHttpsCallable(name).call(data).await()
            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<*, *> ?: emptyMap<String, Any?>()
            Outcome.Success(extract(map))
        } catch (e: FirebaseFunctionsException) {
            Outcome.Failure(e.message ?: "Request failed (${e.code})")
        } catch (e: Exception) {
            Outcome.Failure(e.message ?: "Request failed")
        }
    }

    /** Registers the current Firebase-authenticated user as a "New Member", "Pending" review — never Admin. */
    suspend fun registerResident(nameEn: String, phone: String, holding: String, road: String, block: String) =
        call("registerResident", mapOf("nameEn" to nameEn, "phone" to phone, "holding" to holding, "road" to road, "block" to block)) {
            it["userId"] as? String
        }

    /** Step 1 of a real bKash payment: returns a checkout URL to open, no status change yet. */
    suspend fun initiateBkashPayment(financialRecordId: String, callbackUrl: String) =
        call("initiateBkashPayment", mapOf("financialRecordId" to financialRecordId, "callbackURL" to callbackUrl)) {
            Pair(it["paymentID"] as? String, it["bkashURL"] as? String)
        }

    /** Step 2: confirms with bKash's own API. Only this can mark a due "Completed". */
    suspend fun executeBkashPayment(financialRecordId: String, paymentID: String) =
        call("executeBkashPayment", mapOf("financialRecordId" to financialRecordId, "paymentID" to paymentID)) {
            Pair(it["status"] as? String, it["trxID"] as? String)
        }

    /** Creates a Pending donation record; pay it via initiatePipraPayCharge afterward. */
    suspend fun createPendingDonation(titleEn: String, titleBn: String, amount: Double, purpose: String) =
        call("createPendingDonation", mapOf("titleEn" to titleEn, "titleBn" to titleBn, "amount" to amount, "purpose" to purpose)) {
            it["financialRecordId"] as? String
        }

    /**
     * PipraPay — recommended gateway: self-hosted, no bKash merchant agreement needed.
     * Step 1: opens a PipraPay checkout session. Returns the checkout URL to launch
     * (Custom Tab / WebView) plus PipraPay's own pp_id for the fallback confirm step.
     */
    suspend fun initiatePipraPayCharge(financialRecordId: String, redirectUrl: String, cancelUrl: String) =
        call(
            "initiatePipraPayCharge",
            mapOf("financialRecordId" to financialRecordId, "redirectUrl" to redirectUrl, "cancelUrl" to cancelUrl)
        ) {
            Pair(it["checkout_url"] as? String ?: it["payment_url"] as? String, it["pp_id"] as? String)
        }

    /**
     * PipraPay step 2 (redirect-flow fallback only — the webhook is the primary path
     * and usually beats this call). Independently re-verifies with PipraPay's own API
     * before anything is marked Completed.
     */
    suspend fun confirmPipraPayPayment(financialRecordId: String, ppId: String) =
        call("confirmPipraPayPayment", mapOf("financialRecordId" to financialRecordId, "ppId" to ppId)) {
            Pair(it["success"] as? Boolean ?: false, it["status"] as? String)
        }

    /** Admin-only server-side check enforced in the function itself, not here. */
    suspend fun updateComplaintStatus(complaintId: String, status: String, noteEn: String, noteBn: String) =
        call("updateComplaintStatus", mapOf("complaintId" to complaintId, "status" to status, "adminNoteEn" to noteEn, "adminNoteBn" to noteBn)) {
            it["success"] as? Boolean ?: false
        }

    /** Admin-only server-side check enforced in the function itself, not here. */
    suspend fun approveMembership(userId: String, status: String) =
        call("approveMembership", mapOf("userId" to userId, "status" to status)) {
            it["success"] as? Boolean ?: false
        }

    /** Admin-only server-side check enforced in the function itself, not here. */
    suspend fun setUserRole(targetUid: String, role: String) =
        call("setUserRole", mapOf("targetUid" to targetUid, "role" to role)) {
            it["success"] as? Boolean ?: false
        }

    /** Admin-only server-side check enforced in the function itself, not here. */
    suspend fun recordFinancialAdjustment(
        targetUserId: String, titleEn: String, titleBn: String, amount: Double,
        adjustmentType: String, noteEn: String, noteBn: String
    ) = call(
        "recordFinancialAdjustment",
        mapOf(
            "targetUserId" to targetUserId, "titleEn" to titleEn, "titleBn" to titleBn,
            "amount" to amount, "adjustmentType" to adjustmentType, "noteEn" to noteEn, "noteBn" to noteBn
        )
    ) { it["recordId"] as? String }
}
