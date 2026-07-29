import { db } from '../firebase';
import { collection, query, where, getDocs, onSnapshot } from 'firebase/firestore';
import { isAdminLevel } from './roles';

// The Firestore `users` schema (written by functions/index.js: registerResident,
// and read by the Android app) uses nameEn/holding/block. The web components were
// built against a different shape (name/flatNo/buildingBlock) from the old mock
// data. Rather than rewrite every view that reads currentUser, this normalizes
// once at the point of fetching from Firestore. `role` is passed through exactly
// as stored (SUPER_ADMIN/ADMIN/ENTREPRENEURIAL_MEMBER/GENERAL_MEMBER/NEW_MEMBER)
// — components that only care about admin-or-not should use isAdminLevel(role)
// from services/roles, not compare against a single string.
export function normalizeResident(id, data) {
  return {
    id,
    ...data,
    name: data.nameEn,
    nameBn: data.nameBn,
    flatNo: data.holding,
    buildingBlock: data.block,
    role: data.role,
    isAdmin: isAdminLevel(data.role),
    membershipStatus: data.membershipStatus,
  };
}

export async function findResidentByUid(uid) {
  const q = query(collection(db, 'users'), where('firebaseUid', '==', uid));
  const snap = await getDocs(q);
  if (snap.empty) return null;
  const docSnap = snap.docs[0];
  return normalizeResident(docSnap.id, docSnap.data());
}

/**
 * Live-subscribes to the resident's own profile so role/membershipStatus changes
 * made by an admin (via setUserRole / approveMembership) reflect immediately
 * without requiring a re-login.
 */
export function subscribeToResident(uid, onChange) {
  const q = query(collection(db, 'users'), where('firebaseUid', '==', uid));
  return onSnapshot(q, (snap) => {
    if (snap.empty) {
      onChange(null);
      return;
    }
    const docSnap = snap.docs[0];
    onChange(normalizeResident(docSnap.id, docSnap.data()));
  });
}
