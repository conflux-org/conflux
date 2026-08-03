package io.github.conflux_org.conflux.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
)
