package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val id: Long? = null,
    val name: String? = null,
    val error: String? = null,
)
