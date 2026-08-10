package io.github.conflux_org.conflux.features.auth.presentation

sealed class AuthIntent {
    data class SwitchPage(
        val page: AuthPage,
    ) : AuthIntent()

    // Login
    data class LoginUsernameChanged(
        val username: String,
    ) : AuthIntent()

    data class LoginPasswordChanged(
        val password: String,
    ) : AuthIntent()

    data class LoginToggleShowPassword(
        val show: Boolean,
    ) : AuthIntent()

    data object Login : AuthIntent()

    // SignUp
    data class SignUpUsernameChanged(
        val username: String,
    ) : AuthIntent()

    data class SignUpPasswordChanged(
        val password: String,
    ) : AuthIntent()

    data class ToggleSignUpShowPassword(
        val show: Boolean,
    ) : AuthIntent()

    data object SignUp : AuthIntent()
}
