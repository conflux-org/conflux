package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val username: String? = null,
    val password: String? = null,
)
