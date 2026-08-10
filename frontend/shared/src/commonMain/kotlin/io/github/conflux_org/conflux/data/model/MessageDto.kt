package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.Message
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: Long,
    val author: UserDto,
    val content: String,
) {
    fun toDomain(): Message = Message(id, author.toDomain(), content)
}
