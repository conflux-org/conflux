package io.github.conflux_org.conflux.data

import io.github.conflux_org.conflux.domain.model.ChannelOverwrite
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType
import io.github.conflux_org.conflux.domain.repository.ChannelOverwriteRepository

class FakeChannelOverwriteRepository(
    var shouldSucceed: Boolean = true,
    var mockOverwrites: MutableList<ChannelOverwrite> =
        mutableListOf(
            ChannelOverwrite(
                id = 1L,
                channelId = 101L,
                targetType = OverwriteTargetType.ROLE,
                targetId = 1L,
                allow = 0L,
                deny = 32L,
            ),
        ),
    var errorMessage: String = "操作頻道覆寫失敗",
) : ChannelOverwriteRepository {
    override suspend fun getChannelOverwrites(channelId: Long): Result<List<ChannelOverwrite>> =
        if (shouldSucceed) {
            Result.success(mockOverwrites.filter { it.channelId == channelId })
        } else {
            Result.failure(Exception(errorMessage))
        }

    override suspend fun setChannelOverwrite(
        channelId: Long,
        targetType: OverwriteTargetType,
        targetId: Long,
        allow: Long,
        deny: Long,
    ): Result<ChannelOverwrite> {
        if (!shouldSucceed) return Result.failure(Exception(errorMessage))
        val index =
            mockOverwrites.indexOfFirst {
                it.channelId == channelId && it.targetType == targetType && it.targetId == targetId
            }
        val overwrite =
            if (index != -1) {
                val updated = mockOverwrites[index].copy(allow = allow, deny = deny)
                mockOverwrites[index] = updated
                updated
            } else {
                val created =
                    ChannelOverwrite(
                        id = (mockOverwrites.maxOfOrNull { it.id } ?: 0L) + 1L,
                        channelId = channelId,
                        targetType = targetType,
                        targetId = targetId,
                        allow = allow,
                        deny = deny,
                    )
                mockOverwrites.add(created)
                created
            }
        return Result.success(overwrite)
    }

    override suspend fun deleteChannelOverwrite(
        channelId: Long,
        targetType: OverwriteTargetType,
        targetId: Long,
    ): Result<Unit> {
        if (!shouldSucceed) return Result.failure(Exception(errorMessage))
        val removed =
            mockOverwrites.removeAll {
                it.channelId == channelId && it.targetType == targetType && it.targetId == targetId
            }
        return if (removed) Result.success(Unit) else Result.failure(Exception("Overwrite not found"))
    }
}
