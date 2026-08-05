package io.github.conflux_org.conflux.domain.repository

import io.github.conflux_org.conflux.domain.model.Channel

interface ChannelRepository {
    suspend fun getChannelByGuildId(guildId: Long): Result<List<Channel>>
}
