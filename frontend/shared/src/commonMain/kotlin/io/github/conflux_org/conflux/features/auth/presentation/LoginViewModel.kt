package io.github.conflux_org.conflux.features.auth.presentation

import io.github.conflux_org.conflux.core.common.BaseUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val mainDispatcher: CoroutineDispatcher
) {
    private val _uiState = MutableStateFlow<LoginUiState>(
        BaseUiState.Success(LoginBusinessState())
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var currentBusinessState: LoginBusinessState
        get() = (_uiState.value as? BaseUiState.Success)?.data ?: LoginBusinessState()
        set(value) {
            _uiState.value = BaseUiState.Success(value)
        }

    fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.Login -> TODO("repository 調用")
            LoginIntent.ClearInput -> clearInput()
            LoginIntent.NavigateToRegister -> navigateToRegister()
        }
    }

    private fun login(username: String, password: String) {
        currentBusinessState = currentBusinessState.copy(loginLoading = true)

        CoroutineScope(mainDispatcher).launch {
            try {
                TODO("repository 調用")
            } catch (e: Exception) {
                _uiState.value = BaseUiState.Error(
                    errorMsg = e.message ?: "登入失敗，請稍候在試",
                    retryAction = { handleIntent(LoginIntent.Login(username, password)) }
                )
            } finally {
                currentBusinessState = currentBusinessState.copy(loginLoading = false)
            }
        }
    }

    private fun clearInput() {
        currentBusinessState = LoginBusinessState()
    }

    private fun navigateToRegister() {
        onNavigateToRegister?.invoke()
    }

    var onLoginSuccess: ((userId: String) -> Unit)? = null
    var onNavigateToRegister: (() -> Unit)? = null
}
