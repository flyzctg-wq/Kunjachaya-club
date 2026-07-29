import React, { useState, useEffect } from 'react';
import {
  CreditCard,
  Download,
  CheckCircle,
  Clock,
  AlertCircle,
  ShieldCheck,
  X,
  Receipt,
  Loader2
} from 'lucide-react';
import { translations } from '../translations';
import { generatePdfReceipt } from '../pdfGenerator';
import { functions } from '../firebase';
import { httpsCallable } from 'firebase/functions';

// This used to fake the entire payment flow: a setTimeout, a client-generated
// transaction ID, and a direct write of status:'PAID' into local state — no
// gateway was ever actually contacted. It now calls the real PipraPay Cloud
// Functions (functions/index.js) and never marks anything paid itself; the
// Firestore listener in App.jsx picks up the real status once the server
// confirms it with PipraPay.

const PENDING_KEY = 'kunjachaya_pending_payment_record_id';

function formatDate(value) {
  if (!value) return '—';
  if (typeof value === 'string') return value;
  if (value.toDate) return value.toDate().toLocaleDateString(); // Firestore Timestamp
  return String(value);
}

export default function FinancialsView({ lang, currentUser, financials }) {
  const t = translations[lang];

  const [selectedRecord, setSelectedRecord] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [confirming, setConfirming] = useState(false);

  // Handle the redirect back from PipraPay's hosted checkout page. This is a
  // fallback confirmation path — the server-to-server webhook (piprapayWebhook)
  // is the primary one and usually resolves first; this just closes the loop if
  // the resident is looking at the screen when they return.
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const ppId = params.get('pp_id');
    const pendingRecordId = localStorage.getItem(PENDING_KEY);

    if (params.get('pay_return') && ppId && pendingRecordId) {
      setConfirming(true);
      const confirmPipraPayPayment = httpsCallable(functions, 'confirmPipraPayPayment');
      confirmPipraPayPayment({ financialRecordId: pendingRecordId, ppId })
        .then((res) => {
          if (res.data?.success) {
            setSuccessMessage(t.paymentSuccess || 'Payment confirmed!');
          } else {
            setErrorMessage(
              lang === 'bn'
                ? 'পেমেন্ট এখনো নিশ্চিত হয়নি। কিছুক্ষণ পর আবার চেক করুন।'
                : "Payment isn't confirmed yet — it may still be processing. Check back shortly."
            );
          }
        })
        .catch((err) => setErrorMessage(err.message))
        .finally(() => {
          localStorage.removeItem(PENDING_KEY);
          setConfirming(false);
          // Clean the query string so a page refresh doesn't re-trigger this.
          window.history.replaceState({}, '', window.location.pathname);
        });
    } else if (params.get('pay_cancel')) {
      localStorage.removeItem(PENDING_KEY);
      window.history.replaceState({}, '', window.location.pathname);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handlePay = async (e) => {
    e.preventDefault();
    if (!selectedRecord) return;
    if (!selectedRecord.firestoreId) {
      setErrorMessage('This record has no server ID yet — please refresh and try again.');
      return;
    }

    setIsProcessing(true);
    setErrorMessage('');
    try {
      const redirectUrl = `${window.location.origin}${window.location.pathname}?pay_return=1`;
      const cancelUrl = `${window.location.origin}${window.location.pathname}?pay_cancel=1`;

      const initiatePipraPayCharge = httpsCallable(functions, 'initiatePipraPayCharge');
      const res = await initiatePipraPayCharge({
        financialRecordId: selectedRecord.firestoreId,
        redirectUrl,
        cancelUrl,
      });

      const checkoutUrl = res.data?.checkout_url || res.data?.payment_url;
      if (!checkoutUrl) {
        throw new Error('PipraPay did not return a checkout URL.');
      }

      // Full-page navigation is required here (not client-side routing) since
      // we're handing off to PipraPay's own hosted page. Stash which record
      // we're paying so the redirect-return handler above can confirm it.
      localStorage.setItem(PENDING_KEY, selectedRecord.firestoreId);
      window.location.href = checkoutUrl;
    } catch (err) {
      setErrorMessage(err.message || 'Could not start checkout.');
      setIsProcessing(false);
    }
  };

  return (
    <div className="space-y-6">

      {/* Header Summary Cards */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="px-3 py-1 bg-brand-50 text-brand-600 dark:bg-brand-950/60 dark:text-brand-300 text-xs font-bold uppercase tracking-wider rounded-md">
            {currentUser?.flatNo ? `Flat ${currentUser.flatNo} Ledger` : 'My Ledger'}
          </span>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-white mt-2">
            {t.navFinancials}
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Official dues payment gateway & downloadable digital vouchers
          </p>
        </div>

        <div className="flex gap-3">
          <div className="px-4 py-2 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-xl text-right">
            <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">Status</span>
            <div className="text-sm font-bold text-emerald-700 dark:text-emerald-300">Verified Resident</div>
          </div>
        </div>
      </div>

      {confirming && (
        <div className="p-4 bg-brand-500 text-white rounded-xl font-medium text-sm flex items-center gap-2 shadow-lg">
          <Loader2 className="w-5 h-5 animate-spin shrink-0" />
          <span>{lang === 'bn' ? 'পেমেন্ট নিশ্চিত করা হচ্ছে...' : 'Confirming payment with PipraPay...'}</span>
        </div>
      )}

      {successMessage && (
        <div className="p-4 bg-emerald-500 text-white rounded-xl font-medium text-sm flex items-center justify-between shadow-lg">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-5 h-5 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage('')} className="p-1 hover:bg-white/20 rounded" aria-label="Dismiss">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 bg-rose-500 text-white rounded-xl font-medium text-sm flex items-center justify-between shadow-lg">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage('')} className="p-1 hover:bg-white/20 rounded" aria-label="Dismiss">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Dues Cards Grid */}
      <div className="space-y-4">
        <h3 className="font-bold text-slate-800 dark:text-slate-200 text-lg flex items-center gap-2">
          <Receipt className="w-5 h-5 text-brand-500" />
          <span>Billing Records & Invoices</span>
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {financials.map((item) => {
            const isPaid = item.status === 'Completed';
            const isAwaiting = item.status === 'AwaitingGatewayConfirmation';
            const isFailed = item.status === 'Failed';

            return (
              <div
                key={item.firestoreId || item.id}
                className={`
                  p-5 rounded-2xl border transition shadow-sm bg-white dark:bg-slate-800 flex flex-col justify-between
                  ${isPaid ? 'border-slate-200 dark:border-slate-700' : 'border-amber-300 dark:border-amber-700/80 ring-2 ring-amber-500/10'}
                `}
              >
                <div>
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      {item.monthYear || item.date}
                    </span>
                    <span className={`
                      px-2.5 py-1 text-xs font-extrabold rounded-full uppercase tracking-wider flex items-center gap-1
                      ${isPaid
                        ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/80 dark:text-emerald-300'
                        : isFailed
                        ? 'bg-rose-100 text-rose-700 dark:bg-rose-950/80 dark:text-rose-300'
                        : 'bg-amber-100 text-amber-800 dark:bg-amber-950/80 dark:text-amber-300'}
                    `}>
                      {isPaid ? <CheckCircle className="w-3.5 h-3.5" /> : isAwaiting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Clock className="w-3.5 h-3.5" />}
                      {isPaid ? t.statusPaid : isAwaiting ? 'Awaiting Confirmation' : isFailed ? 'Failed' : t.statusPending}
                    </span>
                  </div>

                  <h4 className="font-bold text-slate-900 dark:text-slate-100 text-base mt-2">
                    {lang === 'bn' ? item.titleBn : item.titleEn}
                  </h4>

                  <div className="mt-4 flex items-baseline gap-2">
                    <span className="text-2xl font-extrabold text-slate-900 dark:text-white">
                      BDT {Number(item.amount || 0).toLocaleString()}
                    </span>
                    <span className="text-xs text-slate-400">Date: {item.date}</span>
                  </div>

                  {isPaid && item.transactionId && (
                    <div className="mt-2 text-xs text-slate-500 dark:text-slate-400 bg-slate-50 dark:bg-slate-900 p-2 rounded-lg font-mono">
                      TxRef: {item.transactionId} | Gateway: {item.paymentGateway} | Date: {formatDate(item.paymentDate)}
                    </div>
                  )}
                </div>

                <div className="mt-5 pt-3 border-t border-slate-100 dark:border-slate-700/60 flex items-center justify-end gap-2">
                  {isPaid ? (
                    <button
                      onClick={() => generatePdfReceipt(item, currentUser || { name: 'Resident', flatNo: '4-A' }, lang)}
                      className="w-full sm:w-auto px-4 py-2 bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-800 dark:text-slate-200 font-semibold text-xs rounded-xl transition flex items-center justify-center gap-2"
                    >
                      <Download className="w-4 h-4 text-brand-500" />
                      <span>{t.downloadReceipt}</span>
                    </button>
                  ) : isAwaiting ? (
                    <span className="text-xs text-amber-600 dark:text-amber-400 font-semibold">
                      {lang === 'bn' ? 'নিশ্চিতকরণের অপেক্ষায়...' : 'Waiting on gateway confirmation…'}
                    </span>
                  ) : (
                    <button
                      onClick={() => setSelectedRecord(item)}
                      className="w-full sm:w-auto px-5 py-2 bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold text-xs rounded-xl transition flex items-center justify-center gap-2 shadow-md shadow-amber-500/20"
                    >
                      <CreditCard className="w-4 h-4" />
                      <span>{t.payNow}</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Real Checkout Handoff Modal */}
      {selectedRecord && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 w-full max-w-md rounded-2xl p-6 shadow-2xl border border-slate-200 dark:border-slate-700">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-700">
              <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
                <CreditCard className="w-5 h-5 text-amber-500" />
                <span>{t.selectPaymentMethod}</span>
              </div>
              <button onClick={() => setSelectedRecord(null)} className="p-1 text-slate-400 hover:text-slate-600" aria-label="Close">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handlePay} className="mt-4 space-y-4">
              <div className="p-3 bg-amber-50 dark:bg-amber-950/50 rounded-xl border border-amber-200 dark:border-amber-800">
                <p className="text-xs font-semibold text-amber-800 dark:text-amber-300">Bill Amount</p>
                <div className="text-xl font-bold text-slate-900 dark:text-white">
                  BDT {Number(selectedRecord.amount || 0).toLocaleString()}
                </div>
                <p className="text-xs text-slate-500 mt-0.5">{selectedRecord.titleEn}</p>
              </div>

              {/* No gateway picker here — PipraPay's own hosted checkout page is
                  where bKash/Nagad/Rocket/Card is actually chosen. */}
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {lang === 'bn'
                  ? 'পরবর্তী পেজে আপনি bKash, Nagad, Rocket বা কার্ড থেকে বেছে নিতে পারবেন।'
                  : "You'll choose bKash, Nagad, Rocket, or Card on PipraPay's secure checkout page next."}
              </p>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={isProcessing}
                  className="w-full py-3 bg-brand-500 hover:bg-brand-600 text-white font-bold text-sm rounded-xl transition shadow-lg flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {isProcessing ? <Loader2 className="w-5 h-5 animate-spin" /> : <ShieldCheck className="w-5 h-5" />}
                  <span>{isProcessing ? 'Opening secure checkout...' : `${t.payNow} (BDT ${Number(selectedRecord.amount || 0).toLocaleString()})`}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
