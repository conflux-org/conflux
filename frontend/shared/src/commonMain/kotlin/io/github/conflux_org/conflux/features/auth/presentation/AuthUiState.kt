package io.github.conflux_org.conflux.features.auth.presentation

enum class AuthPage {
    LOGIN,
    SIGN_UP,
    FORGOT_PASSWORD,
}

data class AuthUiState(
    val currentPage: AuthPage = AuthPage.LOGIN,
    // Login
    val username: String = "",
    val password: String = "",
    val showLoginPassword: Boolean = false,
    val isLoginLoading: Boolean = false,
    val loginError: String = "",
    // Sign Up (simplified to username & password)
    val signUpUsername: String = "",
    val signUpPassword: String = "",
    val showSignUpPassword: Boolean = false,
    val isSignUpLoading: Boolean = false,
    val signUpError: String = "",
) {
    val isLoginButtonEnabled: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !isLoginLoading

    val isSignUpButtonEnabled: Boolean
        get() = signUpUsername.isNotBlank() && signUpPassword.isNotBlank() && !isSignUpLoading
}
