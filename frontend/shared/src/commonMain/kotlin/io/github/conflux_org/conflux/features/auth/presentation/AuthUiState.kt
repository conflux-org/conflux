package io.github.conflux_org.conflux.features.auth.presentation

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoginLoading: Boolean = false,
    val errorMessage: String = "",
) {
    val isLoginButtonEnabled: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !isLoginLoading
}
