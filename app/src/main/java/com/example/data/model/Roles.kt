package com.example.data.model

/**
 * The five-tier role hierarchy, mirrored exactly from functions/index.js (ROLES).
 * These string values must match the Firebase custom claim / Firestore `role`
 * field set server-side — never invent a different casing/spelling here, or
 * role checks will silently fail open or closed depending on the bug.
 */
object Roles {
    const val SUPER_ADMIN = "SUPER_ADMIN"
    const val ADMIN = "ADMIN"
    const val ENTREPRENEURIAL_MEMBER = "ENTREPRENEURIAL_MEMBER"
    const val GENERAL_MEMBER = "GENERAL_MEMBER"
    const val NEW_MEMBER = "NEW_MEMBER"

    private val RANK = mapOf(
        SUPER_ADMIN to 4,
        ADMIN to 3,
        ENTREPRENEURIAL_MEMBER to 2,
        GENERAL_MEMBER to 1,
        NEW_MEMBER to 0
    )

    fun rankOf(role: String?): Int = RANK[role] ?: 0

    /** True for both ADMIN and SUPER_ADMIN — matches the server's requireAdmin(). */
    fun isAdminLevel(role: String?): Boolean = role == ADMIN || role == SUPER_ADMIN

    fun isSuperAdmin(role: String?): Boolean = role == SUPER_ADMIN

    fun displayName(role: String?, bengali: Boolean = false): String = when (role) {
        SUPER_ADMIN -> if (bengali) "সুপার অ্যাডমিন" else "Super Admin"
        ADMIN -> if (bengali) "অ্যাডমিন" else "Admin"
        ENTREPRENEURIAL_MEMBER -> if (bengali) "উদ্যোক্তা সদস্য" else "Entrepreneurial Member"
        GENERAL_MEMBER -> if (bengali) "সাধারণ সদস্য" else "General Member"
        NEW_MEMBER -> if (bengali) "নতুন সদস্য" else "New Member"
        else -> role ?: (if (bengali) "সদস্য" else "Member")
    }
}
