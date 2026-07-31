package io.github.conflux_org.conflux.features.auth.data.model

import io.github.conflux_org.conflux.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val user: User? = null,
)
