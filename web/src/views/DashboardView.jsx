import React from 'react';
import { 
  CreditCard, 
  Wrench, 
  Bell, 
  Calendar, 
  ShieldCheck, 
  ArrowRight, 
  CheckCircle2, 
  AlertTriangle,
  Megaphone,
  UserCheck
} from 'lucide-react';
import { translations } from '../translations';

export default function DashboardView({ 
  lang, 
  currentUser, 
  setActiveTab, 
  financials, 
  notices, 
  events, 
  complaints 
}) {
  const t = translations[lang];

  const pendingDues = financials.filter(f => f.status === 'UNPAID' || f.status === 'OVERDUE');
  const totalPendingAmount = pendingDues.reduce((acc, curr) => acc + curr.amount, 0);

  return (
    <div className="space-y-6">
      
      {/* Hero Welcome Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-brand-500 via-indigo-900 to-slate-900 p-6 md:p-8 text-white shadow-xl">
        <div className="relative z-10 max-w-2xl">
          <span className="px-3 py-1 bg-white/10 backdrop-blur border border-white/20 rounded-full text-xs font-semibold uppercase tracking-wider text-amber-300">
            {t.appName} Portal
          </span>
          <h2 className="text-2xl md:text-3xl font-extrabold mt-3 leading-tight">
            {t.welcomeMsg}, {currentUser ? currentUser.name : 'Resident'}!
          </h2>
          <p className="text-slate-300 text-sm md:text-base mt-2">
            {currentUser 
              ? `${t.flat}: ${currentUser.flatNo} | ${currentUser.buildingBlock}` 
              : 'Logged in as Guest Resident. Select a profile to manage your dues and services.'}
          </p>

          <div className="flex flex-wrap gap-3 mt-6">
            <button
              onClick={() => setActiveTab('financials')}
              className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-slate-950 font-bold rounded-xl text-xs md:text-sm transition flex items-center gap-2 shadow-lg shadow-amber-500/20"
            >
              <CreditCard className="w-4 h-4" />
              <span>{t.payDues}</span>
            </button>
            <button
              onClick={() => setActiveTab('complaints')}
              className="px-4 py-2 bg-white/10 hover:bg-white/20 border border-white/20 text-white font-medium rounded-xl text-xs md:text-sm backdrop-blur transition flex items-center gap-2"
            >
              <Wrench className="w-4 h-4" />
              <span>{t.newComplaint}</span>
            </button>
          </div>
        </div>

        {/* Decorative Graphic Element */}
        <div className="absolute -right-8 -bottom-8 opacity-10 pointer-events-none">
          <ShieldCheck className="w-80 h-80 text-white" />
        </div>
      </div>

      {/* Dues Status Alert Banner */}
      {totalPendingAmount > 0 ? (
        <div className="bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-amber-500/20 text-amber-600 dark:text-amber-400 rounded-lg shrink-0">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div>
              <h4 className="font-bold text-amber-900 dark:text-amber-200 text-sm md:text-base">
                {t.totalDues}: BDT {totalPendingAmount.toLocaleString()}
              </h4>
              <p className="text-xs text-amber-700 dark:text-amber-400 mt-0.5">
                {pendingDues.length} pending bill(s) awaiting payment for current period.
              </p>
            </div>
          </div>
          <button
            onClick={() => setActiveTab('financials')}
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-slate-900 font-bold rounded-lg text-xs md:text-sm transition shrink-0 self-start sm:self-auto"
          >
            {t.payNow}
          </button>
        </div>
      ) : (
        <div className="bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60 rounded-xl p-4 flex items-center gap-3 text-emerald-800 dark:text-emerald-300">
          <CheckCircle2 className="w-6 h-6 text-emerald-500 shrink-0" />
          <div className="text-sm">
            <span className="font-bold">All Dues Clear!</span> Thank you for keeping your account up to date.
          </div>
        </div>
      )}

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        <div 
          onClick={() => setActiveTab('financials')}
          className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 hover:shadow-md transition cursor-pointer flex items-center justify-between"
        >
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{t.totalDues}</span>
            <div className="text-xl font-bold text-slate-900 dark:text-white mt-1">
              BDT {totalPendingAmount.toLocaleString()}
            </div>
          </div>
          <div className="w-10 h-10 rounded-lg bg-rose-50 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400 flex items-center justify-center">
            <CreditCard className="w-5 h-5" />
          </div>
        </div>

        <div 
          onClick={() => setActiveTab('notices')}
          className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 hover:shadow-md transition cursor-pointer flex items-center justify-between"
        >
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Active Notices</span>
            <div className="text-xl font-bold text-slate-900 dark:text-white mt-1">
              {notices.length} Published
            </div>
          </div>
          <div className="w-10 h-10 rounded-lg bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
            <Megaphone className="w-5 h-5" />
          </div>
        </div>

        <div 
          onClick={() => setActiveTab('complaints')}
          className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 hover:shadow-md transition cursor-pointer flex items-center justify-between"
        >
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Service Tickets</span>
            <div className="text-xl font-bold text-slate-900 dark:text-white mt-1">
              {complaints.length} Tickets
            </div>
          </div>
          <div className="w-10 h-10 rounded-lg bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400 flex items-center justify-center">
            <Wrench className="w-5 h-5" />
          </div>
        </div>

        <div 
          onClick={() => setActiveTab('events')}
          className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 hover:shadow-md transition cursor-pointer flex items-center justify-between"
        >
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Upcoming Events</span>
            <div className="text-xl font-bold text-slate-900 dark:text-white mt-1">
              {events.length} Scheduled
            </div>
          </div>
          <div className="w-10 h-10 rounded-lg bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
            <Calendar className="w-5 h-5" />
          </div>
        </div>

      </div>

      {/* Two Column Layout: Latest Announcements & Upcoming Events */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Latest Announcements */}
        <div className="bg-white dark:bg-slate-800 rounded-2xl p-5 border border-slate-200 dark:border-slate-700">
          <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-700">
            <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
              <Bell className="w-5 h-5 text-brand-500" />
              <span>{t.navNotices}</span>
            </div>
            <button 
              onClick={() => setActiveTab('notices')} 
              className="text-xs font-semibold text-brand-500 hover:underline flex items-center gap-1"
            >
              <span>View All</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="divide-y divide-slate-100 dark:divide-slate-700/60 mt-3">
            {notices.map((n) => (
              <div key={n.id} className="py-3.5 first:pt-0 last:pb-0">
                <div className="flex items-center gap-2 mb-1">
                  {n.isPinned && (
                    <span className="px-2 py-0.5 bg-rose-100 text-rose-700 dark:bg-rose-950/80 dark:text-rose-300 text-[10px] font-extrabold rounded-md uppercase">
                      {t.pinned}
                    </span>
                  )}
                  <span className="text-xs font-semibold text-slate-400">{n.date}</span>
                </div>
                <h5 className="font-semibold text-slate-800 dark:text-slate-100 text-sm">
                  {lang === 'bn' ? n.titleBn : n.titleEn}
                </h5>
                <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2 mt-1">
                  {lang === 'bn' ? n.contentBn : n.contentEn}
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* Upcoming Events */}
        <div className="bg-white dark:bg-slate-800 rounded-2xl p-5 border border-slate-200 dark:border-slate-700">
          <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-700">
            <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
              <Calendar className="w-5 h-5 text-emerald-500" />
              <span>{t.upcomingEvents}</span>
            </div>
            <button 
              onClick={() => setActiveTab('events')} 
              className="text-xs font-semibold text-brand-500 hover:underline flex items-center gap-1"
            >
              <span>View All</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="space-y-3.5 mt-3">
            {events.map((evt) => (
              <div key={evt.id} className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-900/60 border border-slate-200/80 dark:border-slate-700/60">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span className="text-[11px] font-bold text-emerald-600 dark:text-emerald-400 uppercase tracking-wider">
                      {evt.date} • {evt.time}
                    </span>
                    <h5 className="font-bold text-slate-900 dark:text-slate-100 text-sm mt-0.5">
                      {lang === 'bn' ? evt.titleBn : evt.titleEn}
                    </h5>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                      📍 {lang === 'bn' ? evt.venueBn : evt.venueEn}
                    </p>
                  </div>
                  <span className="px-2.5 py-1 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 text-xs font-semibold rounded-lg shrink-0">
                    {evt.attendingCount} Attending
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>

    </div>
  );
}
