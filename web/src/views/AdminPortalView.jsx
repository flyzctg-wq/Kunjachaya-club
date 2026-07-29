import React, { useState, useEffect } from 'react';
import { Shield, Plus, CheckCircle2, Megaphone, DollarSign, Send, AlertTriangle, Loader2, Crown, ArchiveRestore, UserCog } from 'lucide-react';
import { translations } from '../translations';
import { db, functions } from '../firebase';
import { collection, addDoc, serverTimestamp, onSnapshot, doc } from 'firebase/firestore';
import { httpsCallable } from 'firebase/functions';
import { isSuperAdmin } from '../services/roles';

// Every action in this panel used to just mutate local React state — publishing a
// notice, issuing dues, resolving a complaint all "worked" on screen and vanished
// on refresh, with nothing ever reaching Firestore or another resident's device.
// Notices go through a direct Firestore write (allowed for admins by
// firestore.rules); dues generation and complaint resolution go through the
// matching Cloud Functions, since financials/{} and complaints status changes
// are both server-only now.

export default function AdminPortalView({ lang, currentUser, complaints }) {
  const t = translations[lang || 'en'];
  const isSuper = isSuperAdmin(currentUser?.role);

  const [noticeTitle, setNoticeTitle] = useState('');
  const [noticeBody, setNoticeBody] = useState('');
  const [noticeCategory, setNoticeCategory] = useState('Urgent');
  const [postingNotice, setPostingNotice] = useState(false);

  const [duesMonth, setDuesMonth] = useState('August 2026');
  const [duesAmount, setDuesAmount] = useState(3500);
  const [issuingDues, setIssuingDues] = useState(false);

  const [resolvingId, setResolvingId] = useState(null);

  const [adminStatusMsg, setAdminStatusMsg] = useState('');
  const [adminErrorMsg, setAdminErrorMsg] = useState('');

  // Super-Admin-only state
  const [workspaceArchived, setWorkspaceArchived] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [newOwnerUid, setNewOwnerUid] = useState('');
  const [transferring, setTransferring] = useState(false);
  const [showTransferConfirm, setShowTransferConfirm] = useState(false);

  useEffect(() => {
    const unsub = onSnapshot(doc(db, 'clubSettings', 'workspace'), (snap) => {
      setWorkspaceArchived(!!snap.data()?.archived);
    });
    return unsub;
  }, []);

  const flash = (msg) => {
    setAdminStatusMsg(msg);
    setTimeout(() => setAdminStatusMsg(''), 5000);
  };
  const flashError = (msg) => {
    setAdminErrorMsg(msg);
    setTimeout(() => setAdminErrorMsg(''), 6000);
  };

  const handlePostNotice = async (e) => {
    e.preventDefault();
    if (!noticeTitle.trim() || !noticeBody.trim()) return;
    setPostingNotice(true);
    try {
      // Direct Firestore write — firestore.rules allows announcements writes for
      // admins (request.auth.token.admin === true), enforced server-side.
      await addDoc(collection(db, 'announcements'), {
        titleEn: noticeTitle,
        titleBn: noticeTitle,
        descriptionEn: noticeBody,
        descriptionBn: noticeBody,
        categoryEn: noticeCategory,
        categoryBn: noticeCategory,
        priority: noticeCategory === 'Urgent' ? 'High' : 'Medium',
        author: 'Club Management Committee',
        date: new Date().toISOString().split('T')[0],
        createdAt: serverTimestamp(),
      });
      setNoticeTitle('');
      setNoticeBody('');
      flash('Official Notice published & broadcasted to all resident devices!');
    } catch (err) {
      flashError(err.message || 'Could not publish notice — check your admin permissions.');
    } finally {
      setPostingNotice(false);
    }
  };

  const handleGenerateDues = async (e) => {
    e.preventDefault();
    setIssuingDues(true);
    try {
      const generateMonthlyDues = httpsCallable(functions, 'generateMonthlyDues');
      const res = await generateMonthlyDues({ monthYear: duesMonth, amount: Number(duesAmount) });
      flash(`Issued BDT ${duesAmount} monthly maintenance invoice for ${duesMonth} to ${res.data?.count ?? 0} active resident flat(s)!`);
    } catch (err) {
      flashError(err.message || 'Could not issue dues — check your admin permissions.');
    } finally {
      setIssuingDues(false);
    }
  };

  const handleResolveComplaint = async (complaint) => {
    if (!complaint.firestoreId) return;
    setResolvingId(complaint.firestoreId);
    try {
      const updateComplaintStatus = httpsCallable(functions, 'updateComplaintStatus');
      await updateComplaintStatus({
        complaintId: complaint.firestoreId,
        status: 'Resolved',
        adminNoteEn: 'Resolved by Executive Maintenance Team.',
        adminNoteBn: 'নির্বাহী রক্ষণাবেক্ষণ দল কর্তৃক সমাধান করা হয়েছে।',
      });
    } catch (err) {
      flashError(err.message || 'Could not resolve complaint — check your admin permissions.');
    } finally {
      setResolvingId(null);
    }
  };

  // --- Super-Admin-only actions below. Both call Cloud Functions that enforce
  // requireSuperAdmin() server-side — this UI gate (isSuper) is just so an
  // Admin doesn't even see controls they can't use, not the actual security
  // boundary, which lives in functions/index.js. ---

  const handleToggleArchive = async () => {
    const nextState = !workspaceArchived;
    const confirmMsg = nextState
      ? 'Archive the entire club workspace? Residents will see a read-only closed notice until this is reversed. Data is NOT deleted.'
      : 'Reopen the club workspace for all residents?';
    if (!window.confirm(confirmMsg)) return;

    setArchiving(true);
    try {
      const setWorkspaceArchivedFn = httpsCallable(functions, 'setWorkspaceArchived');
      await setWorkspaceArchivedFn({ archived: nextState });
      flash(nextState ? 'Workspace archived.' : 'Workspace reopened.');
    } catch (err) {
      flashError(err.message || 'Could not update workspace status — Super Admin only.');
    } finally {
      setArchiving(false);
    }
  };

  const handleTransferOwnership = async () => {
    if (!newOwnerUid.trim()) return;
    setTransferring(true);
    try {
      const transferOwnership = httpsCallable(functions, 'transferOwnership');
      await transferOwnership({ newSuperAdminUid: newOwnerUid.trim() });
      flash('Ownership transferred. You are now an Admin; the new account is Super Admin.');
      setNewOwnerUid('');
      setShowTransferConfirm(false);
    } catch (err) {
      flashError(err.message || 'Could not transfer ownership.');
    } finally {
      setTransferring(false);
    }
  };

  return (
    <div className="space-y-6">
      
      {/* Admin Header */}
      <div className="bg-slate-900 text-white p-6 rounded-2xl shadow-xl border border-slate-800 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <span className="px-3 py-1 bg-amber-500 text-slate-950 font-bold text-xs uppercase tracking-wider rounded-md flex items-center gap-1 w-fit">
            <Shield className="w-3.5 h-3.5" />
            Executive Admin
          </span>
          <h2 className="text-2xl font-bold mt-2">
            {t.adminDashboard}
          </h2>
          <p className="text-xs text-slate-400">
            Publish announcements, issue billing runs, and resolve resident service tickets
          </p>
        </div>
      </div>

      {adminStatusMsg && (
        <div className="p-4 bg-emerald-500 text-white rounded-xl font-bold text-sm flex items-center gap-2 shadow-lg">
          <CheckCircle2 className="w-5 h-5 shrink-0" />
          <span>{adminStatusMsg}</span>
        </div>
      )}
      {adminErrorMsg && (
        <div className="p-4 bg-rose-500 text-white rounded-xl font-bold text-sm flex items-center gap-2 shadow-lg">
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <span>{adminErrorMsg}</span>
        </div>
      )}

      {workspaceArchived && (
        <div className="p-4 bg-slate-800 text-white rounded-xl font-bold text-sm flex items-center gap-2 shadow-lg border border-amber-500/40">
          <ArchiveRestore className="w-5 h-5 shrink-0 text-amber-400" />
          <span>This workspace is currently archived — residents see a read-only closed notice.</span>
        </div>
      )}

      {/* Super Admin Only: Absolute Control */}
      {isSuper && (
        <div className="bg-gradient-to-br from-amber-50 to-white dark:from-amber-950/20 dark:to-slate-800 p-6 rounded-2xl border-2 border-amber-300 dark:border-amber-700/60 shadow-sm space-y-4">
          <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white border-b border-amber-200 dark:border-amber-800/60 pb-3">
            <Crown className="w-5 h-5 text-amber-500" />
            <span>Super Admin — Absolute Control</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <p className="text-xs font-bold text-slate-700 dark:text-slate-300">Workspace Status</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Archiving is reversible and never deletes financial or complaint history — it only puts the club into a read-only state for all residents.
              </p>
              <button
                onClick={handleToggleArchive}
                disabled={archiving}
                className={`w-full py-2.5 font-bold text-xs rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-60 ${
                  workspaceArchived
                    ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                    : 'bg-slate-800 hover:bg-slate-900 text-white'
                }`}
              >
                {archiving ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArchiveRestore className="w-4 h-4" />}
                <span>{workspaceArchived ? 'Reopen Workspace' : 'Archive / Dissolve Workspace'}</span>
              </button>
            </div>

            <div className="space-y-2">
              <p className="text-xs font-bold text-slate-700 dark:text-slate-300">Transfer Ownership</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Hands the Super Admin seat to another account's Firebase UID. You will be demoted to Admin in the same step — this cannot be undone by you afterward.
              </p>
              <input
                type="text"
                value={newOwnerUid}
                onChange={(e) => setNewOwnerUid(e.target.value)}
                placeholder="New Super Admin's Firebase UID"
                className="w-full px-3 py-2 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-lg text-xs font-mono"
              />
              {!showTransferConfirm ? (
                <button
                  onClick={() => newOwnerUid.trim() && setShowTransferConfirm(true)}
                  disabled={!newOwnerUid.trim()}
                  className="w-full py-2.5 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-40"
                >
                  <UserCog className="w-4 h-4" />
                  <span>Transfer Ownership…</span>
                </button>
              ) : (
                <div className="space-y-2">
                  <p className="text-xs font-bold text-rose-600">Are you sure? This is immediate and you lose Super Admin access.</p>
                  <div className="flex gap-2">
                    <button
                      onClick={handleTransferOwnership}
                      disabled={transferring}
                      className="flex-1 py-2 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-60"
                    >
                      {transferring && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                      Confirm Transfer
                    </button>
                    <button
                      onClick={() => setShowTransferConfirm(false)}
                      className="flex-1 py-2 bg-slate-200 dark:bg-slate-700 text-slate-800 dark:text-slate-200 font-bold text-xs rounded-xl transition"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Admin Tools Two Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Post Notice Form */}
        <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
          <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white border-b border-slate-100 dark:border-slate-700 pb-3">
            <Megaphone className="w-5 h-5 text-indigo-500" />
            <span>{t.postNotice}</span>
          </div>

          <form onSubmit={handlePostNotice} className="space-y-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">Notice Title</label>
              <input 
                type="text"
                required
                value={noticeTitle}
                onChange={(e) => setNoticeTitle(e.target.value)}
                placeholder="e.g. Annual General Body Meeting 2026"
                className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">Category</label>
              <select
                value={noticeCategory}
                onChange={(e) => setNoticeCategory(e.target.value)}
                className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm"
              >
                <option value="Urgent">Urgent Notice</option>
                <option value="Maintenance">Maintenance Alert</option>
                <option value="General">General Announcement</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">Notice Content</label>
              <textarea 
                rows="3"
                required
                value={noticeBody}
                onChange={(e) => setNoticeBody(e.target.value)}
                placeholder="Notice details..."
                className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm"
              />
            </div>

            <button
              type="submit"
              disabled={postingNotice}
              className="w-full py-2.5 bg-brand-500 hover:bg-brand-600 text-white font-bold text-xs rounded-xl transition flex items-center justify-center gap-2 shadow-md disabled:opacity-60"
            >
              {postingNotice ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              <span>Publish Notice</span>
            </button>
          </form>
        </div>

        {/* Issue Dues Form */}
        <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
          <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white border-b border-slate-100 dark:border-slate-700 pb-3">
            <DollarSign className="w-5 h-5 text-emerald-500" />
            <span>{t.generateMonthlyDues}</span>
          </div>

          <form onSubmit={handleGenerateDues} className="space-y-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">Billing Period / Month</label>
              <input 
                type="text"
                required
                value={duesMonth}
                onChange={(e) => setDuesMonth(e.target.value)}
                placeholder="e.g. August 2026"
                className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">Monthly Charge per Flat (BDT)</label>
              <input 
                type="number"
                required
                value={duesAmount}
                onChange={(e) => setDuesAmount(e.target.value)}
                className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm"
              />
            </div>

            <div className="p-3 bg-slate-50 dark:bg-slate-900 rounded-xl text-xs text-slate-500">
              * This generates a real, individual Pending invoice for every Active resident — enforced server-side.
            </div>

            <button
              type="submit"
              disabled={issuingDues}
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl transition flex items-center justify-center gap-2 shadow-md disabled:opacity-60"
            >
              {issuingDues ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              <span>Broadcast Billing Invoice</span>
            </button>
          </form>
        </div>

      </div>

      {/* Pending Tickets Resolution Section */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
        <h3 className="font-bold text-slate-900 dark:text-white text-base">
          Pending Resident Tickets ({complaints.filter(c => c.status !== 'Resolved').length})
        </h3>
        
        <div className="divide-y divide-slate-100 dark:divide-slate-700">
          {complaints.filter(c => c.status !== 'Resolved').map(c => (
            <div key={c.firestoreId || c.id} className="py-3 flex items-center justify-between gap-4">
              <div>
                <span className="text-xs font-bold text-slate-400">Flat {c.holdingNo} • {c.categoryEn}</span>
                <p className="font-bold text-sm text-slate-800 dark:text-slate-200">{c.titleEn}</p>
              </div>
              <button
                onClick={() => handleResolveComplaint(c)}
                disabled={resolvingId === c.firestoreId}
                className="px-3 py-1.5 bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold rounded-lg transition shrink-0 disabled:opacity-60 flex items-center gap-1.5"
              >
                {resolvingId === c.firestoreId && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Mark Resolved
              </button>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}
