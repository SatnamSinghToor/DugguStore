package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: UserProfile? = null,
    val error: String? = null,
    val successMessage: String? = null
)

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
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
            result.onSuccess { profile ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    user = profile,
                    successMessage = "Account created successfully!"
                )
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

    fun refreshProfile() {
        checkCurrentUser()
    }
}
