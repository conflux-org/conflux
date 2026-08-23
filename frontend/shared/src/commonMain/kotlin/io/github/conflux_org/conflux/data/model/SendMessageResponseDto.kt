package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageResponseDto(
    val id: Long,
    @SerialName("channel_id") val channelId: Long,
    val author: UserDto,
    val content: String,
    @SerialName("created_at") val createdAt: String,
) {
    fun toDomain(): Message = Message(id, author.toDomain(), content)
}
