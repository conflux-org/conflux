package io.github.conflux_org.conflux.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionCalculationTest {
    @Test
    fun ownerAlwaysHasAllPermissions() {
        val ownerId = 1L
        val roles = emptyList<Role>()
        val userRoleIds = emptyList<Long>()
        val overwrites = emptyList<ChannelOverwrite>()

        val guildPerms = PermissionFlags.computeGuildPermissions(ownerId, ownerId, roles, userRoleIds)
        assertEquals(PermissionFlags.ALL_PERMISSIONS, guildPerms)

        val channelPerms =
            PermissionFlags.computeChannelPermissions(ownerId, ownerId, roles, userRoleIds, overwrites)
        assertEquals(PermissionFlags.ALL_PERMISSIONS, channelPerms)
    }

    @Test
    fun memberInheritsBasePermissionsFromEveryoneRole() {
        val userId = 2L
        val ownerId = 1L
        val everyoneRole =
            Role(
                id = 10L,
                guildId = 1L,
                name = "@everyone",
                permissions = PermissionFlags.VIEW_CHANNEL or PermissionFlags.SEND_MESSAGES,
                position = 0,
                isEveryone = true,
            )
        val roles = listOf(everyoneRole)

        val guildPerms = PermissionFlags.computeGuildPermissions(userId, ownerId, roles, emptyList())
        assertEquals(PermissionFlags.VIEW_CHANNEL or PermissionFlags.SEND_MESSAGES, guildPerms)
        assertTrue(PermissionFlags.hasPermission(guildPerms, PermissionFlags.VIEW_CHANNEL))
        assertTrue(PermissionFlags.hasPermission(guildPerms, PermissionFlags.SEND_MESSAGES))
        assertFalse(PermissionFlags.hasPermission(guildPerms, PermissionFlags.MANAGE_CHANNELS))
    }

    @Test
    fun memberAccumulatesPermissionsFromAssignedRoles() {
        val userId = 2L
        val ownerId = 1L
        val everyoneRole =
            Role(
                id = 10L,
                guildId = 1L,
                name = "@everyone",
                permissions = PermissionFlags.VIEW_CHANNEL,
                position = 0,
                isEveryone = true,
            )
        val modRole =
            Role(
                id = 20L,
                guildId = 1L,
                name = "Moderator",
                permissions = PermissionFlags.SEND_MESSAGES or PermissionFlags.MANAGE_MESSAGES,
                position = 1,
                isEveryone = false,
            )
        val roles = listOf(everyoneRole, modRole)

        val guildPerms = PermissionFlags.computeGuildPermissions(userId, ownerId, roles, listOf(20L))
        val expected = PermissionFlags.VIEW_CHANNEL or PermissionFlags.SEND_MESSAGES or PermissionFlags.MANAGE_MESSAGES
        assertEquals(expected, guildPerms)
    }

    @Test
    fun administratorBypassesAllChannelOverwrites() {
        val userId = 2L
        val ownerId = 1L
        val adminRole =
            Role(
                id = 20L,
                guildId = 1L,
                name = "Admin",
                permissions = PermissionFlags.ADMINISTRATOR,
                position = 1,
                isEveryone = false,
            )
        val overwrite =
            ChannelOverwrite(
                id = 1L,
                channelId = 100L,
                targetType = OverwriteTargetType.MEMBER,
                targetId = userId,
                allow = 0L,
                deny = PermissionFlags.ALL_PERMISSIONS,
            )

        val perms =
            PermissionFlags.computeChannelPermissions(
                userId = userId,
                guildOwnerId = ownerId,
                guildRoles = listOf(adminRole),
                userRoleIds = listOf(20L),
                overwrites = listOf(overwrite),
            )
        assertEquals(PermissionFlags.ALL_PERMISSIONS, perms)
    }

    @Test
    fun channelOverwriteModifiesPermissionsCorrectly() {
        val userId = 2L
        val ownerId = 1L
        val everyoneRole =
            Role(
                id = 10L,
                guildId = 1L,
                name = "@everyone",
                permissions = PermissionFlags.VIEW_CHANNEL or PermissionFlags.SEND_MESSAGES,
                position = 0,
                isEveryone = true,
            )
        // @everyone overwrite denies SEND_MESSAGES
        val everyoneOverwrite =
            ChannelOverwrite(
                id = 1L,
                channelId = 100L,
                targetType = OverwriteTargetType.ROLE,
                targetId = 10L,
                allow = 0L,
                deny = PermissionFlags.SEND_MESSAGES,
            )

        val perms =
            PermissionFlags.computeChannelPermissions(
                userId = userId,
                guildOwnerId = ownerId,
                guildRoles = listOf(everyoneRole),
                userRoleIds = emptyList(),
                overwrites = listOf(everyoneOverwrite),
            )
        assertTrue(PermissionFlags.hasPermission(perms, PermissionFlags.VIEW_CHANNEL))
        assertFalse(PermissionFlags.hasPermission(perms, PermissionFlags.SEND_MESSAGES))
    }

    @Test
    fun memberOverwriteOverridesRoleOverwrite() {
        val userId = 2L
        val ownerId = 1L
        val everyoneRole =
            Role(
                id = 10L,
                guildId = 1L,
                name = "@everyone",
                permissions = PermissionFlags.VIEW_CHANNEL,
                position = 0,
                isEveryone = true,
            )
        val mutedRole =
            Role(
                id = 20L,
                guildId = 1L,
                name = "Muted",
                permissions = 0L,
                position = 1,
                isEveryone = false,
            )
        val roleOverwrite =
            ChannelOverwrite(
                id = 1L,
                channelId = 100L,
                targetType = OverwriteTargetType.ROLE,
                targetId = 20L,
                allow = 0L,
                deny = PermissionFlags.SEND_MESSAGES,
            )
        val memberOverwrite =
            ChannelOverwrite(
                id = 2L,
                channelId = 100L,
                targetType = OverwriteTargetType.MEMBER,
                targetId = userId,
                allow = PermissionFlags.SEND_MESSAGES,
                deny = 0L,
            )

        val perms =
            PermissionFlags.computeChannelPermissions(
                userId = userId,
                guildOwnerId = ownerId,
                guildRoles = listOf(everyoneRole, mutedRole),
                userRoleIds = listOf(20L),
                overwrites = listOf(roleOverwrite, memberOverwrite),
            )
        assertTrue(PermissionFlags.hasPermission(perms, PermissionFlags.SEND_MESSAGES))
    }

    @Test
    fun lackingViewChannelRevokesAllChannelPermissions() {
        val userId = 2L
        val ownerId = 1L
        val everyoneRole =
            Role(
                id = 10L,
                guildId = 1L,
                name = "@everyone",
                permissions = PermissionFlags.SEND_MESSAGES, // No VIEW_CHANNEL
                position = 0,
                isEveryone = true,
            )

        val perms =
            PermissionFlags.computeChannelPermissions(
                userId = userId,
                guildOwnerId = ownerId,
                guildRoles = listOf(everyoneRole),
                userRoleIds = emptyList(),
                overwrites = emptyList(),
            )
        assertEquals(0L, perms)
    }
}
