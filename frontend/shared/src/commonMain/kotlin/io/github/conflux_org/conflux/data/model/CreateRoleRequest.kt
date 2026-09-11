package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateRoleRequest(
    val name: String,
    val permissions: Long = 0L,
    val position: Int = 0,
)
