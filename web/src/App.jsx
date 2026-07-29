import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import DashboardView from './views/DashboardView';
import FinancialsView from './views/FinancialsView';
import NoticesView from './views/NoticesView';
import ComplaintsView from './views/ComplaintsView';
import EventsView from './views/EventsView';
import DirectoryView from './views/DirectoryView';
import AdminPortalView from './views/AdminPortalView';
import DevDocsView from './views/DevDocsView';
import AuthModal from './views/AuthModal';
import SplashScreen from './views/SplashScreen';

import { auth, db } from './firebase';
import { onAuthStateChanged, signOut } from 'firebase/auth';
import { collection, query, where, onSnapshot, doc } from 'firebase/firestore';
import { subscribeToResident } from './services/residents';

// This used to start already "logged in" as a hardcoded mock user — anyone opening
// the site got instant resident access with a one-click switch to Admin. It now
// waits for a real Firebase Auth session and loads the resident's actual profile
// (and their own financials/complaints) from Firestore before rendering anything
// role-gated.

export default function App() {
  const [lang, setLang] = useState('en');
  const [showSplash, setShowSplash] = useState(true);
  const [authUser, setAuthUser] = useState(undefined); // undefined = loading, null = signed out
  const [currentUser, setCurrentUser] = useState(null); // normalized resident profile
  const [activeTab, setActiveTab] = useState('dashboard');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [workspaceArchived, setWorkspaceArchived] = useState(false);

  // Live Firestore-backed state — no more mock arrays.
  const [financials, setFinancials] = useState([]);
  const [notices, setNotices] = useState([]);
  const [complaints, setComplaints] = useState([]);
  const [events, setEvents] = useState([]);

  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkMode]);

  // Public read (firestore.rules: clubSettings allows read: if true) so even a
  // signed-out visitor sees the closed notice instead of an empty login screen.
  useEffect(() => {
    const unsub = onSnapshot(doc(db, 'clubSettings', 'workspace'), (snap) => {
      setWorkspaceArchived(!!snap.data()?.archived);
    }, (err) => console.error('workspace status listener error:', err));
    return unsub;
  }, []);

  // Real auth session, restored automatically by Firebase on reload.
  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (u) => setAuthUser(u));
    return unsub;
  }, []);

  // Live resident profile, kept in sync with Firestore (so an admin's
  // setUserRole/approveMembership takes effect without a re-login).
  useEffect(() => {
    if (!authUser) {
      setCurrentUser(null);
      return;
    }
    const unsub = subscribeToResident(authUser.uid, setCurrentUser);
    return unsub;
  }, [authUser]);

  // Financials: residents see only their own; admins see all (Firestore rules
  // enforce this server-side too — this query just matches what's actually allowed).
  useEffect(() => {
    if (!currentUser) { setFinancials([]); return; }
    const col = collection(db, 'financials');
    const q = currentUser.isAdmin ? col : query(col, where('userId', '==', currentUser.id));
    const unsub = onSnapshot(q, (snap) => {
      setFinancials(snap.docs.map((d) => ({ id: d.id, firestoreId: d.id, ...d.data() })));
    }, (err) => console.error('financials listener error:', err));
    return unsub;
  }, [currentUser]);

  // Complaints: residents see only their own; admins see all — matches
  // firestore.rules (complaints read: isAdmin() || owner).
  useEffect(() => {
    if (!currentUser) { setComplaints([]); return; }
    const col = collection(db, 'complaints');
    const q = currentUser.isAdmin ? col : query(col, where('userId', '==', currentUser.id));
    const unsub = onSnapshot(q, (snap) => {
      setComplaints(snap.docs.map((d) => ({ id: d.id, firestoreId: d.id, ...d.data() })));
    }, (err) => console.error('complaints listener error:', err));
    return unsub;
  }, [currentUser]);

  // Notices/announcements: public read for any signed-in resident.
  useEffect(() => {
    if (!currentUser) { setNotices([]); return; }
    const unsub = onSnapshot(collection(db, 'announcements'), (snap) => {
      setNotices(snap.docs.map((d) => ({ id: d.id, firestoreId: d.id, ...d.data() })));
    }, (err) => console.error('notices listener error:', err));
    return unsub;
  }, [currentUser]);

  // Events: same visibility as notices.
  useEffect(() => {
    if (!currentUser) { setEvents([]); return; }
    const unsub = onSnapshot(collection(db, 'Events'), (snap) => {
      setEvents(snap.docs.map((d) => ({ id: d.id, firestoreId: d.id, ...d.data() })));
    }, (err) => console.error('events listener error:', err));
    return unsub;
  }, [currentUser]);

  const handleLogout = async () => {
    await signOut(auth);
    setActiveTab('dashboard');
  };

  // Cold-start / first load splash screen.
  if (showSplash) {
    return <SplashScreen onFinished={() => setShowSplash(false)} />;
  }

  // Still resolving the Firebase session — avoid flashing any content.
  if (authUser === undefined) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900 text-slate-500">
        Loading…
      </div>
    );
  }

  // Workspace archived (Super Admin action) and this session isn't an admin —
  // show a read-only closed notice instead of the app. An admin/Super Admin can
  // still sign in from here to reopen it via AdminPortalView.
  if (workspaceArchived && !currentUser?.isAdmin) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white p-6">
        <div className="max-w-md text-center space-y-4">
          <h2 className="text-xl font-bold">
            {lang === 'bn' ? 'ক্লাব ওয়ার্কস্পেস আর্কাইভ করা হয়েছে' : 'Club Workspace Archived'}
          </h2>
          <p className="text-sm text-slate-400">
            {lang === 'bn'
              ? 'এই মুহূর্তে অ্যাপটি শুধুমাত্র পাঠযোগ্য এবং নতুন কার্যক্রম বন্ধ আছে। বিস্তারিত জানতে কমিটির সাথে যোগাযোগ করুন।'
              : "This club's workspace is currently archived by the Super Admin and isn't accepting activity right now. Contact the committee for details."}
          </p>
          {!currentUser && (
            <button
              onClick={() => setShowAuthModal(true)}
              className="rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold px-6 py-2.5 text-sm"
            >
              {lang === 'bn' ? 'অ্যাডমিন সাইন ইন' : 'Admin Sign In'}
            </button>
          )}
        </div>
        {showAuthModal && (
          <AuthModal lang={lang} setCurrentUser={setCurrentUser} setShowAuthModal={setShowAuthModal} />
        )}
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 text-slate-800 dark:text-slate-100 flex flex-col transition-colors">

      {/* Top Fixed Header */}
      <Header
        lang={lang}
        setLang={setLang}
        currentUser={currentUser}
        setShowAuthModal={setShowAuthModal}
        onLogout={handleLogout}
        isDarkMode={isDarkMode}
        setIsDarkMode={setIsDarkMode}
        setIsSidebarOpen={setIsSidebarOpen}
      />

      {/* Main Container */}
      <div className="flex-1 max-w-7xl w-full mx-auto flex">

        {/* Navigation Sidebar */}
        {currentUser && (
          <Sidebar
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            lang={lang}
            currentUser={currentUser}
            isSidebarOpen={isSidebarOpen}
            setIsSidebarOpen={setIsSidebarOpen}
          />
        )}

        {/* View Content Area */}
        <main className="flex-1 p-4 md:p-6 lg:p-8 overflow-x-hidden">
          {!currentUser && (
            <div className="max-w-md mx-auto mt-16 text-center space-y-4">
              <h2 className="text-xl font-bold">
                {lang === 'bn' ? 'কুঞ্জছায়া রেসিডেন্ট ক্লাব' : 'Kunjachaya Resident Club'}
              </h2>
              <p className="text-sm text-slate-500">
                {lang === 'bn'
                  ? 'চালিয়ে যেতে সাইন ইন করুন।'
                  : 'Please sign in to continue.'}
              </p>
              <button
                onClick={() => setShowAuthModal(true)}
                className="rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-bold px-6 py-2.5 text-sm"
              >
                {lang === 'bn' ? 'সাইন ইন / নিবন্ধন' : 'Sign In / Register'}
              </button>
            </div>
          )}

          {currentUser && activeTab === 'dashboard' && (
            <DashboardView
              lang={lang}
              currentUser={currentUser}
              setActiveTab={setActiveTab}
              financials={financials}
              notices={notices}
              events={events}
              complaints={complaints}
            />
          )}

          {currentUser && activeTab === 'financials' && (
            <FinancialsView
              lang={lang}
              currentUser={currentUser}
              financials={financials}
              setFinancials={setFinancials}
            />
          )}

          {currentUser && activeTab === 'notices' && (
            <NoticesView
              lang={lang}
              notices={notices}
            />
          )}

          {currentUser && activeTab === 'complaints' && (
            <ComplaintsView
              lang={lang}
              currentUser={currentUser}
              complaints={complaints}
              setComplaints={setComplaints}
            />
          )}

          {currentUser && activeTab === 'events' && (
            <EventsView
              lang={lang}
              events={events}
              setEvents={setEvents}
            />
          )}

          {currentUser && activeTab === 'directory' && (
            <DirectoryView
              lang={lang}
            />
          )}

          {currentUser && activeTab === 'admin' && currentUser?.isAdmin && (
            <AdminPortalView
              lang={lang}
              currentUser={currentUser}
              notices={notices}
              setNotices={setNotices}
              financials={financials}
              setFinancials={setFinancials}
              complaints={complaints}
              setComplaints={setComplaints}
            />
          )}

          {currentUser && activeTab === 'devdocs' && currentUser?.isAdmin && (
            <DevDocsView />
          )}
        </main>

      </div>

      {/* Auth Modal (real Firebase email/password login + registration) */}
      {showAuthModal && (
        <AuthModal
          lang={lang}
          setCurrentUser={setCurrentUser}
          setShowAuthModal={setShowAuthModal}
        />
      )}

    </div>
  );
}
