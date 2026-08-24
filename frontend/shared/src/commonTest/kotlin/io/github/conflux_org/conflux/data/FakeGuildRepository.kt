package io.github.conflux_org.conflux.data

import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.repository.GuildRepository

class FakeGuildRepository(
    var shouldSucceed: Boolean = true,
    var mockGuilds: List<Guild> =
        listOf(
            Guild(id = 1L, name = "General Guild"),
            Guild(id = 2L, name = "Gaming Hub"),
        ),
    var mockCreatedGuild: Guild = Guild(id = 999L, name = "New Guild"),
    var errorMessage: String = "載入伺服器失敗",
) : GuildRepository {
    override suspend fun getGuildsByUserId(userId: Long): Result<List<Guild>> =
        if (shouldSucceed) {
            Result.success(mockGuilds)
        } else {
            Result.failure(Exception(errorMessage))
        }

    override suspend fun createGuild(name: String): Result<Guild> =
        if (shouldSucceed) {
            Result.success(mockCreatedGuild.copy(name = name))
        } else {
            Result.failure(Exception(errorMessage))
        }
}
