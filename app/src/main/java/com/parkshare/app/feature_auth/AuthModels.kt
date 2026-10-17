package com.parkshare.app.feature_auth

/**
 * User roles available within the Parkshare platform.
 * DRIVER: Users seeking, discovering, and booking parking spaces.
 * HOST: Users listing, pricing, and managing parking spaces.
 */
enum class UserRole {
    DRIVER,
    HOST
}

/**
 * Data class representing the active user session state.
 *
 * @property userId Unique identifier for the user account.
 * @property email The authenticated user's email address.
 * @property role The current active role (DRIVER or HOST).
 * @property isAuthenticated Flag indicating whether the user is successfully authenticated.
 */
data class UserSession(
    val userId: String = "",
    val email: String = "",
    val role: UserRole = UserRole.DRIVER,
    val isAuthenticated: Boolean = false
) {
    /**
     * Converts UserSession to a Firestore-compatible document Map.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "email" to email,
            "role" to role.name,
            "isAuthenticated" to isAuthenticated
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): UserSession {
            return UserSession(
                userId = map["userId"] as? String ?: "",
                email = map["email"] as? String ?: "",
                role = (map["role"] as? String)?.let {
                    runCatching { UserRole.valueOf(it) }.getOrNull()
                } ?: UserRole.DRIVER,
                isAuthenticated = map["isAuthenticated"] as? Boolean ?: false
            )
        }
    }
}
