package io.github.conflux_org.conflux.domain.repository

import io.github.conflux_org.conflux.domain.model.Role

interface RoleRepository {
    suspend fun getGuildRoles(guildId: Long): Result<List<Role>>

    suspend fun createRole(
        guildId: Long,
        name: String,
        permissions: Long = 0L,
        position: Int = 0,
    ): Result<Role>

    suspend fun updateRole(
        guildId: Long,
        roleId: Long,
        name: String? = null,
        permissions: Long? = null,
        position: Int? = null,
    ): Result<Role>

    suspend fun deleteRole(
        guildId: Long,
        roleId: Long,
    ): Result<Unit>

    suspend fun assignMemberRole(
        guildId: Long,
        userId: Long,
        roleId: Long,
    ): Result<Unit>

    suspend fun removeMemberRole(
        guildId: Long,
        userId: Long,
        roleId: Long,
    ): Result<Unit>
}
