package io.github.conflux_org.conflux.features.main.presentation

import io.github.conflux_org.conflux.core.ui.components.MemberData
import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.ChannelOverwrite
import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.model.Message
import io.github.conflux_org.conflux.domain.model.PermissionFlags
import io.github.conflux_org.conflux.domain.model.Role

data class MainUiState(
    val isLoadingGuilds: Boolean = false,
    val isLoadingChannels: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val currentUserId: Long = 1L,
    val guilds: List<Guild> = emptyList(),
    val selectedGuild: Guild? = null,
    val channels: List<Channel> = emptyList(),
    val selectedChannel: Channel? = null,
    val messages: List<Message> = emptyList(),
    val errorMessage: String? = null,
    val isCreatingGuild: Boolean = false,
    val isCreatingChannel: Boolean = false,
    val showCreateGuildDialog: Boolean = false,
    val showCreateChannelDialog: Boolean = false,
    val createGuildError: String? = null,
    val createChannelError: String? = null,
    // Roles & Guild Settings
    val roles: List<Role> = emptyList(),
    val isLoadingRoles: Boolean = false,
    val showGuildSettingsDialog: Boolean = false,
    val selectedRoleForEdit: Role? = null,
    val roleActionError: String? = null,
    val isSavingRole: Boolean = false,
    // Channel Overwrites & Channel Settings
    val channelOverwrites: List<ChannelOverwrite> = emptyList(),
    val isLoadingOverwrites: Boolean = false,
    val showChannelSettingsDialog: Boolean = false,
    val selectedChannelForEdit: Channel? = null,
    val overwriteActionError: String? = null,
    val isSavingOverwrite: Boolean = false,
    // Current user's assigned role IDs in selected guild
    val currentUserRoleIds: List<Long> = emptyList(),
    // Member Role Management
    val showMemberRolesDialog: Boolean = false,
    val selectedMemberForRoles: MemberData? = null,
    val memberRoles: Map<String, List<Long>> = emptyMap(),
    val isModifyingMemberRole: Boolean = false,
    val memberRoleActionError: String? = null,
) {
    val canManageRoles: Boolean
        get() =
            selectedGuild?.let { guild ->
                PermissionFlags.hasPermission(
                    PermissionFlags.computeGuildPermissions(currentUserId, guild.id, roles, currentUserRoleIds),
                    PermissionFlags.MANAGE_ROLES,
                )
            } ?: false

    val canManageChannels: Boolean
        get() =
            selectedGuild?.let { guild ->
                PermissionFlags.hasPermission(
                    PermissionFlags.computeGuildPermissions(currentUserId, guild.id, roles, currentUserRoleIds),
                    PermissionFlags.MANAGE_CHANNELS,
                )
            } ?: false

    val canSendInSelectedChannel: Boolean
        get() =
            if (selectedGuild == null || selectedChannel == null) {
                true
            } else {
                PermissionFlags.hasPermission(
                    PermissionFlags.computeChannelPermissions(
                        userId = currentUserId,
                        guildOwnerId = selectedGuild.id,
                        guildRoles = roles,
                        userRoleIds = currentUserRoleIds,
                        overwrites = channelOverwrites,
                    ),
                    PermissionFlags.SEND_MESSAGES,
                )
            }
}
