// Mirrors functions/index.js ROLES exactly and app/.../data/model/Roles.kt —
// these three must always agree, since the string values are also the Firebase
// custom claim values that the Cloud Functions enforce against.

export const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN: 'ADMIN',
  ENTREPRENEURIAL_MEMBER: 'ENTREPRENEURIAL_MEMBER',
  GENERAL_MEMBER: 'GENERAL_MEMBER',
  NEW_MEMBER: 'NEW_MEMBER',
};

const RANK = {
  SUPER_ADMIN: 4,
  ADMIN: 3,
  ENTREPRENEURIAL_MEMBER: 2,
  GENERAL_MEMBER: 1,
  NEW_MEMBER: 0,
};

export function rankOf(role) {
  return RANK[role] ?? 0;
}

// True for both ADMIN and SUPER_ADMIN — matches the server's requireAdmin().
export function isAdminLevel(role) {
  return role === ROLES.ADMIN || role === ROLES.SUPER_ADMIN;
}

export function isSuperAdmin(role) {
  return role === ROLES.SUPER_ADMIN;
}

const DISPLAY_EN = {
  SUPER_ADMIN: 'Super Admin',
  ADMIN: 'Admin',
  ENTREPRENEURIAL_MEMBER: 'Entrepreneurial Member',
  GENERAL_MEMBER: 'General Member',
  NEW_MEMBER: 'New Member',
};

const DISPLAY_BN = {
  SUPER_ADMIN: 'সুপার অ্যাডমিন',
  ADMIN: 'অ্যাডমিন',
  ENTREPRENEURIAL_MEMBER: 'উদ্যোক্তা সদস্য',
  GENERAL_MEMBER: 'সাধারণ সদস্য',
  NEW_MEMBER: 'নতুন সদস্য',
};

export function displayName(role, lang = 'en') {
  const table = lang === 'bn' ? DISPLAY_BN : DISPLAY_EN;
  return table[role] || role || (lang === 'bn' ? 'সদস্য' : 'Member');
}
