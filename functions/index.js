/**
 * Kunjachaya Resident Club — Cloud Functions
 *
 * Design rule enforced everywhere in this file: the client NEVER gets to declare
 * "this is paid" or "I am an admin." Every state change that matters (payment
 * completion, role assignment, complaint resolution) happens here, using the
 * Admin SDK (which bypasses Firestore Security Rules), after this server itself
 * has verified the relevant fact — with bKash for payments, with Firebase Auth
 * custom claims for roles.
 */

const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");
const bkash = require("./bkash");
const piprapay = require("./piprapay");

admin.initializeApp();
const db = admin.firestore();

setGlobalOptions({ region: "asia-southeast1", maxInstances: 10 });

// ---------------------------------------------------------------------------
// Role hierarchy
// ---------------------------------------------------------------------------
// Five tiers, ranked highest to lowest. Stored both as a Firestore `role` string
// (for display/UI gating) and mirrored into a Firebase Auth custom claim (the
// actual enforcement mechanism — Firestore rules and these functions trust the
// claim, never the Firestore field, since only Admin-SDK code can set claims).
const ROLES = ["SUPER_ADMIN", "ADMIN", "ENTREPRENEURIAL_MEMBER", "GENERAL_MEMBER", "NEW_MEMBER"];
const RANK = {
  SUPER_ADMIN: 4,
  ADMIN: 3,
  ENTREPRENEURIAL_MEMBER: 2,
  GENERAL_MEMBER: 1,
  NEW_MEMBER: 0,
};
// Roles an ADMIN (not Super Admin) is allowed to assign/manage — Admins run
// day-to-day member management but can never appoint another Admin or touch
// the Super Admin seat. Only Super Admin can do that.
const ADMIN_MANAGEABLE_ROLES = ["ENTREPRENEURIAL_MEMBER", "GENERAL_MEMBER", "NEW_MEMBER"];

function requireSuperAdmin(request) {
  const uid = requireAuth(request);
  if (request.auth.token && request.auth.token.role === "SUPER_ADMIN") {
    return uid;
  }
  throw new HttpsError("permission-denied", "Super Admin privileges required.");
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function requireAuth(request) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required.");
  }
  return request.auth.uid;
}

async function requireAdmin(request) {
  const uid = requireAuth(request);
  // Source of truth is the `role` custom claim, set only by setUserRole below
  // (itself gated on the caller's own claim) — never read from a client payload.
  // Both ADMIN and SUPER_ADMIN qualify for "admin-level" actions; the finer-
  // grained distinction (e.g. who may appoint an Admin) is enforced separately
  // in setUserRole via requireSuperAdmin.
  const role = request.auth.token && request.auth.token.role;
  if (role === "SUPER_ADMIN" || role === "ADMIN") {
    return uid;
  }
  throw new HttpsError("permission-denied", "Admin privileges required.");
}

async function getResidentByUid(uid) {
  const snap = await db.collection("users").where("firebaseUid", "==", uid).limit(1).get();
  if (snap.empty) return null;
  return { id: snap.docs[0].id, ...snap.docs[0].data() };
}

// ---------------------------------------------------------------------------
// Roles & membership — the fix for the "pick Admin on a dropdown" vulnerability
// ---------------------------------------------------------------------------

/**
 * Change a resident's role. Enforces the actual hierarchy described in the
 * club's role spec, not just a flat "admin or not":
 *   - Only SUPER_ADMIN may appoint or dismiss an ADMIN, or touch another
 *     SUPER_ADMIN/ADMIN account at all.
 *   - ADMIN may promote/demote among ENTREPRENEURIAL_MEMBER / GENERAL_MEMBER /
 *     NEW_MEMBER, but only for targets who are currently at one of those tiers
 *     — an Admin can never touch an Admin or Super Admin account, including
 *     their own.
 *   - Nobody can change their own role through this function, Super Admin
 *     included — ownership transfer (a role change at the very top) has its
 *     own dedicated, more heavily audited function below.
 * As before: there is no path anywhere in this codebase for a user to grant
 * themselves any of this — the first Super Admin is bootstrapped manually via
 * the Admin SDK from a trusted operator machine (see functions/README.md).
 */
