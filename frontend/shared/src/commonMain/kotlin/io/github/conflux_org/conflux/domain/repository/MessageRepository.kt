package io.github.conflux_org.conflux.domain.repository

import io.github.conflux_org.conflux.domain.model.Message

interface MessageRepository {
    suspend fun getMessagesByChannelId(channelId: Long): Result<List<Message>>

    suspend fun sendMessage(
        channelId: Long,
        content: String,
    ): Result<Message>
}
