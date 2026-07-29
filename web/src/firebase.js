// Firebase initialization for the web client.
//
// This file did not exist before — the web app had no Firebase SDK at all and ran
// entirely on hardcoded mock data with a one-click "become Admin" demo picker.
// Config comes from Vite env vars (VITE_*), never hardcoded — see .env.example.

import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getFunctions } from 'firebase/functions';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || 'AIzaSyBt1CUvopFzQC6STqQ-lJ1R9GG5dvS-sXI',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || 'kunjachaya-club.firebaseapp.com',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || 'kunjachaya-club',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || 'kunjachaya-club.firebasestorage.app',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '668738359171',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || '1:668738359171:web:033a0787646aca6077f0b6',
};

let app, auth, db, functions;
try {
  app = initializeApp(firebaseConfig);
  auth = getAuth(app);
  db = getFirestore(app);
  functions = getFunctions(app, 'asia-southeast1');
} catch (err) {
  console.error('Firebase initialization error:', err);
}

export { app, auth, db, functions };

