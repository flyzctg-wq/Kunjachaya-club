/**
 * PipraPay client (self-hosted payment aggregator: bKash/Nagad/Rocket/etc behind one API).
 *
 * This is the recommended path for Kunjachaya Club instead of a direct bKash merchant
 * integration (bkash.js) — bKash's Tokenized Checkout requires a formal merchant
 * agreement with bKash directly, which is a slow approval process for a small
 * resident association. PipraPay is self-hosted (you run your own instance, e.g. on
 * the same cPanel/VPS as omeganetwork.bd) and gives you an API key from its own
 * admin dashboard immediately — no separate bKash merchant approval needed for MVP,
 * though PipraPay itself still settles through real bKash/Nagad/Rocket accounts
 * underneath.
 *
 * Flow (matches PipraPay's actual documented API):
 *   1. createCharge()  -> POST /api/create-charge, returns a checkout_url to redirect to
 *   2. Resident completes payment on PipraPay's hosted checkout page
 *   3. PipraPay calls YOUR webhook_url with { pp_id, ... }, OR redirects the browser
 *      back to redirect_url with pp_id as a query/body param
 *   4. You call verifyPayment(pp_id) -> POST /api/verify-payments — THIS response is
 *      the only thing allowed to mark a financial record "Completed" (see index.js)
 *
 * Config (Firebase secrets, never hardcoded):
 *   PIPRAPAY_BASE_URL   e.g. https://pay.omeganetwork.bd/api  (your own self-hosted instance)
 *                              or https://sandbox.piprapay.com/api for testing
 *   PIPRAPAY_API_KEY    from PipraPay admin -> Brand Setting -> API Setting
 *
 * Set with:
 *   firebase functions:secrets:set PIPRAPAY_API_KEY
 * (PIPRAPAY_BASE_URL can be a plain runtime env var since it's not secret.)
 *
 * You need a real, reachable PipraPay instance (self-hosted or the shared sandbox at
 * sandbox.piprapay.com for testing only — never point production dues collection at
 * the shared sandbox) and your own API key to actually call this. I can't fabricate
 * those for you. Everything else here — endpoint paths, header name, request/response
 * shape — matches PipraPay's published API reference.
 */

const axios = require("axios");

const BASE_URL = process.env.PIPRAPAY_BASE_URL || "https://sandbox.piprapay.com/api";

function authHeaders() {
  const apiKey = process.env.PIPRAPAY_API_KEY;
  if (!apiKey) {
    throw new Error("PIPRAPAY_API_KEY is not configured — set it via Firebase secrets first.");
  }
  return {
    accept: "application/json",
    "content-type": "application/json",
    "mh-piprapay-api-key": apiKey,
  };
}

/**
 * Starts a PipraPay checkout session. Returns whatever identifiers PipraPay hands
 * back (pp_id / checkout_url depending on API version) — no status is written to
 * our own DB from this step; the record stays "Pending" until verifyPayment confirms.
 */
async function createCharge({ fullName, emailOrMobile, amount, orderId, redirectUrl, cancelUrl, webhookUrl }) {
  const response = await axios.post(
    `${BASE_URL}/create-charge`,
    {
      full_name: fullName,
      email_mobile: emailOrMobile,
      amount: String(amount),
      metadata: { order_id: orderId },
      redirect_url: redirectUrl,
      return_type: "GET",
      cancel_url: cancelUrl,
      webhook_url: webhookUrl,
      currency: "BDT",
    },
    { headers: authHeaders(), timeout: 15000 }
  );

  const data = response.data;
  if (!data) {
    throw new Error("PipraPay create-charge returned an empty response.");
  }
  return data; // includes pp_id / checkout_url per PipraPay's response schema
}

/**
 * Re-confirms a payment directly with PipraPay using its own pp_id — this is the
 * step whose result may actually be trusted, whether it arrived via webhook or via
 * the browser redirect fallback. Never trust a client-submitted "it succeeded."
 */
async function verifyPayment(ppId) {
  const response = await axios.post(
    `${BASE_URL}/verify-payments`,
    { pp_id: ppId },
    { headers: authHeaders(), timeout: 15000 }
  );
  return response.data;
}

module.exports = { createCharge, verifyPayment };
