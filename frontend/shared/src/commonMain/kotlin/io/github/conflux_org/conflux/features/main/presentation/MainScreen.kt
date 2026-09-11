package io.github.conflux_org.conflux.features.main.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.conflux_org.conflux.core.ui.components.ChannelSettingsDialog
import io.github.conflux_org.conflux.core.ui.components.ChannelStatus
import io.github.conflux_org.conflux.core.ui.components.CreateChannelDialog
import io.github.conflux_org.conflux.core.ui.components.CreateGuildDialog
import io.github.conflux_org.conflux.core.ui.components.GuildSettingsDialog
import io.github.conflux_org.conflux.core.ui.components.MemberCategoryData
import io.github.conflux_org.conflux.core.ui.components.MemberData
import io.github.conflux_org.conflux.core.ui.components.MemberSidebar
import io.github.conflux_org.conflux.core.ui.components.MessageArea
import io.github.conflux_org.conflux.core.ui.components.MessageData
import io.github.conflux_org.conflux.core.ui.components.Sidebar
import io.github.conflux_org.conflux.core.ui.components.TextChannelItem
import io.github.conflux_org.conflux.core.ui.components.UserStatus
import org.koin.compose.viewmodel.koinViewModel

/**
 * Conflux 主畫面 - 組合所有 Discord 風格組件 (Guild Sidebar, Channel List, Message Feed, Member Sidebar)
 */
