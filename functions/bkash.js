/**
 * bKash Tokenized Checkout (PGW) API client.
 *
 * This implements bKash's actual documented merchant integration flow:
 *   1. Grant Token   -> get a short-lived id_token using merchant app credentials
 *   2. Create Payment -> open a payment session, get back a bkashURL to redirect the payer to
 *   3. Execute Payment -> AFTER the payer completes checkout on bKash's own page/app,
 *                         the merchant calls Execute to get bKash's own confirmation
 *                         (trxID + transactionStatus). This is the only step that may
 *                         ever mark a record "Completed" — never a client-submitted flag.
 *
 * Credentials come from environment config, never hardcoded:
 *   BKASH_BASE_URL       e.g. https://tokenized.sandbox.bka.sh/v1.2.0-beta (sandbox)
 *                             or https://tokenized.pay.bka.sh/v1.2.0-beta (live)
 *   BKASH_USERNAME
 *   BKASH_PASSWORD
 *   BKASH_APP_KEY
 *   BKASH_APP_SECRET
 *
 * Set these with:
 *   firebase functions:secrets:set BKASH_USERNAME
 *   firebase functions:secrets:set BKASH_PASSWORD
 *   firebase functions:secrets:set BKASH_APP_KEY
 *   firebase functions:secrets:set BKASH_APP_SECRET
 * (BKASH_BASE_URL can stay a plain env var / runtime config since it isn't a secret.)
 *
 * You need real sandbox credentials from bKash's merchant onboarding to actually
 * call this — I don't have those and can't fabricate them. Everything else here
 * (request shapes, headers, endpoints, flow order) matches bKash's published PGW spec.
 */

const axios = require("axios");

const BASE_URL = process.env.BKASH_BASE_URL || "https://tokenized.sandbox.bka.sh/v1.2.0-beta";

let cachedToken = null; // { id_token, refresh_token, expiresAt }

async function grantToken() {
  const now = Date.now();
  if (cachedToken && cachedToken.expiresAt > now + 30_000) {
    return cachedToken.id_token;
  }

  const username = process.env.BKASH_USERNAME;
  const password = process.env.BKASH_PASSWORD;
  const appKey = process.env.BKASH_APP_KEY;
  const appSecret = process.env.BKASH_APP_SECRET;

  if (!username || !password || !appKey || !appSecret) {
    throw new Error(
      "bKash credentials are not configured. Set BKASH_USERNAME, BKASH_PASSWORD, " +
      "BKASH_APP_KEY, BKASH_APP_SECRET via Firebase secrets before calling this function."
    );
  }

  const response = await axios.post(
    `${BASE_URL}/tokenized/checkout/token/grant`,
    { app_key: appKey, app_secret: appSecret },
    {
      headers: {
        username,
        password,
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      timeout: 15000,
    }
  );

  const data = response.data;
  if (!data || !data.id_token) {
    throw new Error(`bKash token grant failed: ${JSON.stringify(data)}`);
  }

  // bKash's grant tokens are typically valid ~1 hour; refresh a bit early.
  cachedToken = {
    id_token: data.id_token,
    refresh_token: data.refresh_token,
    expiresAt: now + (Number(data.expires_in || 3300) * 1000),
  };
  return cachedToken.id_token;
}

function authHeaders(idToken) {
  return {
    Authorization: idToken,
    "X-APP-Key": process.env.BKASH_APP_KEY,
    "Content-Type": "application/json",
    Accept: "application/json",
  };
}

/**
 * Create a bKash payment session. Returns { paymentID, bkashURL }.
 * The client should redirect/open a webview to bkashURL for the resident to
 * authorize payment on bKash's own interface — we never collect wallet PINs ourselves.
 */
async function createPayment({ amount, merchantInvoiceNumber, payerReference, callbackURL }) {
  const idToken = await grantToken();

  const response = await axios.post(
    `${BASE_URL}/tokenized/checkout/create`,
    {
      mode: "0011",
      payerReference: String(payerReference).slice(0, 20),
      callbackURL,
      amount: Number(amount).toFixed(2),
      currency: "BDT",
      intent: "sale",
      merchantInvoiceNumber,
    },
    { headers: authHeaders(idToken), timeout: 15000 }
  );

  const data = response.data;
  if (!data || !data.paymentID || !data.bkashURL) {
    throw new Error(`bKash create payment failed: ${JSON.stringify(data)}`);
  }

  return { paymentID: data.paymentID, bkashURL: data.bkashURL };
}

/**
 * Execute (confirm) a previously created payment after the payer completes checkout.
 * This is the ONLY call whose result may be trusted to mark a due "Completed" — it
 * comes back from bKash itself, not from anything the app sent.
 * Returns { transactionStatus, trxID, amount, paymentID } as reported by bKash.
 */
async function executePayment(paymentID) {
  const idToken = await grantToken();

  const response = await axios.post(
    `${BASE_URL}/tokenized/checkout/execute/${encodeURIComponent(paymentID)}`,
    {},
    { headers: authHeaders(idToken), timeout: 20000 }
  );

  const data = response.data;
  if (!data || !data.transactionStatus) {
    throw new Error(`bKash execute payment failed: ${JSON.stringify(data)}`);
  }

  return {
    transactionStatus: data.transactionStatus, // "Completed" | "Failed" | ...
    trxID: data.trxID,
    amount: data.amount,
    paymentID: data.paymentID,
  };
}

/**
 * Independently re-query a payment's status. Useful for reconciliation jobs / webhooks
 * that arrive out of order, or if a client disconnects mid-flow and never called execute.
 */
async function queryPaymentStatus(paymentID) {
  const idToken = await grantToken();

  const response = await axios.post(
    `${BASE_URL}/tokenized/checkout/payment/status/${encodeURIComponent(paymentID)}`,
    {},
    { headers: authHeaders(idToken), timeout: 15000 }
  );

  return response.data;
}

module.exports = { createPayment, executePayment, queryPaymentStatus };
