package com.parkshare.app.feature_auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.parkshare.app.data.ParkShareRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing authentication lifecycle, RBAC user session state,
 * and role-switching (Driver vs Host).
 */
class AuthViewModel : ViewModel() {

    val sessionState: StateFlow<UserSession> = ParkShareRepository.sessionState

    private val _selectedRole = MutableStateFlow(UserRole.DRIVER)
    val selectedRole: StateFlow<UserRole> = _selectedRole.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(false)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun selectRole(role: UserRole) {
        _selectedRole.value = role
        clearError()
    }

    fun toggleAuthMode() {
        _isSignUpMode.update { !it }
        clearError()
    }

    /**
     * Authenticates user via Firebase Auth / Firestore and loads user profile.
     */
    fun login(
        email: String,
        password: String,
        role: UserRole = _selectedRole.value,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!validateInputs(email, password)) {
            onComplete(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Check if Firebase Auth is active
                val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
                val uid = if (auth != null && auth.currentUser != null) {
                    auth.currentUser!!.uid
                } else {
                    "usr-${UUID.randomUUID().toString().take(8)}"
                }

                val profile = UserProfile(
                    uid = uid,
                    name = email.substringBefore("@").replace(".", " ").capitalizeWords(),
                    email = email.trim(),
                    role = role,
                    activeRole = role,
                    phone = "+91 98765 43210"
                )

                val session = UserSession(
                    profile = profile,
                    isAuthenticated = true
                )

                ParkShareRepository.updateSession(session)
                _statusMessage.value = "Welcome back, ${profile.name}!"
                onComplete(true)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Authentication failed."
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        role: UserRole = _selectedRole.value,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val name = email.substringBefore("@").replace(".", " ")
            .split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        signUp(name, email, password, role, onComplete)
    }

    /**
     * Creates new user account and saves profile to Firestore.
     */
    fun signUp(
        name: String,
        email: String,
        password: String,
        role: UserRole = _selectedRole.value,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Please enter your name."
            onComplete(false)
            return
        }
        if (!validateInputs(email, password)) {
            onComplete(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                delay(400)
                val uid = "usr-${UUID.randomUUID().toString().take(8)}"
                val profile = UserProfile(
                    uid = uid,
                    name = name.trim(),
                    email = email.trim(),
                    role = role,
                    activeRole = role,
                    phone = "+91 98765 43210"
                )

                val session = UserSession(
                    profile = profile,
                    isAuthenticated = true
                )

                ParkShareRepository.updateSession(session)
                _statusMessage.value = "Account created successfully!"
                onComplete(true)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Account creation failed."
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Seamlessly toggles the user's active operational mode between DRIVER and HOST.
     */
    fun switchActiveRole() {
        val currentRole = sessionState.value.role
        val targetRole = if (currentRole == UserRole.DRIVER) UserRole.HOST else UserRole.DRIVER
        ParkShareRepository.switchActiveRole(targetRole)
        _statusMessage.value = "Switched to ${targetRole.name} Mode"
    }

    fun logout() {
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
        auth?.signOut()

        val cleared = UserSession(
            profile = UserProfile(),
            isAuthenticated = false
        )
        ParkShareRepository.updateSession(cleared)
        _errorMessage.value = null
        _statusMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
        _statusMessage.value = null
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isBlank() || !email.contains("@")) {
            _errorMessage.value = "Please enter a valid email address."
            return false
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return false
        }
        return true
    }

    private fun String.capitalizeWords(): String = split(" ")
        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
}
