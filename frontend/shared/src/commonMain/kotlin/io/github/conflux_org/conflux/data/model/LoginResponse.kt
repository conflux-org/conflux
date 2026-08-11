package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val id: Long,
    val name: String,
    val token: String? = null,
)
