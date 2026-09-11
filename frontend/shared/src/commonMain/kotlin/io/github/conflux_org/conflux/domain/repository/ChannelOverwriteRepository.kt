package io.github.conflux_org.conflux.domain.repository

import io.github.conflux_org.conflux.domain.model.ChannelOverwrite
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType

interface ChannelOverwriteRepository {
    suspend fun getChannelOverwrites(channelId: Long): Result<List<ChannelOverwrite>>

    suspend fun setChannelOverwrite(
        channelId: Long,
        targetType: OverwriteTargetType,
        targetId: Long,
        allow: Long,
        deny: Long,
    ): Result<ChannelOverwrite>

    suspend fun deleteChannelOverwrite(
        channelId: Long,
        targetType: OverwriteTargetType,
        targetId: Long,
    ): Result<Unit>
}
