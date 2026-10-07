package com.example.learning.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "", val password: String = "", val loading: Boolean = false,
    val emailError: String? = null, val passwordError: String? = null,
    val error: String? = null, val authenticated: Boolean = false
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val mutable = MutableStateFlow(LoginState())
    val state = mutable.asStateFlow()
    fun emailChanged(value: String) { mutable.update { it.copy(email = value, emailError = null, error = null) } }
    fun passwordChanged(value: String) { mutable.update { it.copy(password = value, passwordError = null, error = null) } }
    fun login() {
        val current = mutable.value
        if (current.loading) return
        val email = current.email.trim()
        val emailError = if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) "Enter a valid email address" else null
        val passwordError = if (current.password.isBlank()) "Enter your password" else null
        mutable.update { it.copy(emailError = emailError, passwordError = passwordError, error = null) }
        if (emailError != null || passwordError != null) return
        mutable.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                auth.login(email, current.password)
                mutable.update { it.copy(loading = false, authenticated = true, password = "") }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                mutable.update { it.copy(loading = false, error = e.message ?: "Unable to sign in. Please retry.") }
            }
        }
    }
}
