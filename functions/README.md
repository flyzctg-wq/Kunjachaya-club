# Kunjachaya Club — Cloud Functions

Server-enforced replacement for the old client-trusted role/payment logic.
See `index.js` for what each function does and why.

## Setup

```bash
cd functions
npm install
```

## Secrets (never commit these — set via Firebase, not .env in the repo)

```bash
firebase functions:secrets:set BKASH_USERNAME
firebase functions:secrets:set BKASH_PASSWORD
firebase functions:secrets:set BKASH_APP_KEY
firebase functions:secrets:set BKASH_APP_SECRET
firebase functions:secrets:set PIPRAPAY_API_KEY
```

Get real bKash values from bKash's merchant onboarding (sandbox first:
https://developer.bka.sh). Get a PipraPay API key from your own self-hosted
instance's admin dashboard (Brand Setting -> API Setting). Set `BKASH_BASE_URL`
and `PIPRAPAY_BASE_URL` as normal (non-secret) runtime env vars — sandbox by
default, switch to live only once you're ready.

## Role hierarchy

Five tiers, ranked highest to lowest, stored as a Firebase Auth custom claim
(`role`) — the actual enforcement point — and mirrored into the `role` field on
the resident's Firestore doc for display:

1. **SUPER_ADMIN** — absolute control. Only one at a time (see `transferOwnership`).
   Can archive/dissolve the workspace (`setWorkspaceArchived`), appoint or dismiss
   Admins, and do everything an Admin can.
2. **ADMIN** — day-to-day operations: approve/adjust Entrepreneurial, General, and
   New Members (`approveMembership`, `setUserRole`), manage resources, publish
   notices, schedule events, resolve complaints. Cannot appoint another Admin or
   touch the Super Admin seat — that's `setUserRole` throwing `permission-denied`
   by design, not an oversight.
3. **ENTREPRENEURIAL_MEMBER** — active contributors, on the leadership track.
4. **GENERAL_MEMBER** — standard active resident.
5. **NEW_MEMBER** — onboarding/pending. Every self-registration starts here;
   `approveMembership` steps a New Member up to General Member the moment their
   membership is approved.

`setUserRole` enforces the hierarchy itself (Admin can only reassign among tiers
3–5, and only for targets already at tiers 3–5; Super Admin can reassign anyone
except the Super Admin seat, which goes through `transferOwnership` instead so
there's never a moment with zero or two Super Admins).

## Bootstrapping the first Super Admin (one-time, manual, by design)

There is deliberately no in-app path to become an Admin or Super Admin —
`setUserRole` can only be called by someone who already holds one of those
claims, which is exactly the vulnerability this replaces. The very first Super
Admin has to be granted from a trusted operator machine with the Firebase Admin
SDK / CLI, e.g.:

```bash
node -e "
  const admin = require('firebase-admin');
  admin.initializeApp();
  admin.auth().setCustomUserClaims('<FIREBASE_UID_OF_FIRST_SUPER_ADMIN>', { role: 'SUPER_ADMIN', admin: true })
    .then(() => console.log('done'));
"
```

After that, the Super Admin can appoint Admins (`setUserRole`), and Admins can
manage member-tier promotions/approvals through the app itself.

## Deploy

```bash
firebase deploy --only functions,firestore:rules
```

## What changed vs. the old (fake) flow

- Payment status is now only ever set by `executeBkashPayment` / the PipraPay
  webhook, after the gateway's own API confirms the transaction — not by a
  client-side timer.
- Roles are only ever set by `setUserRole` / `transferOwnership`, gated on the
  caller's own custom claim and the hierarchy rules above — not by a dropdown
  on the login screen.
- `firestore.rules` denies all direct client writes to `financials` and enforces
  that `users.role` / `users.membershipStatus` can't be edited by the resident
  who owns the doc.
