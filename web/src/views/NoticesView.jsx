import React, { useState } from 'react';
import { Bell, Pin, Search, Filter, Megaphone, Calendar } from 'lucide-react';
import { translations } from '../translations';

export default function NoticesView({ lang, notices }) {
  const t = translations[lang];
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredNotices = notices.filter(n => {
    const matchesCategory = selectedCategory === 'ALL' || n.category.toUpperCase() === selectedCategory;
    const title = lang === 'bn' ? n.titleBn : n.titleEn;
    const content = lang === 'bn' ? n.contentBn : n.contentEn;
    const matchesSearch = title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          content.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="space-y-6">
      
      {/* Header & Filter Search */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
        <div>
          <span className="px-3 py-1 bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-300 text-xs font-bold uppercase tracking-wider rounded-md">
            Executive Bulletin
          </span>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-white mt-2">
            {t.navNotices}
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {t.publishedBy}
          </p>
        </div>

        {/* Search & Category Filter Bar */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
            <input 
              type="text" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search notices by title or content..."
              className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="flex gap-1.5 overflow-x-auto pb-1 sm:pb-0">
            {['ALL', 'URGENT', 'MAINTENANCE', 'GENERAL'].map(cat => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`
                  px-3.5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider whitespace-nowrap transition
                  ${selectedCategory === cat 
                    ? 'bg-brand-500 text-white shadow-sm' 
                    : 'bg-slate-100 dark:bg-slate-700/60 text-slate-600 dark:text-slate-300 hover:bg-slate-200'}
                `}
              >
                {cat === 'ALL' ? t.allNotices : cat}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Notices List */}
      <div className="space-y-4">
        {filteredNotices.map((n) => (
          <div 
            key={n.id}
            className={`
              p-6 rounded-2xl bg-white dark:bg-slate-800 border transition shadow-sm
              ${n.isPinned ? 'border-indigo-300 dark:border-indigo-700/80 ring-2 ring-indigo-500/10' : 'border-slate-200 dark:border-slate-700'}
            `}
          >
            <div className="flex items-center justify-between gap-2 flex-wrap mb-2">
              <div className="flex items-center gap-2">
                {n.isPinned && (
                  <span className="px-2.5 py-1 bg-rose-100 text-rose-700 dark:bg-rose-950/80 dark:text-rose-300 text-[10px] font-extrabold rounded-md uppercase tracking-wider flex items-center gap-1">
                    <Pin className="w-3 h-3" />
                    {t.pinned}
                  </span>
                )}
                <span className="px-2.5 py-1 bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-300 text-[10px] font-bold rounded-md uppercase">
                  {n.category}
                </span>
              </div>
              <span className="text-xs font-semibold text-slate-400 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5" />
                {n.date}
              </span>
            </div>

            <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
              {lang === 'bn' ? n.titleBn : n.titleEn}
            </h3>

            <p className="text-sm text-slate-600 dark:text-slate-300 mt-2 leading-relaxed whitespace-pre-line">
              {lang === 'bn' ? n.contentBn : n.contentEn}
            </p>

            <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-700/60 flex items-center justify-between text-xs text-slate-400">
              <span>Ref: {n.id}</span>
              <span>{t.publishedBy}</span>
            </div>
          </div>
        ))}

        {filteredNotices.length === 0 && (
          <div className="text-center py-12 bg-white dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700">
            <Megaphone className="w-10 h-10 text-slate-300 dark:text-slate-600 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-500">No notices found matching your criteria.</p>
          </div>
        )}
      </div>

    </div>
  );
}
