package io.github.conflux_org.conflux.core.common

sealed class BaseUiState<out T> {
    object Loading : BaseUiState<Nothing>()
    data class Success<out T>(val data: T) : BaseUiState<T>()
    data class Error(
        val errorMsg: String,
        val retryAction: (() -> Unit)? = null
    ) : BaseUiState<Nothing>()
}