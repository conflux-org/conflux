package io.github.conflux_org.conflux.domain.repository

import io.github.conflux_org.conflux.domain.model.Guild

interface GuildRepository {
    suspend fun getGuildsByUserId(userId: Long): Result<List<Guild>>
}
