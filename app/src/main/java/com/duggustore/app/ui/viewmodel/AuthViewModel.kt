package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: UserProfile? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val requiresEmailVerification: Boolean = false,
    val pendingVerificationEmail: String = "",
    val verificationResent: Boolean = false
)

const val VERIFICATION_CODE_LENGTH = 6

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            if (!SessionManager.isLoggedIn()) return@launch
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.getCurrentUserProfile()
            result.onSuccess { profile ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoggedIn = profile != null,
                    user = profile
                )
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String, phone: String, role: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.signUp(email, password, fullName, phone, role)
            result.onSuccess { signUpResult ->
                if (signUpResult.requiresVerification) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        requiresEmailVerification = true,
                        pendingVerificationEmail = signUpResult.email
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = signUpResult.profile,
                        successMessage = "Account created successfully!"
                    )
                }
            }
            result.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign up failed"
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.signIn(email, password)
            result.onSuccess { profile ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    user = profile
                )
            }
            result.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    /**
     * [email] comes from the verify route, so the flow still works if the process was
     * killed and restored while the user was in their mail app and the in-memory
     * pendingVerificationEmail is gone.
     */
    fun verifyEmailCode(email: String, code: String) {
        val target = email.ifBlank { _state.value.pendingVerificationEmail }
        if (target.isBlank()) {
            _state.value = _state.value.copy(error = "No email to verify. Please sign up again.")
            return
        }
        if (code.length != VERIFICATION_CODE_LENGTH) {
            _state.value = _state.value.copy(error = "Enter the $VERIFICATION_CODE_LENGTH-digit code from your email.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.verifyEmailCode(target, code)
            result.onSuccess { profile ->
                // Clearing requiresEmailVerification alongside isLoggedIn keeps the
                // verify screen from re-triggering once navigation moves on.
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    user = profile,
                    requiresEmailVerification = false,
                    pendingVerificationEmail = "",
                    verificationResent = false,
                    successMessage = "Email verified!"
                )
            }
            result.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Verification failed"
                )
            }
        }
    }

    fun resendVerificationEmail(email: String = "") {
        val target = email.ifBlank { _state.value.pendingVerificationEmail }
        if (target.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.resendVerificationEmail(target)
            result.onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    verificationResent = true
                )
            }
            result.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to resend email"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _state.value = AuthState()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun resetVerificationState() {
        _state.value = _state.value.copy(
            requiresEmailVerification = false,
            pendingVerificationEmail = "",
            verificationResent = false
        )
    }

    fun refreshProfile() {
        checkCurrentUser()
    }
}