exports.setUserRole = onCall(async (request) => {
  const callerUid = requireAuth(request);
  const callerRole = request.auth.token && request.auth.token.role;

  const { targetUid, role } = request.data || {};
  if (!targetUid || !ROLES.includes(role)) {
    throw new HttpsError("invalid-argument", `targetUid and a valid role are required (one of: ${ROLES.join(", ")}).`);
  }
  if (targetUid === callerUid) {
    throw new HttpsError("permission-denied", "You cannot change your own role.");
  }

  const targetUserRecord = await admin.auth().getUser(targetUid);
  const targetCurrentRole = (targetUserRecord.customClaims && targetUserRecord.customClaims.role) || "NEW_MEMBER";

  if (callerRole === "SUPER_ADMIN") {
    // Super Admin may assign any role to anyone, with one exception: appointing
    // a new SUPER_ADMIN should go through transferOwnership, not this function,
    // since it also needs to demote the outgoing Super Admin in the same step.
    if (role === "SUPER_ADMIN") {
      throw new HttpsError("failed-precondition", "Use transferOwnership to change the Super Admin seat.");
    }
  } else if (callerRole === "ADMIN") {
    if (!ADMIN_MANAGEABLE_ROLES.includes(role)) {
      throw new HttpsError("permission-denied", "Admins can only assign Entrepreneurial, General, or New Member roles.");
    }
    if (!ADMIN_MANAGEABLE_ROLES.includes(targetCurrentRole)) {
      throw new HttpsError("permission-denied", "Admins cannot change the role of another Admin or the Super Admin.");
    }
  } else {
    throw new HttpsError("permission-denied", "Admin or Super Admin privileges required.");
  }

  await admin.auth().setCustomUserClaims(targetUid, {
    role,
    admin: role === "SUPER_ADMIN" || role === "ADMIN", // kept for firestore.rules' isAdmin()
  });

  const userSnap = await db.collection("users").where("firebaseUid", "==", targetUid).limit(1).get();
  if (!userSnap.empty) {
    await userSnap.docs[0].ref.update({
      role,
      roleChangedAt: admin.firestore.FieldValue.serverTimestamp(),
      roleChangedBy: callerUid,
    });
  }

  return { success: true };
});

/**
 * Super-Admin-only ownership transfer: hands the Super Admin seat to another
 * account and demotes the outgoing Super Admin to ADMIN in the same operation,
 * so there is never a moment with zero or two Super Admins as a side effect of
 * a partial failure (both writes happen before either custom-claims call, and
 * claims are set in sequence with the new Super Admin first).
 */
exports.transferOwnership = onCall(async (request) => {
  const callerUid = requireSuperAdmin(request);
  const { newSuperAdminUid } = request.data || {};
  if (!newSuperAdminUid) {
    throw new HttpsError("invalid-argument", "newSuperAdminUid is required.");
  }
  if (newSuperAdminUid === callerUid) {
    throw new HttpsError("failed-precondition", "You are already the Super Admin.");
  }

  await admin.auth().setCustomUserClaims(newSuperAdminUid, { role: "SUPER_ADMIN", admin: true });
  await admin.auth().setCustomUserClaims(callerUid, { role: "ADMIN", admin: true });

  const updates = [];
  const newSnap = await db.collection("users").where("firebaseUid", "==", newSuperAdminUid).limit(1).get();
  if (!newSnap.empty) {
    updates.push(newSnap.docs[0].ref.update({ role: "SUPER_ADMIN", roleChangedAt: admin.firestore.FieldValue.serverTimestamp(), roleChangedBy: callerUid }));
  }
  const oldSnap = await db.collection("users").where("firebaseUid", "==", callerUid).limit(1).get();
  if (!oldSnap.empty) {
    updates.push(oldSnap.docs[0].ref.update({ role: "ADMIN", roleChangedAt: admin.firestore.FieldValue.serverTimestamp(), roleChangedBy: callerUid }));
  }
  await Promise.all(updates);

  return { success: true };
});

/**
 * Super-Admin-only: archives the club workspace (a soft, reversible "dissolve").
 * Does not delete data — sets a flag every client checks and renders a read-only
 * / closed state from, so this is safe to reverse by calling it again with
 * archived: false, and doesn't destroy financial/complaint history.
 */
exports.setWorkspaceArchived = onCall(async (request) => {
  const callerUid = requireSuperAdmin(request);
  const { archived } = request.data || {};
  if (typeof archived !== "boolean") {
    throw new HttpsError("invalid-argument", "archived (boolean) is required.");
  }
  await db.collection("clubSettings").doc("workspace").set({
    archived,
    archivedAt: archived ? admin.firestore.FieldValue.serverTimestamp() : null,
    archivedBy: archived ? callerUid : null,
  }, { merge: true });
  return { success: true };
});

