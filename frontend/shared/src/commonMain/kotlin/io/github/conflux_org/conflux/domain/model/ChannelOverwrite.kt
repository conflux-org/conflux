package io.github.conflux_org.conflux.domain.model

data class ChannelOverwrite(
    val id: Long,
    val channelId: Long,
    val targetType: OverwriteTargetType,
    val targetId: Long,
    val allow: Long,
    val deny: Long,
    val createdAt: String = "",
)
