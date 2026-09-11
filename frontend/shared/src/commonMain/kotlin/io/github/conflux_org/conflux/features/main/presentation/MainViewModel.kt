package io.github.conflux_org.conflux.features.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.conflux_org.conflux.core.ui.components.MemberData
import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType
import io.github.conflux_org.conflux.domain.model.Role
import io.github.conflux_org.conflux.domain.repository.ChannelOverwriteRepository
import io.github.conflux_org.conflux.domain.repository.ChannelRepository
import io.github.conflux_org.conflux.domain.repository.GuildRepository
import io.github.conflux_org.conflux.domain.repository.MessageRepository
import io.github.conflux_org.conflux.domain.repository.RoleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val guildRepository: GuildRepository,
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val roleRepository: RoleRepository,
    private val channelOverwriteRepository: ChannelOverwriteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadInitialData -> loadInitialData(intent.userId)
            is MainIntent.SelectGuild -> selectGuild(intent.guild)
            is MainIntent.SelectChannel -> selectChannel(intent.channel)
            is MainIntent.SendMessage -> sendMessage(intent.content)
            is MainIntent.ShowCreateGuildDialog -> showCreateGuildDialog(intent.show)
            is MainIntent.ShowCreateChannelDialog -> showCreateChannelDialog(intent.show)
            is MainIntent.CreateGuild -> createGuild(intent.name)
            is MainIntent.CreateChannel -> createChannel(intent.guildId, intent.name)
            is MainIntent.ShowGuildSettingsDialog -> showGuildSettingsDialog(intent.show)
            is MainIntent.SelectRoleForEdit -> selectRoleForEdit(intent.role)
            is MainIntent.CreateRole -> createRole(intent.guildId, intent.name, intent.permissions)
            is MainIntent.UpdateRole -> updateRole(intent.guildId, intent.roleId, intent.name, intent.permissions)
            is MainIntent.DeleteRole -> deleteRole(intent.guildId, intent.roleId)
            is MainIntent.ShowChannelSettingsDialog -> showChannelSettingsDialog(intent.show, intent.channel)
            is MainIntent.SetChannelOverwrite ->
                setChannelOverwrite(
                    intent.channelId,
                    intent.targetType,
                    intent.targetId,
                    intent.allow,
                    intent.deny,
                )
            is MainIntent.DeleteChannelOverwrite ->
                deleteChannelOverwrite(
                    intent.channelId,
                    intent.targetType,
                    intent.targetId,
                )
            is MainIntent.ShowMemberRolesDialog -> showMemberRolesDialog(intent.show, intent.member)
            is MainIntent.AssignMemberRole -> assignMemberRole(intent.guildId, intent.userId, intent.roleId)
            is MainIntent.RemoveMemberRole -> removeMemberRole(intent.guildId, intent.userId, intent.roleId)
        }
    }

    private fun showCreateGuildDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showCreateGuildDialog = show,
                createGuildError = if (show) null else it.createGuildError,
            )
        }
    }

    private fun showCreateChannelDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showCreateChannelDialog = show,
                createChannelError = if (show) null else it.createChannelError,
            )
        }
    }

    private fun showGuildSettingsDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showGuildSettingsDialog = show,
                roleActionError = null,
                selectedRoleForEdit = if (show) it.selectedRoleForEdit ?: it.roles.firstOrNull() else null,
            )
        }
    }

    private fun selectRoleForEdit(role: Role?) {
        _uiState.update {
            it.copy(selectedRoleForEdit = role, roleActionError = null)
        }
    }

    private fun createRole(
        guildId: Long,
        name: String,
        permissions: Long,
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(roleActionError = "身分組名稱不能為空白") }
            return
        }
        _uiState.update { it.copy(isSavingRole = true, roleActionError = null) }
        viewModelScope.launch(mainDispatcher) {
            roleRepository
                .createRole(guildId = guildId, name = name.trim(), permissions = permissions)
                .onSuccess { newRole ->
                    _uiState.update {
                        it.copy(
                            roles = it.roles + newRole,
                            selectedRoleForEdit = newRole,
                            isSavingRole = false,
                            roleActionError = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingRole = false,
                            roleActionError = error.message ?: "建立身分組失敗",
                        )
                    }
                }
        }
    }

    private fun updateRole(
        guildId: Long,
        roleId: Long,
        name: String?,
        permissions: Long?,
    ) {
        _uiState.update { it.copy(isSavingRole = true, roleActionError = null) }
        viewModelScope.launch(mainDispatcher) {
            roleRepository
                .updateRole(guildId = guildId, roleId = roleId, name = name, permissions = permissions)
                .onSuccess { updatedRole ->
                    _uiState.update { state ->
                        val updatedRoles = state.roles.map { if (it.id == roleId) updatedRole else it }
                        state.copy(
                            roles = updatedRoles,
                            selectedRoleForEdit = updatedRole,
                            isSavingRole = false,
                            roleActionError = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingRole = false,
                            roleActionError = error.message ?: "更新身分組失敗",
                        )
                    }
                }
        }
    }

    private fun deleteRole(
        guildId: Long,
        roleId: Long,
    ) {
        _uiState.update { it.copy(isSavingRole = true, roleActionError = null) }
        viewModelScope.launch(mainDispatcher) {
            roleRepository
                .deleteRole(guildId = guildId, roleId = roleId)
                .onSuccess {
                    _uiState.update {
                        val remainingRoles = it.roles.filter { r -> r.id != roleId }
                        it.copy(
                            roles = remainingRoles,
                            selectedRoleForEdit = remainingRoles.firstOrNull(),
                            isSavingRole = false,
                            roleActionError = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingRole = false,
                            roleActionError = error.message ?: "刪除身分組失敗",
                        )
                    }
                }
        }
    }

    private fun showChannelSettingsDialog(
        show: Boolean,
        channel: Channel?,
    ) {
        val targetChannel = channel ?: _uiState.value.selectedChannel
        _uiState.update {
            it.copy(
                showChannelSettingsDialog = show,
                selectedChannelForEdit = if (show) targetChannel else null,
                overwriteActionError = null,
            )
        }
        if (show && targetChannel != null) {
            loadChannelOverwrites(targetChannel.id)
        }
    }

    private fun loadChannelOverwrites(channelId: Long) {
        _uiState.update { it.copy(isLoadingOverwrites = true) }
        viewModelScope.launch(mainDispatcher) {
            channelOverwriteRepository
                .getChannelOverwrites(channelId)
                .onSuccess { overwrites ->
                    _uiState.update {
                        it.copy(channelOverwrites = overwrites, isLoadingOverwrites = false)
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingOverwrites = false,
                            overwriteActionError = error.message ?: "載入頻道覆寫失敗",
                        )
                    }
                }
        }
    }

    private fun setChannelOverwrite(
        channelId: Long,
        targetType: OverwriteTargetType,
        targetId: Long,
        allow: Long,
        deny: Long,
    ) {
        _uiState.update { it.copy(isSavingOverwrite = true, overwriteActionError = null) }
        viewModelScope.launch(mainDispatcher) {
            channelOverwriteRepository
                .setChannelOverwrite(channelId, targetType, targetId, allow, deny)
                .onSuccess { overwrite ->
                    _uiState.update { state ->
                        val index =
                            state.channelOverwrites.indexOfFirst { ow ->
                                ow.channelId == channelId && ow.targetType == targetType && ow.targetId == targetId
                            }
                        val updatedList =
                            if (index != -1) {
                                state.channelOverwrites.mapIndexed { i, ow -> if (i == index) overwrite else ow }
                            } else {
                                state.channelOverwrites + overwrite
                            }
                        state.copy(
                            channelOverwrites = updatedList,
                            isSavingOverwrite = false,
                            overwriteActionError = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingOverwrite = false,
                            overwriteActionError = error.message ?: "設定頻道覆寫失敗",
                        )
                    }
                }
        }
    }

    private fun deleteChannelOverwrite(
        channelId: Long,
        targetType: OverwriteTargetType,
        targetId: Long,
    ) {
        _uiState.update { it.copy(isSavingOverwrite = true, overwriteActionError = null) }
        viewModelScope.launch(mainDispatcher) {
            channelOverwriteRepository
                .deleteChannelOverwrite(channelId, targetType, targetId)
                .onSuccess {
                    _uiState.update { state ->
                        val remaining =
                            state.channelOverwrites.filterNot { ow ->
                                ow.channelId == channelId && ow.targetType == targetType && ow.targetId == targetId
                            }
                        state.copy(
                            channelOverwrites = remaining,
                            isSavingOverwrite = false,
                            overwriteActionError = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingOverwrite = false,
                            overwriteActionError = error.message ?: "刪除頻道覆寫失敗",
                        )
                    }
                }
        }
    }

    private fun showMemberRolesDialog(
        show: Boolean,
        member: MemberData?,
    ) {
        _uiState.update {
            it.copy(
                showMemberRolesDialog = show,
                selectedMemberForRoles = if (show) member else null,
                memberRoleActionError = null,
            )
        }
    }

    private fun assignMemberRole(
        guildId: Long,
        userId: Long,
        roleId: Long,
    ) {
        _uiState.update { it.copy(isModifyingMemberRole = true, memberRoleActionError = null) }
        viewModelScope.launch(mainDispatcher) {
            roleRepository
                .assignMemberRole(guildId = guildId, userId = userId, roleId = roleId)
                .onSuccess {
                    _uiState.update { state ->
                        val current =
                            state.memberRoles[userId.toString()]
                                ?: state.selectedMemberForRoles
                                    ?.takeIf { m -> m.id == userId.toString() }
                                    ?.roleIds
                                    .orEmpty()
                        val updated = (current + roleId).distinct()
                        state.copy(
                            memberRoles = state.memberRoles + (userId.toString() to updated),
                            isModifyingMemberRole = false,
                            memberRoleActionError = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isModifyingMemberRole = false,
                            memberRoleActionError = error.message ?: "指派身分組失敗",
                        )
                    }
                }
        }
    }

    private fun removeMemberRole(
        guildId: Long,
        userId: Long,
        roleId: Long,
    ) {
        _uiState.update { it.copy(isModifyingMemberRole = true, memberRoleActionError = null) }
        viewModelScope.launch(mainDispatcher) {
            roleRepository
                .removeMemberRole(guildId = guildId, userId = userId, roleId = roleId)
                .onSuccess {
                    _uiState.update { state ->
                        val current =
                            state.memberRoles[userId.toString()]
                                ?: state.selectedMemberForRoles
                                    ?.takeIf { m -> m.id == userId.toString() }
                                    ?.roleIds
                                    .orEmpty()
                        val updated = current.filterNot { it == roleId }
                        state.copy(
                            memberRoles = state.memberRoles + (userId.toString() to updated),
                            isModifyingMemberRole = false,
                            memberRoleActionError = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isModifyingMemberRole = false,
                            memberRoleActionError = error.message ?: "移除身分組失敗",
                        )
                    }
                }
        }
    }

    private fun createGuild(name: String) {
        if (name.isBlank()) {
            _uiState.update {
                it.copy(createGuildError = "伺服器名稱不能為空")
            }
            return
        }

        _uiState.update {
            it.copy(
                isCreatingGuild = true,
                createGuildError = null,
            )
        }

        viewModelScope.launch(mainDispatcher) {
            guildRepository
                .createGuild(name.trim())
                .onSuccess { createdGuild ->
                    _uiState.update {
                        it.copy(
                            guilds = it.guilds + createdGuild,
                            isCreatingGuild = false,
                            showCreateGuildDialog = false,
                            createGuildError = null,
                        )
                    }
                    selectGuild(createdGuild)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingGuild = false,
                            createGuildError = error.message ?: "建立伺服器失敗",
                        )
                    }
                }
        }
    }

    private fun createChannel(
        guildId: Long,
        name: String,
    ) {
        if (name.isBlank()) {
            _uiState.update {
                it.copy(createChannelError = "頻道名稱不能為空")
            }
            return
        }

        _uiState.update {
            it.copy(
                isCreatingChannel = true,
                createChannelError = null,
            )
        }

        viewModelScope.launch(mainDispatcher) {
            channelRepository
                .createChannel(guildId, name.trim())
                .onSuccess { createdChannel ->
                    _uiState.update {
                        it.copy(
                            channels = it.channels + createdChannel,
                            isCreatingChannel = false,
                            showCreateChannelDialog = false,
                            createChannelError = null,
                        )
                    }
                    selectChannel(createdChannel)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingChannel = false,
                            createChannelError = error.message ?: "建立頻道失敗",
                        )
                    }
                }
        }
    }

    private fun loadInitialData(userId: Long) {
        _uiState.update {
            it.copy(
                isLoadingGuilds = true,
                currentUserId = userId,
                errorMessage = null,
            )
        }
        viewModelScope.launch(mainDispatcher) {
            guildRepository
                .getGuildsByUserId(userId)
                .onSuccess { guilds ->
                    _uiState.update { it.copy(guilds = guilds, isLoadingGuilds = false) }
                    if (guilds.isNotEmpty()) {
                        selectGuild(guilds.first())
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingGuilds = false,
                            errorMessage = error.message ?: "載入伺服器失敗",
                        )
                    }
                }
        }
    }

    private fun selectGuild(guild: Guild) {
        _uiState.update {
            it.copy(
                selectedGuild = guild,
                isLoadingChannels = true,
                isLoadingRoles = true,
                errorMessage = null,
                channels = emptyList(),
                selectedChannel = null,
                messages = emptyList(),
                roles = emptyList(),
                selectedRoleForEdit = null,
            )
        }
        viewModelScope.launch(mainDispatcher) {
            channelRepository
                .getChannelsByGuildId(guild.id)
                .onSuccess { channels ->
                    _uiState.update { it.copy(channels = channels, isLoadingChannels = false) }
                    if (channels.isNotEmpty()) {
                        selectChannel(channels.first())
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingChannels = false,
                            errorMessage = error.message ?: "載入頻道失敗",
                        )
                    }
                }
        }

        viewModelScope.launch(mainDispatcher) {
            roleRepository
                .getGuildRoles(guild.id)
                .onSuccess { roles ->
                    _uiState.update {
                        it.copy(
                            roles = roles,
                            isLoadingRoles = false,
                            selectedRoleForEdit = roles.firstOrNull { r -> r.isEveryone } ?: roles.firstOrNull(),
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingRoles = false,
                            roleActionError = error.message ?: "載入身分組失敗",
                        )
                    }
                }
        }
    }

    private fun selectChannel(channel: Channel) {
        _uiState.update {
            it.copy(
                selectedChannel = channel,
                isLoadingMessages = true,
                errorMessage = null,
                messages = emptyList(),
            )
        }
        viewModelScope.launch(mainDispatcher) {
            messageRepository
                .getMessagesByChannelId(channel.id)
                .onSuccess { messages ->
                    _uiState.update { it.copy(messages = messages, isLoadingMessages = false) }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMessages = false,
                            errorMessage = error.message ?: "載入訊息失敗",
                        )
                    }
                }
        }
    }

    private fun sendMessage(content: String) {
        val currentChannel = _uiState.value.selectedChannel
        if (currentChannel == null) {
            _uiState.update { it.copy(errorMessage = "尚未選擇頻道，無法發送訊息") }
            return
        }

        if (content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "訊息內容不可為空白") }
            return
        }

        viewModelScope.launch(mainDispatcher) {
            messageRepository
                .sendMessage(currentChannel.id, content.trim())
                .onSuccess { newMessage ->
                    _uiState.update { it.copy(messages = it.messages + newMessage) }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "發送訊息失敗")
                    }
                }
        }
    }
}
