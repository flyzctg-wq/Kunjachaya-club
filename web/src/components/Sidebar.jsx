import React from 'react';
import { 
  LayoutDashboard, 
  CreditCard, 
  Bell, 
  Wrench, 
  Calendar, 
  Users, 
  ShieldAlert, 
  Code2, 
  X 
} from 'lucide-react';
import { translations } from '../translations';

export default function Sidebar({ 
  activeTab, 
  setActiveTab, 
  lang, 
  currentUser, 
  isSidebarOpen, 
  setIsSidebarOpen 
}) {
  const t = translations[lang];

  const menuItems = [
    { id: 'dashboard', label: t.navDashboard, icon: LayoutDashboard },
    { id: 'financials', label: t.navFinancials, icon: CreditCard, badge: 'Due' },
    { id: 'notices', label: t.navNotices, icon: Bell },
    { id: 'complaints', label: t.navComplaints, icon: Wrench },
    { id: 'events', label: t.navEvents, icon: Calendar },
    { id: 'directory', label: t.navDirectory, icon: Users },
  ];

  if (currentUser?.isAdmin) {
    menuItems.push({ id: 'admin', label: t.navAdmin, icon: ShieldAlert, highlight: true });
    // Dev Docs exposes the Firestore schema and sample backend code — admin-only,
    // not a general nav item every resident could previously see.
    menuItems.push({ id: 'devdocs', label: t.navDevDocs, icon: Code2 });
  }

  return (
    <>
      {/* Mobile Drawer Overlay */}
      {isSidebarOpen && (
        <div 
          onClick={() => setIsSidebarOpen(false)}
          className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-40 md:hidden"
        />
      )}

      <aside className={`
        fixed md:sticky top-[61px] left-0 h-[calc(100vh-61px)] w-64 bg-white dark:bg-slate-900 
        border-r border-slate-200 dark:border-slate-800 z-40 transition-transform duration-300
        ${isSidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
        flex flex-col justify-between p-4 overflow-y-auto
      `}>
        <div>
          <div className="flex items-center justify-between md:hidden pb-3 mb-3 border-b border-slate-200 dark:border-slate-800">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Navigation</span>
            <button onClick={() => setIsSidebarOpen(false)} className="p-1 text-slate-400 hover:text-slate-600">
              <X className="w-5 h-5" />
            </button>
          </div>

          <nav className="space-y-1.5">
            {menuItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;

              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setActiveTab(item.id);
                    setIsSidebarOpen(false);
                  }}
                  className={`
                    w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all
                    ${isActive 
                      ? 'bg-brand-500 text-white shadow-md shadow-brand-500/20' 
                      : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'}
                    ${item.highlight ? 'border border-amber-500/40 text-amber-600 dark:text-amber-400' : ''}
                  `}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={`w-5 h-5 ${isActive ? 'text-white' : item.highlight ? 'text-amber-500' : 'text-slate-500 dark:text-slate-400'}`} />
                    <span>{item.label}</span>
                  </div>

                  {item.badge && !isActive && (
                    <span className="px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-full bg-rose-100 text-rose-700 dark:bg-rose-950/60 dark:text-rose-400">
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Footer Info Box */}
        <div className="pt-4 border-t border-slate-200 dark:border-slate-800 text-center">
          <p className="text-xs font-semibold text-slate-700 dark:text-slate-300">
            Kunjachaya Resident Club
          </p>
          <p className="text-[11px] text-slate-400 mt-0.5">
            Web & Android Unified Portal
          </p>
        </div>
      </aside>
    </>
  );
}
