package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SignupResponse(
    val id: Long,
    val name: String,
)
