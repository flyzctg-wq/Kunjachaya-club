import React, { useState, useEffect } from 'react';
import { Users, Phone, Search, PhoneCall, MessageCircle, Siren } from 'lucide-react';
import { translations } from '../translations';
import { mockEmergencyContacts } from '../mockData';
import { db } from '../firebase';
import { collection, query, where, onSnapshot } from 'firebase/firestore';

// This used to render a hardcoded list of 10 fictional residents from
// mockData.js — every visitor saw the same fake names regardless of who
// actually lives in the building. It now reads the real `users` collection
// (Active residents only) live from Firestore. Emergency hotlines stay as
// static config data for now — those genuinely don't change per-resident and
// aren't sensitive, so they're a lower priority to move off mock data.

export default function DirectoryView({ lang }) {
  const t = translations[lang];
  const [searchQuery, setSearchQuery] = useState('');
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, 'users_public'), where('membershipStatus', '==', 'Active'));
    const unsub = onSnapshot(
      q,
      (snap) => {
        setMembers(
          snap.docs.map((d) => {
            const data = d.data();
            return {
              id: d.id,
              flatNo: data.holding || '',
              name: data.nameEn || '',
              nameBn: data.nameBn || data.nameEn || '',
              phone: data.primaryContact || data.phone || '',
              profession: data.professionEn || '',
              status: data.membershipStatus || 'Active',
            };
          })
        );
        setLoading(false);
      },
      (err) => {
        console.error('directory listener error:', err);
        setLoading(false);
      }
    );
    return unsub;
  }, []);

  const filteredMembers = members.filter(m =>
    m.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    m.flatNo.toLowerCase().includes(searchQuery.toLowerCase()) ||
    m.profession.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      
      {/* Emergency Hotlines */}
      <div className="bg-gradient-to-r from-rose-900 to-slate-900 text-white p-6 rounded-2xl shadow-lg border border-rose-800">
        <div className="flex items-center gap-2 mb-4">
          <Siren className="w-6 h-6 text-rose-400 animate-pulse" />
          <h3 className="font-bold text-lg">{t.emergencyContacts}</h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {mockEmergencyContacts.map((item, idx) => (
            <div key={idx} className="bg-white/10 backdrop-blur border border-white/10 p-3.5 rounded-xl flex items-center justify-between">
              <div>
                <p className="text-xs font-bold text-rose-200">{item.role}</p>
                <p className="text-sm font-semibold text-white mt-0.5">{item.name}</p>
                <p className="text-xs font-mono text-slate-300 mt-1">{item.phone}</p>
              </div>
              <a 
                href={`tel:${item.phone.replace(/[^0-9+]/g, '')}`} 
                className="p-2.5 bg-rose-500 hover:bg-rose-600 text-white rounded-lg transition"
                title="Call Emergency Hotline"
                aria-label={`Call ${item.name}`}
              >
                <PhoneCall className="w-4 h-4" />
              </a>
            </div>
          ))}
        </div>
      </div>

      {/* Member Search Bar */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
        <div>
          <span className="px-3 py-1 bg-brand-50 text-brand-600 dark:bg-brand-950/60 dark:text-brand-300 text-xs font-bold uppercase tracking-wider rounded-md">
            Resident Roster
          </span>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-white mt-2">
            {t.navDirectory}
          </h2>
        </div>

        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input 
            type="text" 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={t.searchMember}
            className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
      </div>

      {loading && (
        <div className="text-center text-sm text-slate-400 py-8">Loading residents…</div>
      )}

      {!loading && filteredMembers.length === 0 && (
        <div className="text-center text-sm text-slate-400 py-8 flex flex-col items-center gap-2">
          <Users className="w-8 h-8 text-slate-300" />
          <span>No active residents match your search yet.</span>
        </div>
      )}

      {/* Members Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredMembers.map((m) => (
          <div key={m.id} className="bg-white dark:bg-slate-800 p-5 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between">
                <span className="px-2.5 py-1 bg-brand-500 text-white font-extrabold text-xs rounded-lg">
                  Flat {m.flatNo || '—'}
                </span>
                <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400 uppercase tracking-wider">
                  ● {m.status}
                </span>
              </div>

              <h4 className="font-bold text-slate-900 dark:text-slate-100 text-base mt-3">
                {lang === 'bn' ? m.nameBn : m.name}
              </h4>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                {m.profession}
              </p>
            </div>

            <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-700 flex items-center justify-between gap-2">
              <span className="text-xs font-mono text-slate-600 dark:text-slate-300">{m.phone}</span>
              <div className="flex gap-1.5">
                <a 
                  href={`tel:${m.phone}`}
                  className="p-2 bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-200 hover:bg-brand-500 hover:text-white rounded-lg transition"
                  title={t.call}
                  aria-label={`${t.call} ${m.name}`}
                >
                  <Phone className="w-4 h-4" />
                </a>
                <a 
                  href={`https://wa.me/${m.phone.replace(/[^0-9]/g, '')}`}
                  target="_blank"
                  rel="noreferrer"
                  className="p-2 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500 hover:text-white rounded-lg transition"
                  title={t.whatsapp}
                  aria-label={`${t.whatsapp} ${m.name}`}
                >
                  <MessageCircle className="w-4 h-4" />
                </a>
              </div>
            </div>
          </div>
        ))}
      </div>

    </div>
  );
}
