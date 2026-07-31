package io.github.conflux_org.conflux.features.auth.presentation

import androidx.lifecycle.ViewModel
import io.github.conflux_org.conflux.data.repository.AuthRepositoryImpl
import io.github.conflux_org.conflux.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
) : ViewModel() {
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

    private fun login(
        username: String,
        password: String,
    ) {
        _uiState.update { it.copy(isLoginLoading = true) }

        CoroutineScope(mainDispatcher).launch {
            try {
                authRepository
                    .login(username, password)
                    .onSuccess { user ->
                        _uiState.update { it.copy(errorMessage = "") }
                        onLoginSuccess?.invoke(user.id)
                    }.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message ?: "登入失敗") }
                    }
            } finally {
                _uiState.update { it.copy(isLoginLoading = false) }
            }
        }
    }

    private fun navigateToRegister() {
        onNavigateToRegister?.invoke()
    }

    var onLoginSuccess: ((userId: Long) -> Unit)? = null
    var onNavigateToRegister: (() -> Unit)? = null
}
