# Kunjachaya Club — DevOps Fix Pass: Changelog

Everything below was found by reading the actual code and fixed by editing it — not simulated.
Two files to use: `kunjachaya-club-fixed.zip` (full repo, ready to open) and
`kunjachaya-club-fixes.patch` (a `git apply`-able diff if you'd rather merge into your own clone).

## 🔴 Critical — Authorization

- **Removed the client-controlled role toggle.** `registerWithEmail`, `loginWithEmail`,
  `loginWithPhone` no longer take a `selectedRole` parameter anywhere. New Cloud Function
  `registerResident` hardcodes `role: "Member"`, `membershipStatus: "Pending"` server-side.
  `setUserRole` is the only path to Admin, gated on an existing admin's Firebase custom claim
  (bootstrap steps in `functions/README.md`).
- **Fixed the biometric login bypass.** It now resolves the exact resident tied to the
  device's *persisted Firebase UID* (`FirebaseAuth.getInstance().currentUser`), never "any
  user matching the currently toggled role." If no real session exists yet, biometric
  quick-login isn't offered at all.
- **Admin nav item and Dev Docs were reachable by every user** in both the web sidebar and
  the Android bottom nav — neither had a role check. Fixed at both the nav-visibility level
  and the route-render level (defense in depth) in `Sidebar.jsx`, `App.jsx`, `MainActivity.kt`.
- **`firestore.rules` written from scratch.** Denies all direct client writes to `financials`;
  `complaints` are readable only by their owner or an admin (was `if isSignedIn()` — any
  resident could read everyone's complaints); `users.role`/`membershipStatus` can't be edited
  by the resident who owns the doc.
- Removed the hardcoded demo password (`ClubResident2026!`) and other pre-filled demo data
  from the login/registration form.

## 🔴 Critical — Payment Gateway (was 100% simulated)

- Built a real **PipraPay integration** (self-hosted aggregator, no bKash merchant agreement
  needed): `functions/piprapay.js` implements `createCharge`/`verifyPayment` against PipraPay's
  actual documented API. `initiatePipraPayCharge` (callable), `piprapayWebhook` (server-to-server
  confirmation — the primary path), and `confirmPipraPayPayment` (redirect-flow fallback) all
  live in `functions/index.js`. Nothing marks a due "Completed" except a verified response from
  PipraPay's own `/verify-payments` endpoint.
- Also implemented a real bKash Tokenized Checkout client (`functions/bkash.js`) as an
  alternative gateway, matching bKash's actual grant/create/execute flow, in case direct
  merchant approval comes through later.
- Replaced the Android UI's fake `delay(600)` progress-bar simulation and self-generated
  transaction ID with a real Custom Tabs checkout launch (`FinancialsScreen.kt`) tied to the
  functions above. Added the matching `<intent-filter>` in `AndroidManifest.xml` so the
  `kunjachayaclub://payment-redirect` callback actually returns to the app.
- Donations now create a real `Pending` record server-side (`createPendingDonation`) instead
  of instantly writing `Completed` with a fabricated ID.
- Admin ledger adjustments (`recordFinancialAdjustment`) were still doing a direct client
  write that the new Firestore rules would reject — caught this and rewired it through the
  matching Cloud Function.
- Removed the now-nonfunctional bKash/Nagad/Rocket gateway-picker chips and wallet-number
  field from both payment dialogs — PipraPay's own hosted checkout page is where that choice
  actually happens now; the old chips did nothing with the selection.

## 🟠 Logic bugs

- Replaced `hashCode().toLong()` document-ID derivation (a real collision risk at scale) with
  proper `@DocumentId`-annotated `firestoreId` string fields on `FinancialRecordEntity`,
  `ComplaintEntity`, and `AnnouncementEntity`. Caught mid-fix that the annotation itself was
  required — without it, Firestore's `toObject()` wouldn't have populated the field at all.
- Replaced fragile name-substring user matching (`nameEn.contains(email.substringBefore("@"))`)
  with exact `firebaseUid` lookups (`UserEntity.firebaseUid`, `UserDao.getUserByFirebaseUid`).
- Found and deleted an entire **orphaned duplicate ViewModel** (`FinancialsViewModel.kt`) with
  the same fake-payment pattern — it wasn't wired to any screen, so it was pure dead-code risk.
- Consolidated three independent `updateComplaintStatus` implementations (`ClubViewModel`,
  `ComplaintsViewModel`, `AdminDashboardViewModel` — only one of which even reached Firestore)
  into a single path through the `updateComplaintStatus` Cloud Function.
- Removed three now-unreachable client-write methods from `FirestoreRepository.kt`
  (`updateComplaintStatus`, `updateMembershipStatus`, `addFinancialRecord`) that would either
  fail under the new rules or, worse, quietly work if rules aren't deployed yet.

## 🟡 Data protection & dependencies

- Excluded the local Room DB (NID references, addresses, financial records) from Android's
  backup/data-extraction rules — was previously eligible for cloud backup and `adb backup`.
- Bumped `jspdf` (2.5.1 → 4.2.1) and `vite` (5.1.6 → 8.1.5, with `@vitejs/plugin-react` → 6.0.4)
  to clear the dompurify XSS and esbuild advisories. **Actually verified**: `npm audit` → 0
  vulnerabilities, `npm run build` → succeeds.

## Before you deploy

- `functions/README.md` covers secrets setup (`firebase functions:secrets:set ...`) and the
  one manual step that can't be automated by design: bootstrapping the first Admin via the
  Admin SDK directly, since there's deliberately no in-app path to grant that claim.
- You'll need your own PipraPay instance + API key (or bKash merchant sandbox credentials)
  and a real Firebase project — nothing here was compiled or run against live infrastructure.
- One known remaining gap, flagged rather than rushed: `firestore.rules` still allows any
  signed-in resident to read the full `users` collection (needed for the Directory feature).
  Properly fixing this means splitting `users` into public (name/phone/block) and private
  (NID/DOB/financial-adjacent) fields — a real schema change, not a one-line rule edit.
