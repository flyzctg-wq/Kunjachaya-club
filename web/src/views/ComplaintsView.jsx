import React, { useState } from 'react';
import { Wrench, Plus, CheckCircle, Clock, AlertTriangle, Send, X, MessageSquare, Loader2 } from 'lucide-react';
import { translations } from '../translations';
import { db } from '../firebase';
import { collection, addDoc, serverTimestamp } from 'firebase/firestore';

// Submitting used to only touch local React state — nothing was ever written to
// Firestore, so a "submitted" complaint vanished on refresh and no admin ever saw
// it. This now writes a real document (Firestore rules allow residents to create
// their own complaint, always as "Pending" — status changes after that are
// admin-only, via the updateComplaintStatus Cloud Function).

export default function ComplaintsView({ lang, currentUser, complaints }) {
  const t = translations[lang];

  const [showNewModal, setShowNewModal] = useState(false);
  const [category, setCategory] = useState('Plumbing');
  const [titleInput, setTitleInput] = useState('');
  const [descInput, setDescInput] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!titleInput.trim() || !descInput.trim() || !currentUser?.id) return;

    setSubmitting(true);
    setError('');
    try {
      await addDoc(collection(db, 'complaints'), {
        userId: currentUser.id,
        userNameEn: currentUser.name || '',
        userNameBn: currentUser.nameBn || currentUser.name || '',
        holdingNo: currentUser.flatNo || '',
        titleEn: titleInput,
        titleBn: titleInput,
        categoryEn: category,
        categoryBn: category,
        descriptionEn: descInput,
        descriptionBn: descInput,
        status: 'Pending',
        adminNoteEn: '',
        adminNoteBn: '',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setTitleInput('');
      setDescInput('');
      setShowNewModal(false);
    } catch (err) {
      setError(err.message || 'Could not submit complaint.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <span className="px-3 py-1 bg-amber-50 text-amber-600 dark:bg-amber-950/60 dark:text-amber-300 text-xs font-bold uppercase tracking-wider rounded-md">
            Maintenance Helpdesk
          </span>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-white mt-2">
            {t.navComplaints}
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Submit water, electrical, lift, or security maintenance tickets for Flat {currentUser?.flatNo || '4-A'}
          </p>
        </div>

        <button
          onClick={() => setShowNewModal(true)}
          className="px-4 py-2.5 bg-brand-500 hover:bg-brand-600 text-white font-bold text-xs md:text-sm rounded-xl transition flex items-center gap-2 shadow-md shadow-brand-500/20"
        >
          <Plus className="w-4 h-4" />
          <span>{t.submitNewComplaint}</span>
        </button>
      </div>

      {/* Tickets List */}
      <div className="space-y-4">
        {complaints.map((c) => {
          const isResolved = c.status === 'Resolved';
          const isInProgress = c.status === 'Under Review';

          return (
            <div 
              key={c.firestoreId || c.id}
              className="p-6 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 shadow-sm space-y-3"
            >
              <div className="flex items-center justify-between gap-2 flex-wrap">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-1 bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-300 text-xs font-bold rounded-md">
                    {c.categoryEn}
                  </span>
                  <span className="text-xs font-semibold text-slate-400">Flat {c.holdingNo}</span>
                </div>

                <span className={`
                  px-3 py-1 text-xs font-extrabold rounded-full uppercase tracking-wider flex items-center gap-1.5
                  ${isResolved 
                    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/80 dark:text-emerald-300' 
                    : isInProgress 
                    ? 'bg-blue-100 text-blue-700 dark:bg-blue-950/80 dark:text-blue-300' 
                    : 'bg-amber-100 text-amber-800 dark:bg-amber-950/80 dark:text-amber-300'}
                `}>
                  {isResolved ? <CheckCircle className="w-3.5 h-3.5" /> : <Clock className="w-3.5 h-3.5" />}
                  <span>{c.status}</span>
                </span>
              </div>

              <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                {lang === 'bn' ? (c.titleBn || c.titleEn) : c.titleEn}
              </h3>

              <p className="text-sm text-slate-600 dark:text-slate-300">
                {c.descriptionEn}
              </p>

              {c.adminNoteEn && (
                <div className="mt-3 p-3.5 bg-slate-50 dark:bg-slate-900/60 rounded-xl border border-slate-200/80 dark:border-slate-700/80 flex items-start gap-2.5">
                  <MessageSquare className="w-4 h-4 text-brand-500 shrink-0 mt-0.5" />
                  <div className="text-xs">
                    <span className="font-bold text-slate-800 dark:text-slate-200">Management Response: </span>
                    <span className="text-slate-600 dark:text-slate-300">{c.adminNoteEn}</span>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* New Ticket Modal */}
      {showNewModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 w-full max-w-lg rounded-2xl p-6 shadow-2xl border border-slate-200 dark:border-slate-700">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-700">
              <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
                <Wrench className="w-5 h-5 text-brand-500" />
                <span>{t.submitNewComplaint}</span>
              </div>
              <button onClick={() => setShowNewModal(false)} className="p-1 text-slate-400 hover:text-slate-600" aria-label="Close">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="mt-4 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  {t.issueCategory}
                </label>
                <select 
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm font-medium focus:ring-2 focus:ring-brand-500"
                >
                  <option value="Plumbing">{t.plumbing}</option>
                  <option value="Electrical">{t.electrical}</option>
                  <option value="Security">{t.security}</option>
                  <option value="Elevator">{t.elevator}</option>
                  <option value="Cleanliness">{t.cleanliness}</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  {t.title}
                </label>
                <input 
                  type="text" 
                  required
                  value={titleInput}
                  onChange={(e) => setTitleInput(e.target.value)}
                  placeholder="e.g. Water leak in Flat 4-A master bath"
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  {t.description}
                </label>
                <textarea 
                  rows="3"
                  required
                  value={descInput}
                  onChange={(e) => setDescInput(e.target.value)}
                  placeholder="Provide details for technician..."
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm focus:ring-2 focus:ring-brand-500"
                />
              </div>

              {error && <p className="text-xs text-rose-500">{error}</p>}

              <button
                type="submit"
                disabled={submitting}
                className="w-full py-3 bg-brand-500 hover:bg-brand-600 text-white font-bold text-sm rounded-xl transition flex items-center justify-center gap-2 shadow-lg disabled:opacity-60"
              >
                {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                <span>{t.submit}</span>
              </button>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
