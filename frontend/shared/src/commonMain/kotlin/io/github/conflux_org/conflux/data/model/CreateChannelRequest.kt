package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateChannelRequest(
    val name: String,
)
