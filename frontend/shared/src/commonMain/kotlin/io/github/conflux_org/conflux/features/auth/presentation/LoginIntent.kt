package io.github.conflux_org.conflux.features.auth.presentation

sealed class LoginIntent {
    data class UsernameChanged(val username: String): LoginIntent()
    data class PasswordChanged(val password: String): LoginIntent()
    object Login: LoginIntent()
    object NavigateToRegister : LoginIntent()
}