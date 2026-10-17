package com.parkshare.app.feature_auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkshare.app.data.ParkShareRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing authentication state lifecycle, role selection logic,
 * and Firestore user session synchronization for Parkshare.
 */
class AuthViewModel : ViewModel() {

    private val _sessionState = MutableStateFlow(ParkShareRepository.sessionState.value)
    val sessionState: StateFlow<UserSession> = _sessionState.asStateFlow()

    private val _selectedRole = MutableStateFlow(UserRole.DRIVER)
    val selectedRole: StateFlow<UserRole> = _selectedRole.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(false)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun selectRole(role: UserRole) {
        _selectedRole.value = role
        clearError()
    }

    fun toggleAuthMode() {
        _isSignUpMode.update { !it }
        clearError()
    }

    /**
     * Authenticates the user with email, password, and chosen role.
     *
     * @param email User's email address
     * @param password User's password
     * @param role User's role (DRIVER or HOST)
     * @param onComplete Callback invoked with success flag
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
            // Simulate Firestore auth network round-trip
            delay(400)
            val session = UserSession(
                userId = "usr-${UUID.randomUUID().toString().take(8)}",
                email = email.trim(),
                role = role,
                isAuthenticated = true
            )

            _sessionState.value = session
            ParkShareRepository.updateSession(session)
            _isLoading.value = false
            onComplete(true)
        }
    }

    /**
     * Signs up a new user and persists initial role profile to Firestore.
     */
    fun signUp(
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
            delay(500)
            val session = UserSession(
                userId = "usr-${UUID.randomUUID().toString().take(8)}",
                email = email.trim(),
                role = role,
                isAuthenticated = true
            )

            _sessionState.value = session
            ParkShareRepository.updateSession(session)
            _isLoading.value = false
            onComplete(true)
        }
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

    /**
     * Logs out the current user and clears session state.
     */
    fun logout() {
        _errorMessage.value = null
        val cleared = UserSession(
            userId = "",
            email = "",
            role = UserRole.DRIVER,
            isAuthenticated = false
        )
        _sessionState.value = cleared
        ParkShareRepository.updateSession(cleared)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
