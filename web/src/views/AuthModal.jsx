import React, { useState } from 'react';
import { Building, X, Loader2 } from 'lucide-react';
import { translations } from '../translations';
import { auth, functions } from '../firebase';
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
} from 'firebase/auth';
import { httpsCallable } from 'firebase/functions';
import { findResidentByUid } from '../services/residents';

// This modal used to be a one-click "pick Member or Admin" demo switcher with no
// credentials at all — that was the core vulnerability. It's now a real
// email/password login + registration form backed by Firebase Auth. Registration
// always creates a "Member" / "Pending" resident via the registerResident Cloud
// Function (see functions/index.js) — there is no role selector here, on purpose.

export default function AuthModal({ lang, setCurrentUser, setShowAuthModal }) {
  const t = translations[lang];

  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const getErrorMessage = (err) => {
    const code = err?.code || '';
    if (code === 'auth/configuration-not-found' || code === 'auth/operation-not-allowed') {
      return lang === 'bn'
        ? 'ফায়ারবেস কনসোলে ইমেইল/পাসওয়ার্ড লগইন চালু করা নেই। Firebase Console -> Authentication -> Sign-in method এ ইমেইল/পাসওয়ার্ড Enable করুন।'
        : 'Email/Password authentication is disabled in Firebase Console. Enable it in Firebase Console -> Authentication -> Sign-in method -> Email/Password.';
    }
    return err?.message || 'Operation failed.';
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const cred = await signInWithEmailAndPassword(auth, email.trim(), password);
      const resident = await findResidentByUid(cred.user.uid);
      if (!resident) {
        setError(
          lang === 'bn'
            ? 'এই অ্যাকাউন্টের জন্য কোনো প্রোফাইল পাওয়া যায়নি। প্রথমে নিবন্ধন সম্পন্ন করুন।'
            : 'No resident profile found for this account. Please complete registration.'
        );
        setBusy(false);
        return;
      }
      setCurrentUser(resident);
      setShowAuthModal(false);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    setInfo('');
    try {
      await createUserWithEmailAndPassword(auth, email.trim(), password);
      // Role/membershipStatus are hardcoded server-side in registerResident
      // ("Member" / "Pending") — nothing client-chosen is trusted here.
      const registerResident = httpsCallable(functions, 'registerResident');
      await registerResident({ nameEn: name || 'Resident', phone });
      setInfo(
        lang === 'bn'
          ? 'নিবন্ধন সম্পন্ন হয়েছে। আপনার অ্যাকাউন্ট কমিটির অনুমোদনের অপেক্ষায় আছে।'
          : 'Registration complete. Your account is awaiting committee approval.'
      );
      setMode('login');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-slate-800 w-full max-w-md rounded-2xl p-6 shadow-2xl border border-slate-200 dark:border-slate-700 space-y-5">

        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-700">
          <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
            <Building className="w-5 h-5 text-brand-500" />
            <span>{t.login}</span>
          </div>
          <button onClick={() => setShowAuthModal(false)} className="p-1 text-slate-400 hover:text-slate-600">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex rounded-xl bg-slate-100 dark:bg-slate-900 p-1 text-sm font-semibold">
          <button
            type="button"
            onClick={() => { setMode('login'); setError(''); setInfo(''); }}
            className={`flex-1 py-2 rounded-lg transition ${mode === 'login' ? 'bg-white dark:bg-slate-700 shadow text-brand-500' : 'text-slate-500'}`}
          >
            {lang === 'bn' ? 'সাইন ইন' : 'Sign In'}
          </button>
          <button
            type="button"
            onClick={() => { setMode('register'); setError(''); setInfo(''); }}
            className={`flex-1 py-2 rounded-lg transition ${mode === 'register' ? 'bg-white dark:bg-slate-700 shadow text-brand-500' : 'text-slate-500'}`}
          >
            {lang === 'bn' ? 'নিবন্ধন' : 'Register'}
          </button>
        </div>

        {mode === 'register' && (
          <div className="rounded-lg bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-3 text-xs text-slate-500 dark:text-slate-400">
            {lang === 'bn'
              ? 'নতুন অ্যাকাউন্ট "নতুন সদস্য" হিসেবে তৈরি হবে এবং কমিটির অনুমোদনের পর "সাধারণ সদস্য"-তে উন্নীত হবে।'
              : 'New accounts start as a New Member and are upgraded to General Member once the committee approves.'}
          </div>
        )}

        <form onSubmit={mode === 'login' ? handleLogin : handleRegister} className="space-y-3">
          {mode === 'register' && (
            <>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={lang === 'bn' ? 'পূর্ণ নাম' : 'Full Name'}
                className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 px-3 py-2 text-sm"
              />
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder={lang === 'bn' ? 'ফোন নম্বর' : 'Phone Number'}
                className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 px-3 py-2 text-sm"
              />
            </>
          )}
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={lang === 'bn' ? 'ইমেইল' : 'Email'}
            className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 px-3 py-2 text-sm"
          />
          <input
            type="password"
            required
            minLength={6}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder={lang === 'bn' ? 'পাসওয়ার্ড' : 'Password'}
            className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 px-3 py-2 text-sm"
          />

          {error && <p className="text-xs text-rose-500">{error}</p>}
          {info && <p className="text-xs text-emerald-600">{info}</p>}

          <button
            type="submit"
            disabled={busy}
            className="w-full flex items-center justify-center gap-2 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-bold py-2.5 text-sm transition disabled:opacity-60"
          >
            {busy && <Loader2 className="w-4 h-4 animate-spin" />}
            {mode === 'login'
              ? (lang === 'bn' ? 'সাইন ইন করুন' : 'Sign In')
              : (lang === 'bn' ? 'নিবন্ধন করুন' : 'Register')}
          </button>
        </form>

        <div className="pt-2 text-center text-[11px] text-slate-400">
          Kunjachaya Resident Club Management System
        </div>

      </div>
    </div>
  );
}
