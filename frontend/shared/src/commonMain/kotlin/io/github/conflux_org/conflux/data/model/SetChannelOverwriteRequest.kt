package io.github.conflux_org.conflux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SetChannelOverwriteRequest(
    val allow: Long = 0L,
    val deny: Long = 0L,
)
