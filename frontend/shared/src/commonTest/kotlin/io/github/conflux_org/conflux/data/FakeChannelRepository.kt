package io.github.conflux_org.conflux.data

import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.repository.ChannelRepository

class FakeChannelRepository(
    var shouldSucceed: Boolean = true,
    var mockChannels: List<Channel> =
        listOf(
            Channel(id = 101L, name = "general"),
            Channel(id = 102L, name = "announcements"),
        ),
    var mockCreatedChannel: Channel = Channel(id = 999L, name = "new-channel"),
    var errorMessage: String = "載入頻道失敗",
) : ChannelRepository {
    override suspend fun getChannelsByGuildId(guildId: Long): Result<List<Channel>> =
        if (shouldSucceed) {
            Result.success(mockChannels)
        } else {
            Result.failure(Exception(errorMessage))
        }

    override suspend fun createChannel(
        guildId: Long,
        name: String,
    ): Result<Channel> =
        if (shouldSucceed) {
            Result.success(mockCreatedChannel.copy(name = name))
        } else {
            Result.failure(Exception(errorMessage))
        }
}
