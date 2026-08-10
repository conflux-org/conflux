package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String? = null,
)
