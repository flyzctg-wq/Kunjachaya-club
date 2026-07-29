import React, { useState } from 'react';
import { Calendar, MapPin, Clock, Users, CheckCircle } from 'lucide-react';
import { translations } from '../translations';

export default function EventsView({ lang, events, setEvents }) {
  const t = translations[lang];

  const handleRsvp = (id) => {
    const updated = events.map(evt => {
      if (evt.id === id) {
        const isReg = !evt.isRegistered;
        return {
          ...evt,
          isRegistered: isReg,
          attendingCount: isReg ? evt.attendingCount + 1 : evt.attendingCount - 1
        };
      }
      return evt;
    });
    setEvents(updated);
  };

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm">
        <span className="px-3 py-1 bg-emerald-50 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-300 text-xs font-bold uppercase tracking-wider rounded-md">
          Community Gathering
        </span>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white mt-2">
          {t.navEvents}
        </h2>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Cultural nights, sports competitions, and general resident assemblies
        </p>
      </div>

      {/* Events Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {events.map((evt) => (
          <div 
            key={evt.id}
            className="p-6 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 shadow-sm flex flex-col justify-between space-y-4"
          >
            <div>
              <div className="flex items-center justify-between gap-2">
                <span className="px-2.5 py-1 bg-emerald-100 text-emerald-700 dark:bg-emerald-950/80 dark:text-emerald-300 text-xs font-bold rounded-md">
                  {evt.category}
                </span>
                <span className="text-xs font-semibold text-slate-400 flex items-center gap-1">
                  <Users className="w-3.5 h-3.5 text-indigo-500" />
                  {evt.attendingCount} Attending
                </span>
              </div>

              <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100 mt-3">
                {lang === 'bn' ? evt.titleBn : evt.titleEn}
              </h3>

              <div className="mt-3 space-y-2 text-xs text-slate-600 dark:text-slate-300">
                <div className="flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-emerald-500 shrink-0" />
                  <span>{evt.date}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Clock className="w-4 h-4 text-amber-500 shrink-0" />
                  <span>{evt.time}</span>
                </div>
                <div className="flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-rose-500 shrink-0" />
                  <span>{lang === 'bn' ? evt.venueBn : evt.venueEn}</span>
                </div>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-100 dark:border-slate-700">
              <button
                onClick={() => handleRsvp(evt.id)}
                className={`
                  w-full py-2.5 rounded-xl font-bold text-xs transition flex items-center justify-center gap-2
                  ${evt.isRegistered 
                    ? 'bg-emerald-500 text-white shadow-md' 
                    : 'bg-brand-500 hover:bg-brand-600 text-white shadow-md'}
                `}
              >
                {evt.isRegistered ? (
                  <>
                    <CheckCircle className="w-4 h-4" />
                    <span>RSVP Confirmed (Registered)</span>
                  </>
                ) : (
                  <>
                    <Calendar className="w-4 h-4" />
                    <span>{t.rsvp}</span>
                  </>
                )}
              </button>
            </div>
          </div>
        ))}
      </div>

    </div>
  );
}
