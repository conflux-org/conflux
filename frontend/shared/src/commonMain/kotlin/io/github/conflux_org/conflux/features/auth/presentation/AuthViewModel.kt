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

    var onLoginSuccess: ((userId: Long) -> Unit)? = null
    var onSignUpSuccess: ((userId: Long) -> Unit)? = null

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.SwitchPage -> {
                _uiState.update { it.copy(currentPage = intent.page, loginError = "", signUpError = "") }
            }
            // Login
            is AuthIntent.LoginUsernameChanged -> {
                _uiState.update { it.copy(username = intent.username, loginError = "") }
            }
            is AuthIntent.LoginPasswordChanged -> {
                _uiState.update { it.copy(password = intent.password, loginError = "") }
            }
            is AuthIntent.LoginToggleShowPassword -> {
                _uiState.update { it.copy(showLoginPassword = intent.show) }
            }
            is AuthIntent.Login -> {
                login(_uiState.value.username, _uiState.value.password)
            }
            // SignUp
            is AuthIntent.SignUpUsernameChanged -> {
                _uiState.update { it.copy(signUpUsername = intent.username, signUpError = "") }
            }
            is AuthIntent.SignUpPasswordChanged -> {
                _uiState.update { it.copy(signUpPassword = intent.password, signUpError = "") }
            }
            is AuthIntent.ToggleSignUpShowPassword -> {
                _uiState.update { it.copy(showSignUpPassword = intent.show) }
            }
            is AuthIntent.SignUp -> {
                signUp(_uiState.value.signUpUsername, _uiState.value.signUpPassword)
            }
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
                        _uiState.update { it.copy(loginError = "") }
                        onLoginSuccess?.invoke(user.id)
                    }.onFailure { error ->
                        _uiState.update { it.copy(loginError = error.message ?: "登入失敗") }
                    }
            } finally {
                _uiState.update { it.copy(isLoginLoading = false) }
            }
        }
    }

    private fun signUp(
        username: String,
        password: String,
    ) {
        if (username.isBlank()) {
            _uiState.update { it.copy(signUpError = "請輸入用戶名稱") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(signUpError = "密碼至少需要 6 個字元") }
            return
        }

        // TODO: Implement signup repository call in future steps
        _uiState.update { it.copy(signUpError = "") }
        onSignUpSuccess?.invoke(1L)
    }
}
