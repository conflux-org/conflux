package io.github.conflux_org.conflux.features.main.presentation

import io.github.conflux_org.conflux.core.ui.components.MemberData
import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType
import io.github.conflux_org.conflux.domain.model.Role

sealed interface MainIntent {
    data class LoadInitialData(
        val userId: Long,
    ) : MainIntent

    data class SelectGuild(
        val guild: Guild,
    ) : MainIntent

    data class SelectChannel(
        val channel: Channel,
    ) : MainIntent

    data class SendMessage(
        val content: String,
    ) : MainIntent

    data class CreateGuild(
        val name: String,
    ) : MainIntent

    data class CreateChannel(
        val guildId: Long,
        val name: String,
    ) : MainIntent

    data class ShowCreateGuildDialog(
        val show: Boolean,
    ) : MainIntent

    data class ShowCreateChannelDialog(
        val show: Boolean,
    ) : MainIntent

    // Roles & Guild Settings
    data class ShowGuildSettingsDialog(
        val show: Boolean,
    ) : MainIntent

    data class SelectRoleForEdit(
        val role: Role?,
    ) : MainIntent

    data class CreateRole(
        val guildId: Long,
        val name: String,
        val permissions: Long = 0L,
    ) : MainIntent

    data class UpdateRole(
        val guildId: Long,
        val roleId: Long,
        val name: String? = null,
        val permissions: Long? = null,
    ) : MainIntent

    data class DeleteRole(
        val guildId: Long,
        val roleId: Long,
    ) : MainIntent

    // Channel Overwrites & Channel Settings
    data class ShowChannelSettingsDialog(
        val show: Boolean,
        val channel: Channel? = null,
    ) : MainIntent

    data class SetChannelOverwrite(
        val channelId: Long,
        val targetType: OverwriteTargetType,
        val targetId: Long,
        val allow: Long,
        val deny: Long,
    ) : MainIntent

    data class DeleteChannelOverwrite(
        val channelId: Long,
        val targetType: OverwriteTargetType,
        val targetId: Long,
    ) : MainIntent

    // Member Role Management
    data class ShowMemberRolesDialog(
        val show: Boolean,
        val member: MemberData? = null,
    ) : MainIntent

    data class AssignMemberRole(
        val guildId: Long,
        val userId: Long,
        val roleId: Long,
    ) : MainIntent

    data class RemoveMemberRole(
        val guildId: Long,
        val userId: Long,
        val roleId: Long,
    ) : MainIntent
}
