package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateGuildRequest(
    val name: String,
)
