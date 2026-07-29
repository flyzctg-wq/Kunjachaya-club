import React from 'react';
import { Globe, User, LogOut, Shield, Menu, Building, Sun, Moon } from 'lucide-react';
import { translations } from '../translations';
import { displayName } from '../services/roles';

export default function Header({ 
  lang, 
  setLang, 
  currentUser, 
  onLogout, 
  setShowAuthModal, 
  isDarkMode, 
  setIsDarkMode,
  setIsSidebarOpen 
}) {
  const t = translations[lang];

  return (
    <header className="sticky top-0 z-30 bg-white/95 dark:bg-slate-900/95 backdrop-blur border-b border-slate-200 dark:border-slate-800 px-4 py-3 transition-colors">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        
        {/* Left Brand Area */}
        <div className="flex items-center gap-3">
          <button 
            onClick={() => setIsSidebarOpen(prev => !prev)}
            className="md:hidden p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg"
          >
            <Menu className="w-6 h-6" />
          </button>

          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-500 to-indigo-600 flex items-center justify-center text-white shadow-md shadow-brand-500/20">
              <Building className="w-5 h-5" />
            </div>
            <div>
              <h1 className="font-bold text-slate-900 dark:text-white leading-tight text-base md:text-lg">
                {t.appName}
              </h1>
              <p className="text-xs text-slate-500 dark:text-slate-400 hidden sm:block">
                {t.subTitle}
              </p>
            </div>
          </div>
        </div>

        {/* Right Tools Area */}
        <div className="flex items-center gap-2 md:gap-3">
          
          {/* Language Switcher Button */}
          <button
            onClick={() => setLang(lang === 'en' ? 'bn' : 'en')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs md:text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 transition"
          >
            <Globe className="w-4 h-4 text-brand-500" />
            <span>{t.switchLang}</span>
          </button>

          {/* Dark / Light Theme Toggle */}
          <button
            onClick={() => setIsDarkMode(!isDarkMode)}
            className="p-2 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            title="Toggle Light/Dark Theme"
          >
            {isDarkMode ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-indigo-600" />}
          </button>

          {/* User Profile / Auth State */}
          {currentUser ? (
            <div className="flex items-center gap-2 pl-2 border-l border-slate-200 dark:border-slate-800">
              <div className="hidden sm:block text-right">
                <div className="text-xs md:text-sm font-semibold text-slate-800 dark:text-slate-100 flex items-center gap-1 justify-end">
                  {currentUser.isAdmin && <Shield className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />}
                  {currentUser.name}
                </div>
                <div className="text-[11px] text-slate-500 dark:text-slate-400">
                  {t.flat} {currentUser.flatNo} ({displayName(currentUser.role, lang)})
                </div>
              </div>

              <button
                onClick={onLogout}
                className="p-2 rounded-lg text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 transition"
                title={t.logout}
                aria-label={t.logout}
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowAuthModal(true)}
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-brand-500 hover:bg-brand-600 text-white rounded-lg text-xs md:text-sm font-medium transition shadow-sm"
            >
              <User className="w-4 h-4" />
              <span>{t.login}</span>
            </button>
          )}

        </div>

      </div>
    </header>
  );
}
