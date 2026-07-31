package io.github.conflux_org.conflux.features.auth.presentation

import io.github.conflux_org.conflux.features.auth.data.repository.AuthRepositoryImpl
import io.github.conflux_org.conflux.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val mainDispatcher: CoroutineDispatcher,
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.UsernameChanged -> {
                _uiState.update { it.copy(username = intent.username, errorMessage = "") }
            }
            is AuthIntent.PasswordChanged -> {
                _uiState.update { it.copy(password = intent.password, errorMessage = "") }
            }
            is AuthIntent.Login -> {
                login(_uiState.value.username, _uiState.value.password)
            }
            AuthIntent.NavigateToRegister -> navigateToRegister()
        }
    }

    private fun login(username: String, password: String) {
        _uiState.update { it.copy(isLoginLoading = true, errorMessage = "") }

        CoroutineScope(mainDispatcher).launch {
            try {
                val success = authRepository.login(username, password)
                if (success) {
                    onLoginSuccess?.invoke(username)
                } else {
                    _uiState.update { it.copy(errorMessage = "帳號或密碼錯誤，登入失敗") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "登入失敗") }
            } finally {
                _uiState.update { it.copy(isLoginLoading = false) }
            }
        }
    }

    private fun navigateToRegister() {
        onNavigateToRegister?.invoke()
    }

    var onLoginSuccess: ((userId: String) -> Unit)? = null
    var onNavigateToRegister: (() -> Unit)? = null
}
