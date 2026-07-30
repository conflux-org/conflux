package io.github.conflux_org.conflux.features.auth.presentation

sealed class LoginIntent {
    data class Login(val username: String, val password: String) : LoginIntent()
    object ClearInput : LoginIntent()
    object NavigateToRegister : LoginIntent()
}