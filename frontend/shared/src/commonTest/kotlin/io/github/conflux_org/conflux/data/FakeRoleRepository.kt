package io.github.conflux_org.conflux.data

import io.github.conflux_org.conflux.domain.model.PermissionFlags
import io.github.conflux_org.conflux.domain.model.Role
import io.github.conflux_org.conflux.domain.repository.RoleRepository

class FakeRoleRepository(
    var shouldSucceed: Boolean = true,
    var mockRoles: MutableList<Role> =
        mutableListOf(
            Role(
                id = 1L,
                guildId = 1L,
                name = "@everyone",
                permissions = PermissionFlags.VIEW_CHANNEL or PermissionFlags.SEND_MESSAGES,
                position = 0,
                isEveryone = true,
            ),
            Role(
                id = 2L,
                guildId = 1L,
                name = "Moderator",
                permissions = PermissionFlags.MANAGE_CHANNELS or PermissionFlags.MANAGE_MESSAGES,
                position = 1,
                isEveryone = false,
            ),
        ),
    var errorMessage: String = "操作身份組失敗",
) : RoleRepository {
    var assignedMemberRoles: MutableList<Triple<Long, Long, Long>> = mutableListOf()

    override suspend fun getGuildRoles(guildId: Long): Result<List<Role>> =
        if (shouldSucceed) {
            Result.success(mockRoles.filter { it.guildId == guildId })
        } else {
            Result.failure(Exception(errorMessage))
        }

    override suspend fun createRole(
        guildId: Long,
        name: String,
        permissions: Long,
        position: Int,
    ): Result<Role> {
        if (!shouldSucceed) return Result.failure(Exception(errorMessage))
        val newRole =
            Role(
                id = (mockRoles.maxOfOrNull { it.id } ?: 0L) + 1L,
                guildId = guildId,
                name = name,
                permissions = permissions,
                position = position,
                isEveryone = false,
            )
        mockRoles.add(newRole)
        return Result.success(newRole)
    }

    override suspend fun updateRole(
        guildId: Long,
        roleId: Long,
        name: String?,
        permissions: Long?,
        position: Int?,
    ): Result<Role> {
        if (!shouldSucceed) return Result.failure(Exception(errorMessage))
        val index = mockRoles.indexOfFirst { it.id == roleId && it.guildId == guildId }
        if (index == -1) return Result.failure(Exception("Role not found"))
        val existing = mockRoles[index]
        val updated =
            existing.copy(
                name = name ?: existing.name,
                permissions = permissions ?: existing.permissions,
                position = position ?: existing.position,
            )
        mockRoles[index] = updated
        return Result.success(updated)
    }

    override suspend fun deleteRole(
        guildId: Long,
        roleId: Long,
    ): Result<Unit> {
        if (!shouldSucceed) return Result.failure(Exception(errorMessage))
        val removed = mockRoles.removeAll { it.id == roleId && it.guildId == guildId }
        return if (removed) Result.success(Unit) else Result.failure(Exception("Role not found"))
    }

    override suspend fun assignMemberRole(
        guildId: Long,
        userId: Long,
        roleId: Long,
    ): Result<Unit> {
        if (!shouldSucceed) return Result.failure(Exception(errorMessage))
        assignedMemberRoles.add(Triple(guildId, userId, roleId))
        return Result.success(Unit)
    }

    override suspend fun removeMemberRole(
        guildId: Long,
        userId: Long,
        roleId: Long,
    ): Result<Unit> {
        if (!shouldSucceed) return Result.failure(Exception(errorMessage))
        assignedMemberRoles.removeAll { it.first == guildId && it.second == userId && it.third == roleId }
        return Result.success(Unit)
    }
}
