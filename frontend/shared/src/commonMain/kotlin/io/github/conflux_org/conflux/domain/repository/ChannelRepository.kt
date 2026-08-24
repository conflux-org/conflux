package io.github.conflux_org.conflux.domain.repository

import io.github.conflux_org.conflux.domain.model.Channel

interface ChannelRepository {
    suspend fun getChannelsByGuildId(guildId: Long): Result<List<Channel>>

    suspend fun createChannel(
        guildId: Long,
        name: String,
    ): Result<Channel>
}
