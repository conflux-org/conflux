package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.ChannelOverwrite
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelOverwriteDto(
    val id: Long,
    @SerialName("channel_id")
    val channelId: Long,
    @SerialName("target_type")
    val targetType: String,
    @SerialName("target_id")
    val targetId: Long,
    val allow: Long,
    val deny: Long,
    @SerialName("created_at")
    val createdAt: String? = null,
) {
    fun toDomain(): ChannelOverwrite =
        ChannelOverwrite(
            id = id,
            channelId = channelId,
            targetType =
                if (targetType.equals(OverwriteTargetType.MEMBER.name, ignoreCase = true)) {
                    OverwriteTargetType.MEMBER
                } else {
                    OverwriteTargetType.ROLE
                },
            targetId = targetId,
            allow = allow,
            deny = deny,
            createdAt = createdAt.orEmpty(),
        )
}
