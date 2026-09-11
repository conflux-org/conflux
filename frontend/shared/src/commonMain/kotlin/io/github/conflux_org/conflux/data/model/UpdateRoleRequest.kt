package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateRoleRequest(
    val name: String? = null,
    val permissions: Long? = null,
    val position: Int? = null,
)