@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val sampleCategories =
        remember {
            listOf(
                MemberCategoryData(
                    roleName = "管理員",
                    members =
                        listOf(
                            MemberData(
                                id = "m1",
                                name = "Alex (Owner)",
                                nameColor = Color(0xFFF1C40F),
                                avatarColor = Color(0xFFE91E63),
                                status = UserStatus.Online,
                                customStatus = "Coding KMP UI",
                            ),
                            MemberData(
                                id = "m2",
                                name = "ConfluxBot",
                                avatarColor = Color(0xFF5865F2),
                                status = UserStatus.Online,
                                isBot = true,
                            ),
                        ),
                ),
                MemberCategoryData(
                    roleName = "線上成員",
                    members =
                        listOf(
                            MemberData(
                                id = "m3",
                                name = "Taylor",
                                avatarColor = Color(0xFF2ECC71),
                                status = UserStatus.Idle,
                                customStatus = "AFK - Getting coffee",
                            ),
                            MemberData(
                                id = "m4",
                                name = "Jordan",
                                avatarColor = Color(0xFF9B59B6),
                                status = UserStatus.Dnd,
                                customStatus = "Do Not Disturb / Busy",
                            ),
                        ),
                ),
                MemberCategoryData(
                    roleName = "離線成員",
                    members =
                        listOf(
                            MemberData(
                                id = "m5",
                                name = "Morgan",
                                avatarColor = Color(0xFF95A5A6),
                                status = UserStatus.Offline,
                            ),
                        ),
                ),
            )
        }

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1F22)),
    ) {
        // 1. 最左側伺服器 Guild 側邊欄 (寬度 72.dp)
        Sidebar(
            guilds = uiState.guilds,
            selectedGuildId = uiState.selectedGuild?.id,
            onGuildClick = { guild ->
                viewModel.handleIntent(MainIntent.SelectGuild(guild))
            },
            onAddGuildClick = {
                viewModel.handleIntent(MainIntent.ShowCreateGuildDialog(true))
            },
        )

        // 2. 頻道列表欄 (寬度 240.dp)
        Column(
            modifier =
                Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2B2D31)),
        ) {
            // 伺服器名稱 Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = uiState.selectedGuild?.name.orEmpty(),
                    color = Color(0xFFF2F3F5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                if (uiState.selectedGuild != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.handleIntent(MainIntent.ShowGuildSettingsDialog(true))
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "伺服器身分組設定",
                                tint = Color(0xFFB5BAC1),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        if (uiState.canManageChannels) {
                            IconButton(
                                onClick = {
                                    viewModel.handleIntent(MainIntent.ShowCreateChannelDialog(true))
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "新增頻道",
                                    tint = Color(0xFFB5BAC1),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFF1F2023),
            )

            // 頻道清單
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                items(uiState.channels, key = { it.id }) { channel ->
                    val status =
                        if (channel.id == uiState.selectedChannel?.id) {
                            ChannelStatus.Selected
                        } else {
                            ChannelStatus.Idle
                        }

                    val onSettingsClick: (() -> Unit)? =
                        if (uiState.canManageChannels) {
                            {
                                viewModel.handleIntent(
                                    MainIntent.ShowChannelSettingsDialog(show = true, channel = channel),
                                )
                            }
                        } else {
                            null
                        }

                    TextChannelItem(
                        name = channel.name,
                        status = status,
                        onSettingsClick = onSettingsClick,
                        onClick = { viewModel.handleIntent(MainIntent.SelectChannel(channel)) },
                    )
                }
            }
        }

        // 3. 中央訊息區塊 (權重 1f 佔滿剩餘寬度)
        val displayMessages =
            uiState.messages.map { msg ->
                MessageData(
                    id = msg.id.toString(),
                    senderName = msg.author.name,
                    avatarColor = Color(0xFF5865F2),
                    timestamp = "今天",
                    content = msg.content,
                )
            }

        MessageArea(
            channelName = uiState.selectedChannel?.name.orEmpty(),
            messages = displayMessages,
            modifier = Modifier.weight(1f),
            canSendMessage = uiState.canSendInSelectedChannel,
            onSendMessage = { text ->
                viewModel.handleIntent(MainIntent.SendMessage(text))
            },
        )

        // 4. 右側成員側邊欄 (寬度 240.dp)
        MemberSidebar(
            categories = sampleCategories,
        )
    }

    // 對話框彈窗
    CreateGuildDialog(
        show = uiState.showCreateGuildDialog,
        isLoading = uiState.isCreatingGuild,
        errorMessage = uiState.createGuildError,
        onDismiss = { viewModel.handleIntent(MainIntent.ShowCreateGuildDialog(false)) },
        onCreateGuild = { name -> viewModel.handleIntent(MainIntent.CreateGuild(name)) },
    )

    CreateChannelDialog(
        show = uiState.showCreateChannelDialog,
        isLoading = uiState.isCreatingChannel,
        errorMessage = uiState.createChannelError,
        onDismiss = { viewModel.handleIntent(MainIntent.ShowCreateChannelDialog(false)) },
        onCreateChannel = { name ->
            uiState.selectedGuild?.let { guild ->
                viewModel.handleIntent(MainIntent.CreateChannel(guild.id, name))
            }
        },
    )

    if (uiState.showGuildSettingsDialog) {
        GuildSettingsDialog(
            roles = uiState.roles,
            selectedRole = uiState.selectedRoleForEdit,
            isSaving = uiState.isSavingRole,
            errorMessage = uiState.roleActionError,
            canManageRoles = uiState.canManageRoles,
            onSelectRole = { role -> viewModel.handleIntent(MainIntent.SelectRoleForEdit(role)) },
            onCreateRole = { name ->
                uiState.selectedGuild?.let { guild ->
                    viewModel.handleIntent(MainIntent.CreateRole(guild.id, name))
                }
            },
            onUpdateRole = { roleId, name, permissions ->
                uiState.selectedGuild?.let { guild ->
                    viewModel.handleIntent(
                        MainIntent.UpdateRole(guild.id, roleId, name, permissions),
                    )
                }
            },
            onDeleteRole = { roleId ->
                uiState.selectedGuild?.let { guild ->
                    viewModel.handleIntent(MainIntent.DeleteRole(guild.id, roleId))
                }
            },
            onDismiss = { viewModel.handleIntent(MainIntent.ShowGuildSettingsDialog(false)) },
        )
    }

    if (uiState.showChannelSettingsDialog && (uiState.selectedChannelForEdit ?: uiState.selectedChannel) != null) {
        val channelToEdit = uiState.selectedChannelForEdit ?: uiState.selectedChannel!!
        ChannelSettingsDialog(
            channel = channelToEdit,
            roles = uiState.roles,
            overwrites = uiState.channelOverwrites,
            isLoading = uiState.isLoadingOverwrites,
            isSaving = uiState.isSavingOverwrite,
            errorMessage = uiState.overwriteActionError,
            canManageChannels = uiState.canManageChannels,
            onSetOverwrite = { targetType, targetId, allow, deny ->
                viewModel.handleIntent(
                    MainIntent.SetChannelOverwrite(channelToEdit.id, targetType, targetId, allow, deny),
                )
            },
            onDeleteOverwrite = { targetType, targetId ->
                viewModel.handleIntent(
                    MainIntent.DeleteChannelOverwrite(channelToEdit.id, targetType, targetId),
                )
            },
            onDismiss = { viewModel.handleIntent(MainIntent.ShowChannelSettingsDialog(false)) },
        )
    }
}

@Preview
@Composable
fun Preview() {
    MainScreen()
}
