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
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

if (!firebaseConfig.apiKey || !firebaseConfig.projectId) {
  // Fail loudly in dev rather than silently running against an undefined project.
  console.error(
    'Firebase config is missing. Copy .env.example to .env.local and fill in your ' +
    'Firebase project settings (Project Settings -> General -> Your apps -> SDK setup).'
  );
}

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
// Must match the region set in functions/index.js (setGlobalOptions region).
export const functions = getFunctions(app, 'asia-southeast1');
