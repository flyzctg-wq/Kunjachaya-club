import React, { useState } from 'react';
import { Code2, Server, Database, Smartphone, Globe, Copy, Check } from 'lucide-react';

export default function DevDocsView() {
  const [copiedSection, setCopiedSection] = useState('');

  const copyToClipboard = (text, section) => {
    navigator.clipboard.writeText(text);
    setCopiedSection(section);
    setTimeout(() => setCopiedSection(''), 2500);
  };

  const firestoreSchema = `{
  "users": {
    "{uid}": {
      "flatNo": "4-A",
      "name": "Tanvir Hasan",
      "phone": "+8801711234567",
      "email": "tanvir@kunjachaya.org",
      "role": "MEMBER", // "MEMBER" | "ADMIN"
      "buildingBlock": "Alpha-A",
      "createdAt": "2026-01-15T08:00:00Z"
    }
  },
  "financials": {
    "{recordId}": {
      "titleEn": "Monthly Maintenance - July 2026",
      "titleBn": "মাসিক রক্ষণাবেক্ষণ ফি - জুলাই ২০২৬",
      "flatNo": "4-A",
      "amount": 3500,
      "status": "UNPAID", // "UNPAID" | "PAID"
      "dueDate": "2026-07-31",
      "paymentDate": null,
      "txRef": null
    }
  },
  "announcements": {
    "{noticeId}": {
      "titleEn": "Tree Plantation Drive 2026",
      "titleBn": "বৃক্ষরোপণ অভিযান ২০২৬",
      "category": "Urgent",
      "isPinned": true,
      "date": "2026-07-25"
    }
  }
}`;

  const cloudFunctionCode = `/**
 * Kunjachaya Resident Club - Firebase Payment Cloud Function
 */
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.processDuesPayment = functions.https.onRequest(async (req, res) => {
  const { recordId, flatNo, amount, gateway, txRef } = req.body;
  
  if (!recordId || !flatNo || !amount) {
    return res.status(400).json({ error: 'Missing required payment parameters' });
  }

  try {
    await admin.firestore().collection('financials').doc(recordId).update({
      status: 'PAID',
      paymentDate: admin.firestore.FieldValue.serverTimestamp(),
      paymentMethod: gateway,
      txRef: txRef
    });

    return res.status(200).json({ success: true, message: 'Payment recorded' });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});`;

  const iosCapacitorGuide = `# iOS / Cross-Platform Deployment Guide

## 1. Vercel Deployment (Automated via GitHub)
1. Push this repository to GitHub.
2. In Vercel, click "New Project" and import your repo.
3. Vercel automatically detects \`vercel.json\` and deploys the Web app instantly.

## 2. iOS Native App via Capacitor Wrapper
To wrap the Web application into a native iOS Xcode project:

\`\`\`bash
# Inside the root directory:
cd web
npm install @capacitor/core @capacitor/cli @capacitor/ios
npx cap init KunjachayaClub com.kunjachaya.club --web-dir dist
npm run build
npx cap add ios
npx cap open ios
\`\`\`

3. In Xcode, select your Signing Team and press Run on the iOS Simulator or physical iPhone!`;

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-sm">
        <span className="px-3 py-1 bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-300 text-xs font-bold uppercase tracking-wider rounded-md">
          Architecture & Integration
        </span>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white mt-2">
          Developer Documentation & API Reference
        </h2>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Firebase NoSQL JSON Schema, Node.js payment webhook, and Vercel / iOS build configurations
        </p>
      </div>

      {/* Code Snippet Cards */}
      <div className="space-y-6">
        
        {/* Firestore Schema */}
        <div className="bg-slate-900 text-slate-100 p-6 rounded-2xl border border-slate-800 space-y-3 font-mono text-xs">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div className="flex items-center gap-2 font-bold text-amber-400">
              <Database className="w-4 h-4" />
              <span>Firebase Firestore NoSQL Schema</span>
            </div>
            <button 
              onClick={() => copyToClipboard(firestoreSchema, 'schema')}
              className="p-1.5 hover:bg-slate-800 text-slate-400 hover:text-white rounded transition flex items-center gap-1"
            >
              {copiedSection === 'schema' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copiedSection === 'schema' ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="overflow-x-auto text-slate-300">{firestoreSchema}</pre>
        </div>

        {/* Cloud Function Code */}
        <div className="bg-slate-900 text-slate-100 p-6 rounded-2xl border border-slate-800 space-y-3 font-mono text-xs">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div className="flex items-center gap-2 font-bold text-indigo-400">
              <Server className="w-4 h-4" />
              <span>Node.js Firebase Payment Cloud Function</span>
            </div>
            <button 
              onClick={() => copyToClipboard(cloudFunctionCode, 'function')}
              className="p-1.5 hover:bg-slate-800 text-slate-400 hover:text-white rounded transition flex items-center gap-1"
            >
              {copiedSection === 'function' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copiedSection === 'function' ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="overflow-x-auto text-slate-300">{cloudFunctionCode}</pre>
        </div>

        {/* iOS & Vercel Guide */}
        <div className="bg-slate-900 text-slate-100 p-6 rounded-2xl border border-slate-800 space-y-3 font-mono text-xs">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div className="flex items-center gap-2 font-bold text-emerald-400">
              <Globe className="w-4 h-4" />
              <span>Vercel & iOS Deployment Setup</span>
            </div>
            <button 
              onClick={() => copyToClipboard(iosCapacitorGuide, 'ios')}
              className="p-1.5 hover:bg-slate-800 text-slate-400 hover:text-white rounded transition flex items-center gap-1"
            >
              {copiedSection === 'ios' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copiedSection === 'ios' ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="overflow-x-auto text-slate-300">{iosCapacitorGuide}</pre>
        </div>

      </div>

    </div>
  );
}
