package io.github.conflux_org.conflux.domain.model

object PermissionFlags {
    const val ADMINISTRATOR: Long = 1L shl 0 // 1
    const val MANAGE_GUILD: Long = 1L shl 1 // 2
    const val MANAGE_ROLES: Long = 1L shl 2 // 4
    const val MANAGE_CHANNELS: Long = 1L shl 3 // 8
    const val VIEW_CHANNEL: Long = 1L shl 4 // 16
    const val SEND_MESSAGES: Long = 1L shl 5 // 32
    const val MANAGE_MESSAGES: Long = 1L shl 6 // 64
    const val ALL_PERMISSIONS: Long = 0x7FL // 127

    fun hasPermission(
        permissions: Long,
        flag: Long,
    ): Boolean = (permissions and flag) == flag

    fun computeGuildPermissions(
        userId: Long,
        guildOwnerId: Long?,
        roles: List<Role>,
        userRoleIds: List<Long>,
    ): Long {
        if (guildOwnerId != null && guildOwnerId == userId) {
            return ALL_PERMISSIONS
        }

        val everyoneRole = roles.firstOrNull { it.isEveryone }
        var permissions = everyoneRole?.permissions ?: 0L

        val userRoles = roles.filter { it.id in userRoleIds && !it.isEveryone }
        for (role in userRoles) {
            permissions = permissions or role.permissions
        }

        if (hasPermission(permissions, ADMINISTRATOR)) {
            return ALL_PERMISSIONS
        }

        return permissions
    }

    fun computeChannelPermissions(
        userId: Long,
        guildOwnerId: Long?,
        guildRoles: List<Role>,
        userRoleIds: List<Long>,
        overwrites: List<ChannelOverwrite>,
    ): Long {
        if (guildOwnerId != null && guildOwnerId == userId) {
            return ALL_PERMISSIONS
        }

        val guildPermissions = computeGuildPermissions(userId, guildOwnerId, guildRoles, userRoleIds)
        if (hasPermission(guildPermissions, ADMINISTRATOR)) {
            return ALL_PERMISSIONS
        }

        var permissions = guildPermissions

        // 1. @everyone channel overwrite
        val everyoneRole = guildRoles.firstOrNull { it.isEveryone }
        if (everyoneRole != null) {
            val everyoneOverwrite =
                overwrites.firstOrNull {
                    it.targetType == OverwriteTargetType.ROLE && it.targetId == everyoneRole.id
                }
            if (everyoneOverwrite != null) {
                permissions = permissions and everyoneOverwrite.deny.inv()
                permissions = permissions or everyoneOverwrite.allow
            }
        }

        // 2. Member's role overwrites (excluding @everyone)
        val roleOverwrites =
            overwrites.filter {
                it.targetType == OverwriteTargetType.ROLE && it.targetId in userRoleIds && it.targetId != everyoneRole?.id
            }
        var rolesAllow = 0L
        var rolesDeny = 0L
        for (ow in roleOverwrites) {
            rolesAllow = rolesAllow or ow.allow
            rolesDeny = rolesDeny or ow.deny
        }
        permissions = permissions and rolesDeny.inv()
        permissions = permissions or rolesAllow

        // 3. Member-specific overwrite
        val memberOverwrite =
            overwrites.firstOrNull {
                it.targetType == OverwriteTargetType.MEMBER && it.targetId == userId
            }
        if (memberOverwrite != null) {
            permissions = permissions and memberOverwrite.deny.inv()
            permissions = permissions or memberOverwrite.allow
        }

        // 4. VIEW_CHANNEL dependency
        if (!hasPermission(permissions, VIEW_CHANNEL)) {
            return 0L
        }

        return permissions
    }
}
