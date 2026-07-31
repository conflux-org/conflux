package io.github.conflux_org.conflux.features.auth.presentation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val mainDispatcher: CoroutineDispatcher
) {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UsernameChanged -> {
                _uiState.update { it.copy(username = intent.username, errorMessage = "") }
            }
            is LoginIntent.PasswordChanged -> {
                _uiState.update { it.copy(password = intent.password, errorMessage = "") }
            }
            is LoginIntent.Login -> {
                login(_uiState.value.username, _uiState.value.password)
            }
            LoginIntent.NavigateToRegister -> navigateToRegister()
        }
    }

    private fun login(username: String, password: String) {
        _uiState.update { it.copy(isLoginLoading = true, errorMessage = "") }

        CoroutineScope(mainDispatcher).launch {
            try {
                // TODO("repository 調用")
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
