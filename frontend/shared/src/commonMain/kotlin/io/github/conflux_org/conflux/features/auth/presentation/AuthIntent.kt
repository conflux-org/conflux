package io.github.conflux_org.conflux.features.auth.presentation

sealed class AuthIntent {
    data class UsernameChanged(val username: String) : AuthIntent()
    data class PasswordChanged(val password: String) : AuthIntent()
    object Login : AuthIntent()
    object NavigateToRegister : AuthIntent()
}
