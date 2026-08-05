package io.github.conflux_org.conflux.domain.repository

import io.github.conflux_org.conflux.domain.model.Message

interface MessageRepository {
    suspend fun getMessageByChannelId(channelId: Long): Result<List<Message>>
}