/**
 * New residents self-register at "NEW_MEMBER" only (enforced here, never trusted
 * from the client) and start "Pending" until an admin approves them — see
 * approveMembership, which also handles the New Member -> General Member step-up.
 */
exports.registerResident = onCall(async (request) => {
  const uid = requireAuth(request);
  const { nameEn, nameBn, phone, holding, road, block } = request.data || {};

  if (!nameEn || !phone) {
    throw new HttpsError("invalid-argument", "nameEn and phone are required.");
  }

  const existing = await getResidentByUid(uid);
  if (existing) {
    throw new HttpsError("already-exists", "A profile already exists for this account.");
  }

  // Every brand-new account starts at the bottom of the hierarchy — onboarding/
  // pending, basic access only. This claim is what actually gates permissions;
  // the Firestore `role` field below is for display and firestore.rules reads.
  await admin.auth().setCustomUserClaims(uid, { role: "NEW_MEMBER", admin: false });

  const docRef = db.collection("users").doc();
  await docRef.set({
    firebaseUid: uid,
    nameEn,
    nameBn: nameBn || nameEn,
    phone,
    primaryContact: phone,
    holding: holding || "",
    road: road || "",
    block: block || "",
    role: "NEW_MEMBER",          // hardcoded — never taken from request.data
    membershipStatus: "Pending", // requires admin approval, see approveMembership
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { success: true, userId: docRef.id };
});

/**
 * Approves (or otherwise changes) a resident's membership status. Approving a
 * still-New Member also steps them up to GENERAL_MEMBER in the same call — per
 * the role spec, New Member is explicitly "before being upgraded to a General
 * Member," and approval is that upgrade moment. Doesn't touch anyone already at
 * Entrepreneurial Member or above.
 */
exports.approveMembership = onCall(async (request) => {
  const callerUid = await requireAdmin(request);
  const { userId, status } = request.data || {};
  if (!userId || !["Active", "Pending", "Suspended"].includes(status)) {
    throw new HttpsError("invalid-argument", "userId and a valid status are required.");
  }

  const userRef = db.collection("users").doc(userId);
  const userSnap = await userRef.get();
  if (!userSnap.exists) {
    throw new HttpsError("not-found", "Resident not found.");
  }
  const userData = userSnap.data();

  const update = {
    membershipStatus: status,
    statusChangedAt: admin.firestore.FieldValue.serverTimestamp(),
    statusChangedBy: callerUid,
  };

  if (status === "Active" && userData.role === "NEW_MEMBER" && userData.firebaseUid) {
    update.role = "GENERAL_MEMBER";
    update.roleChangedAt = admin.firestore.FieldValue.serverTimestamp();
    update.roleChangedBy = callerUid;
    await admin.auth().setCustomUserClaims(userData.firebaseUid, { role: "GENERAL_MEMBER", admin: false });
  }

  await userRef.update(update);
  return { success: true };
});

/**
 * Creates a Pending donation record for the current resident, to be paid via the
 * normal PipraPay checkout flow (initiatePipraPayCharge / confirmPipraPayPayment).
 * Replaces the old client-side processPayment(), which used to write "Completed"
 * directly with a fabricated transaction ID.
 */
exports.createPendingDonation = onCall(async (request) => {
  const uid = requireAuth(request);
  const { titleEn, titleBn, amount, purpose } = request.data || {};
  if (!titleEn || typeof amount !== "number" || amount <= 0) {
    throw new HttpsError("invalid-argument", "titleEn and a positive amount are required.");
  }

  const resident = await getResidentByUid(uid);
  if (!resident) {
    throw new HttpsError("failed-precondition", "No resident profile found for this account.");
  }

  const docRef = db.collection("financials").doc();
  await docRef.set({
    userId: resident.id,
    titleEn,
    titleBn: titleBn || titleEn,
    amount,
    type: "Donation",
    purpose: purpose || "",
    status: "Pending",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { success: true, financialRecordId: docRef.id };
});

// ---------------------------------------------------------------------------
// PipraPay — recommended path (self-hosted aggregator, no bKash merchant
// agreement needed). Same rule as bKash above: nothing here trusts a client-
// asserted "it's paid." Completion only ever comes from verifyPayment(), which
// calls PipraPay's own /verify-payments endpoint.
// ---------------------------------------------------------------------------

async function loadOwnedRecord(uid, financialRecordId) {
  const recordRef = db.collection("financials").doc(financialRecordId);
  const recordSnap = await recordRef.get();
  if (!recordSnap.exists) {
    throw new HttpsError("not-found", "Financial record not found.");
  }
  const record = recordSnap.data();
  const resident = await getResidentByUid(uid);
  if (!resident || resident.id !== record.userId) {
    throw new HttpsError("permission-denied", "You can only pay your own dues.");
  }
  return { recordRef, record, resident };
}

/**
 * Step 1: open a PipraPay checkout session for a specific due. Returns whatever
 * checkout URL PipraPay hands back for the client to open. No status change yet.
 */
exports.initiatePipraPayCharge = onCall(async (request) => {
  const uid = requireAuth(request);
  const { financialRecordId, redirectUrl, cancelUrl } = request.data || {};
  if (!financialRecordId || !redirectUrl || !cancelUrl) {
    throw new HttpsError("invalid-argument", "financialRecordId, redirectUrl and cancelUrl are required.");
  }

  const { recordRef, record, resident } = await loadOwnedRecord(uid, financialRecordId);
  if (record.status === "Completed") {
    throw new HttpsError("failed-precondition", "This due is already paid.");
  }

  // The webhook URL is this deployed function's own HTTPS endpoint — PipraPay
  // calls it server-to-server once the resident finishes checkout.
  const webhookUrl = `https://${process.env.GCLOUD_PROJECT || "YOUR-PROJECT-ID"}.cloudfunctions.net/piprapayWebhook`;

  const chargeResponse = await piprapay.createCharge({
    fullName: resident.nameEn || "Resident",
    emailOrMobile: resident.primaryContact || resident.phone,
    amount: record.amount,
    orderId: financialRecordId, // ties the webhook back to this exact record
    redirectUrl,
    cancelUrl,
    webhookUrl,
  });

  await recordRef.update({
    paymentGateway: "PipraPay",
    pendingPipraPayId: chargeResponse.pp_id || null,
    status: "AwaitingGatewayConfirmation",
  });

  return chargeResponse; // includes checkout_url / pp_id for the client to open
});

/**
 * Server-to-server webhook. This is the primary confirmation path — PipraPay calls
 * this directly once a payment finishes, independent of whether the resident's app
 * is even still open. Always re-verify via verifyPayment(); never trust the webhook
 * body's status field on its own, since a webhook payload is still just an HTTP
 * request from the internet until confirmed against PipraPay's own API.
 */
exports.piprapayWebhook = onRequest(async (req, res) => {
  try {
    const incomingKey = req.headers["mh-piprapay-api-key"];
    if (!incomingKey || incomingKey !== process.env.PIPRAPAY_API_KEY) {
      res.status(401).send("Invalid webhook signature");
      return;
    }

    const ppId = req.body && req.body.pp_id;
    if (!ppId) {
      res.status(400).send("Missing pp_id");
      return;
    }

    const verified = await piprapay.verifyPayment(ppId);
    const orderId = verified && verified.metadata && verified.metadata.order_id;
    if (!orderId) {
      res.status(400).send("Could not resolve order_id from verified payment");
      return;
    }

    const recordRef = db.collection("financials").doc(orderId);
    const isPaid = verified.status === "completed" || verified.status === "success" || verified.transaction_status === "Completed";

    if (isPaid) {
      await recordRef.update({
        status: "Completed",
        transactionId: verified.trx_id || verified.transaction_id || ppId,
        paymentGateway: "PipraPay",
        paymentDate: admin.firestore.FieldValue.serverTimestamp(),
        pendingPipraPayId: admin.firestore.FieldValue.delete(),
      });
    } else {
      await recordRef.update({ status: "Failed", pendingPipraPayId: admin.firestore.FieldValue.delete() });
    }

    // Acknowledge quickly — PipraPay only needs a 200 to consider the webhook delivered.
    res.status(200).send("OK");
  } catch (err) {
    console.error("piprapayWebhook error:", err);
    res.status(500).send("Internal error");
  }
});

/**
 * Fallback confirmation path for the browser-redirect flow (in case the webhook is
 * delayed or the network drops it): the client calls this with the pp_id it got
 * from the redirect, and we independently re-verify with PipraPay before trusting it.
 * Idempotent with the webhook — both converge on the same verifyPayment() call.
 */
exports.confirmPipraPayPayment = onCall(async (request) => {
  const uid = requireAuth(request);
  const { financialRecordId, ppId } = request.data || {};
  if (!financialRecordId || !ppId) {
    throw new HttpsError("invalid-argument", "financialRecordId and ppId are required.");
  }

  const { recordRef, record } = await loadOwnedRecord(uid, financialRecordId);
  if (record.status === "Completed") {
    return { success: true, status: "Completed", alreadyConfirmed: true };
  }

  const verified = await piprapay.verifyPayment(ppId);
  const isPaid = verified.status === "completed" || verified.status === "success" || verified.transaction_status === "Completed";

  if (isPaid) {
    await recordRef.update({
      status: "Completed",
      transactionId: verified.trx_id || verified.transaction_id || ppId,
      paymentGateway: "PipraPay",
      paymentDate: admin.firestore.FieldValue.serverTimestamp(),
      pendingPipraPayId: admin.firestore.FieldValue.delete(),
    });
    return { success: true, status: "Completed" };
  }

  await recordRef.update({ status: "Failed", pendingPipraPayId: admin.firestore.FieldValue.delete() });
  return { success: false, status: verified.status || "Failed" };
});

// ---------------------------------------------------------------------------
// bKash — kept as an alternative gateway if you later get direct merchant
// approval; PipraPay above is the faster path to a working integration today.
// ---------------------------------------------------------------------------

/**
 * Step 1 of a real payment: verify the resident owns this due, then open a
 * bKash payment session server-side and hand back the checkout URL. No status
 * is written yet — "Pending" stays until executePayment (step 2) confirms.
 */
exports.initiateBkashPayment = onCall(async (request) => {
  const uid = requireAuth(request);
  const { financialRecordId, callbackURL } = request.data || {};
  if (!financialRecordId || !callbackURL) {
    throw new HttpsError("invalid-argument", "financialRecordId and callbackURL are required.");
  }

  const recordRef = db.collection("financials").doc(financialRecordId);
  const recordSnap = await recordRef.get();
  if (!recordSnap.exists) {
    throw new HttpsError("not-found", "Financial record not found.");
  }
  const record = recordSnap.data();

  const resident = await getResidentByUid(uid);
  if (!resident || resident.id !== record.userId) {
    throw new HttpsError("permission-denied", "You can only pay your own dues.");
  }
  if (record.status === "Completed") {
    throw new HttpsError("failed-precondition", "This due is already paid.");
  }

  const { paymentID, bkashURL } = await bkash.createPayment({
    amount: record.amount,
    merchantInvoiceNumber: financialRecordId,
    payerReference: resident.holding || resident.id,
    callbackURL,
  });

  await recordRef.update({
    pendingPaymentId: paymentID,
    paymentGateway: "bKash",
    status: "AwaitingGatewayConfirmation",
  });

  return { paymentID, bkashURL };
});

/**
 * Step 2: after the resident completes checkout on bKash's page and the app
 * is redirected back, call this with the paymentID. bKash's OWN response is
 * what marks the record Completed — never a client-asserted status/txId.
 */
exports.executeBkashPayment = onCall(async (request) => {
  const uid = requireAuth(request);
  const { financialRecordId, paymentID } = request.data || {};
  if (!financialRecordId || !paymentID) {
    throw new HttpsError("invalid-argument", "financialRecordId and paymentID are required.");
  }

  const recordRef = db.collection("financials").doc(financialRecordId);
  const recordSnap = await recordRef.get();
  if (!recordSnap.exists) {
    throw new HttpsError("not-found", "Financial record not found.");
  }
  const record = recordSnap.data();

  const resident = await getResidentByUid(uid);
  if (!resident || resident.id !== record.userId) {
    throw new HttpsError("permission-denied", "You can only pay your own dues.");
  }
  if (record.pendingPaymentId !== paymentID) {
    throw new HttpsError("failed-precondition", "paymentID does not match the initiated session.");
  }

  const result = await bkash.executePayment(paymentID);

  if (result.transactionStatus === "Completed") {
    await recordRef.update({
      status: "Completed",
      transactionId: result.trxID,
      paymentGateway: "bKash",
      paymentDate: admin.firestore.FieldValue.serverTimestamp(),
      pendingPaymentId: admin.firestore.FieldValue.delete(),
    });
    return { success: true, status: "Completed", trxID: result.trxID };
  }

  await recordRef.update({ status: "Failed", pendingPaymentId: admin.firestore.FieldValue.delete() });
  return { success: false, status: result.transactionStatus || "Failed" };
});

/**
 * Admin-only manual ledger adjustment (e.g. correcting a due, recording a waiver).
 * financials/{} denies ALL client writes in firestore.rules, so this is now the
 * only path for adjustments too — not just payments.
 */
exports.recordFinancialAdjustment = onCall(async (request) => {
  await requireAdmin(request);
  const { targetUserId, titleEn, titleBn, amount, adjustmentType, noteEn, noteBn } = request.data || {};
  if (!targetUserId || !titleEn || typeof amount !== "number") {
    throw new HttpsError("invalid-argument", "targetUserId, titleEn and amount are required.");
  }

  const docRef = db.collection("financials").doc();
  await docRef.set({
    userId: targetUserId,
    titleEn,
    titleBn: titleBn || titleEn,
    amount,
    type: "Adjustment",
    adjustmentType: adjustmentType || "Manual",
    status: "Completed",
    paymentGateway: "Admin Adjustment",
    adminNoteEn: noteEn || "",
    adminNoteBn: noteBn || "",
    createdBy: request.auth.uid,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { success: true, recordId: docRef.id };
});

/**
 * Admin-only: issues a monthly maintenance due to every Active resident at once.
 * The web AdminPortalView's "Broadcast Billing Invoice" action used to just push
 * a fake record into local React state — nothing was ever persisted anywhere.
 */
exports.generateMonthlyDues = onCall(async (request) => {
  await requireAdmin(request);
  const { monthYear, amount, titleEn, titleBn } = request.data || {};
  if (!monthYear || typeof amount !== "number" || amount <= 0) {
    throw new HttpsError("invalid-argument", "monthYear and a positive amount are required.");
  }

  const activeResidents = await db.collection("users").where("membershipStatus", "==", "Active").get();
  if (activeResidents.empty) {
    return { success: true, count: 0 };
  }

  const batch = db.batch();
  activeResidents.docs.forEach((residentDoc) => {
    const docRef = db.collection("financials").doc();
    batch.set(docRef, {
      userId: residentDoc.id,
      titleEn: titleEn || `Monthly Maintenance Dues - ${monthYear}`,
      titleBn: titleBn || `মাসিক রক্ষণাবেক্ষণ ফি - ${monthYear}`,
      amount,
      type: "Due",
      monthYear,
      date: new Date().toISOString().split("T")[0],
      status: "Pending",
      createdBy: request.auth.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });
  await batch.commit();

  return { success: true, count: activeResidents.size };
});

// ---------------------------------------------------------------------------
// Complaints — status changes are admin-only, enforced server-side
// ---------------------------------------------------------------------------

exports.updateComplaintStatus = onCall(async (request) => {
  await requireAdmin(request);
  const { complaintId, status, adminNoteEn, adminNoteBn } = request.data || {};
  if (!complaintId || !["Pending", "Under Review", "Resolved"].includes(status)) {
    throw new HttpsError("invalid-argument", "complaintId and a valid status are required.");
  }

  await db.collection("complaints").doc(complaintId).update({
    status,
    adminNoteEn: adminNoteEn || "",
    adminNoteBn: adminNoteBn || "",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedBy: request.auth.uid,
  });

  return { success: true };
});

// ---------------------------------------------------------------------------
// Firestore Triggers — Automatic Public Profile Sync
// ---------------------------------------------------------------------------

/**
 * Automatically syncs non-sensitive public profile fields from `users/{userId}` to
 * `users_public/{userId}` whenever a user document is created, updated, or deleted.
 * Ensures the Resident Directory has access to public resident roster data without
 * exposing private NID/DOB or contact details.
 */
exports.syncPublicUser = onDocumentWritten("users/{userId}", async (event) => {
  const userId = event.params.userId;
  const afterData = event.data.after && event.data.after.exists ? event.data.after.data() : null;

  if (!afterData) {
    // Document deleted -> remove public directory entry
    await db.collection("users_public").doc(userId).delete();
    return;
  }

  const publicData = {
    firebaseUid: afterData.firebaseUid || "",
    nameEn: afterData.nameEn || "",
    nameBn: afterData.nameBn || afterData.nameEn || "",
    holding: afterData.holding || "",
    road: afterData.road || "",
    block: afterData.block || "",
    primaryContact: afterData.primaryContact || afterData.phone || "",
    professionEn: afterData.professionEn || "",
    membershipStatus: afterData.membershipStatus || "Pending",
    role: afterData.role || "NEW_MEMBER",
    profilePicUrl: afterData.profilePicUrl || "",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  await db.collection("users_public").doc(userId).set(publicData, { merge: true });
});

