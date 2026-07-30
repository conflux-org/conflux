package io.github.conflux_org.conflux.features.auth.presentation

import io.github.conflux_org.conflux.core.common.BaseUiState

data class LoginBusinessState(
    val username: String = "",
    val password: String = "",
    val isLoginButtonEnabled: Boolean = false,
    val loginLoading: Boolean = false
)

typealias LoginUiState = BaseUiState<LoginBusinessState>
