package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.Channel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateChannelResponseDto(
    val id: Long,
    val name: String,
    @SerialName("guild_id")
    val guildId: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
) {
    fun toDomain(): Channel = Channel(id = id, name = name)
}
