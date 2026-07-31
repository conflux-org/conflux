package io.github.conflux_org.conflux.features.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val id: Long? = null,
    val name: String? = null,
    val user: UserData? = null,
)

@Serializable
data class UserData(
    val id: Long,
    val name: String,
)
