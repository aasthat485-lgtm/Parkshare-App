package com.parkshare.app.feature_auth

/**
 * User roles available within the ParkShare platform.
 * DRIVER: Users seeking, discovering, and booking parking spaces.
 * HOST: Users listing, pricing, and managing parking spaces.
 */
enum class UserRole {
    DRIVER,
    HOST
}

/**
 * Production-grade User Profile matching Firestore "users" collection document schema.
 *
 * @property uid Unique Firebase Auth user identifier.
 * @property name User's full display name.
 * @property email User's email address.
 * @property role Primary account role (DRIVER or HOST).
 * @property activeRole Currently active operational mode in UI (supports seamless role switching).
 * @property phone Contact phone number.
 * @property createdAt Epoch timestamp of user creation.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.DRIVER,
    val activeRole: UserRole = role,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts UserProfile to a Firestore-compatible document Map.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to role.name,
            "activeRole" to activeRole.name,
            "phone" to phone,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): UserProfile {
            val roleVal = (map["role"] as? String)?.let {
                runCatching { UserRole.valueOf(it) }.getOrNull()
            } ?: UserRole.DRIVER

            val activeRoleVal = (map["activeRole"] as? String)?.let {
                runCatching { UserRole.valueOf(it) }.getOrNull()
            } ?: roleVal

            return UserProfile(
                uid = map["uid"] as? String ?: "",
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                role = roleVal,
                activeRole = activeRoleVal,
                phone = map["phone"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Data class representing the active user session state.
 */
data class UserSession(
    val profile: UserProfile = UserProfile(),
    val isAuthenticated: Boolean = false
) {
    val email: String get() = profile.email
    val role: UserRole get() = profile.activeRole
    val uid: String get() = profile.uid
}
