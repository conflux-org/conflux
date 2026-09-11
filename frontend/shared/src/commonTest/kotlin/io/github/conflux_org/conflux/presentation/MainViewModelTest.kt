package io.github.conflux_org.conflux.presentation

import io.github.conflux_org.conflux.core.ui.components.MemberData
import io.github.conflux_org.conflux.data.FakeChannelOverwriteRepository
import io.github.conflux_org.conflux.data.FakeChannelRepository
import io.github.conflux_org.conflux.data.FakeGuildRepository
import io.github.conflux_org.conflux.data.FakeMessageRepository
import io.github.conflux_org.conflux.data.FakeRoleRepository
import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.ChannelOverwrite
import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.model.Message
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType
import io.github.conflux_org.conflux.domain.model.PermissionFlags
import io.github.conflux_org.conflux.domain.model.User
import io.github.conflux_org.conflux.features.main.presentation.MainIntent
import io.github.conflux_org.conflux.features.main.presentation.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        fakeGuildRepo: FakeGuildRepository = FakeGuildRepository(shouldSucceed = true),
        fakeChannelRepo: FakeChannelRepository = FakeChannelRepository(shouldSucceed = true),
        fakeMessageRepo: FakeMessageRepository = FakeMessageRepository(shouldSucceed = true),
        fakeRoleRepo: FakeRoleRepository = FakeRoleRepository(shouldSucceed = true),
        fakeChannelOverwriteRepo: FakeChannelOverwriteRepository = FakeChannelOverwriteRepository(shouldSucceed = true),
    ): MainViewModel =
        MainViewModel(
            mainDispatcher = testDispatcher,
            guildRepository = fakeGuildRepo,
            channelRepository = fakeChannelRepo,
            messageRepository = fakeMessageRepo,
            roleRepository = fakeRoleRepo,
            channelOverwriteRepository = fakeChannelOverwriteRepo,
        )

    @Test
    fun loadInitialData_withGuildsChannelsMessages_cascadesAndSelectsFirstItems() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 42L))

            val state = viewModel.uiState.value
            assertEquals(42L, state.currentUserId)
            assertEquals(2, state.guilds.size)
            assertEquals(Guild(1L, "General Guild"), state.selectedGuild)
            assertEquals(2, state.channels.size)
            assertEquals(Channel(101L, "general"), state.selectedChannel)
            assertEquals(2, state.messages.size)
            assertEquals(2, state.roles.size)
            assertFalse(state.isLoadingGuilds)
            assertFalse(state.isLoadingChannels)
            assertFalse(state.isLoadingMessages)
            assertNull(state.errorMessage)
        }

    @Test
    fun loadInitialData_withEmptyGuilds_doesNotLoadChannelsOrMessages() =
        runTest {
            val viewModel =
                createViewModel(
                    fakeGuildRepo = FakeGuildRepository(shouldSucceed = true, mockGuilds = emptyList()),
                )

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            val state = viewModel.uiState.value
            assertTrue(state.guilds.isEmpty())
            assertNull(state.selectedGuild)
            assertTrue(state.channels.isEmpty())
            assertNull(state.selectedChannel)
            assertTrue(state.messages.isEmpty())
            assertFalse(state.isLoadingGuilds)
            assertNull(state.errorMessage)
        }

    @Test
    fun loadInitialData_guildRepoFailure_setsErrorMessage() =
        runTest {
            val viewModel =
                createViewModel(
                    fakeGuildRepo =
                        FakeGuildRepository(shouldSucceed = false, errorMessage = "無法載入伺服器"),
                )

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            val state = viewModel.uiState.value
            assertEquals("無法載入伺服器", state.errorMessage)
            assertTrue(state.guilds.isEmpty())
            assertNull(state.selectedGuild)
            assertFalse(state.isLoadingGuilds)
        }

    @Test
    fun selectGuild_success_updatesSelectedGuild_andLoadsChannelsAndFirstChannelMessages() =
        runTest {
            val customChannels =
                listOf(
                    Channel(id = 201L, name = "gaming-chat"),
                    Channel(id = 202L, name = "gaming-voice"),
                )
            val customMessages =
                listOf(
                    Message(
                        id = 3001L,
                        author = User(id = 3L, name = "Gamer"),
                        content = "GG WP",
                    ),
                )
            val viewModel =
                createViewModel(
                    fakeChannelRepo =
                        FakeChannelRepository(shouldSucceed = true, mockChannels = customChannels),
                    fakeMessageRepo =
                        FakeMessageRepository(shouldSucceed = true, mockMessages = customMessages),
                )

            val targetGuild = Guild(id = 2L, name = "Gaming Hub")
            viewModel.handleIntent(MainIntent.SelectGuild(targetGuild))

            val state = viewModel.uiState.value
            assertEquals(targetGuild, state.selectedGuild)
            assertEquals(2, state.channels.size)
            assertEquals(Channel(201L, "gaming-chat"), state.selectedChannel)
            assertEquals(1, state.messages.size)
            assertEquals("GG WP", state.messages.first().content)
            assertFalse(state.isLoadingChannels)
            assertFalse(state.isLoadingMessages)
            assertNull(state.errorMessage)
        }

    @Test
    fun selectGuild_channelRepoFailure_setsErrorMessage() =
        runTest {
            val viewModel =
                createViewModel(
                    fakeChannelRepo =
                        FakeChannelRepository(shouldSucceed = false, errorMessage = "無法載入頻道列表"),
                )

            val targetGuild = Guild(id = 2L, name = "Gaming Hub")
            viewModel.handleIntent(MainIntent.SelectGuild(targetGuild))

            val state = viewModel.uiState.value
            assertEquals(targetGuild, state.selectedGuild)
            assertEquals("無法載入頻道列表", state.errorMessage)
            assertTrue(state.channels.isEmpty())
            assertNull(state.selectedChannel)
            assertFalse(state.isLoadingChannels)
        }

    @Test
    fun selectChannel_success_updatesSelectedChannel_andLoadsMessages() =
        runTest {
            val customMessages =
                listOf(
                    Message(
                        id = 5001L,
                        author = User(id = 10L, name = "Announcer"),
                        content = "Maintenance notice",
                    ),
                )
            val viewModel =
                createViewModel(
                    fakeMessageRepo =
                        FakeMessageRepository(shouldSucceed = true, mockMessages = customMessages),
                )

            val targetChannel = Channel(id = 102L, name = "announcements")
            viewModel.handleIntent(MainIntent.SelectChannel(targetChannel))

            val state = viewModel.uiState.value
            assertEquals(targetChannel, state.selectedChannel)
            assertEquals(1, state.messages.size)
            assertEquals("Maintenance notice", state.messages.first().content)
            assertFalse(state.isLoadingMessages)
            assertNull(state.errorMessage)
        }

    @Test
    fun selectChannel_messageRepoFailure_setsErrorMessage() =
        runTest {
            val viewModel =
                createViewModel(
                    fakeMessageRepo =
                        FakeMessageRepository(shouldSucceed = false, errorMessage = "無法載入頻道訊息"),
                )

            val targetChannel = Channel(id = 101L, name = "general")
            viewModel.handleIntent(MainIntent.SelectChannel(targetChannel))

            val state = viewModel.uiState.value
            assertEquals(targetChannel, state.selectedChannel)
            assertEquals("無法載入頻道訊息", state.errorMessage)
            assertTrue(state.messages.isEmpty())
            assertFalse(state.isLoadingMessages)
        }

    @Test
    fun sendMessage_addsServerMessageToMessagesList() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 77L))
            val initialCount = viewModel.uiState.value.messages.size

            val newMessageContent = "This is a new test message from user!"
            viewModel.handleIntent(MainIntent.SendMessage(newMessageContent))

            val state = viewModel.uiState.value
            assertEquals(initialCount + 1, state.messages.size)
            val lastMessage = state.messages.last()
            assertEquals(newMessageContent, lastMessage.content)
            assertEquals(1L, lastMessage.author.id)
        }

    @Test
    fun sendMessage_failure_setsErrorMessageWithoutAddingMessage() =
        runTest {
            val viewModel =
                createViewModel(
                    fakeMessageRepo = FakeMessageRepository(shouldSucceed = false, errorMessage = "無法發送訊息"),
                )

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 77L))
            val initialCount = viewModel.uiState.value.messages.size
            viewModel.handleIntent(MainIntent.SendMessage("failed"))

            assertEquals(initialCount, viewModel.uiState.value.messages.size)
            assertEquals("無法發送訊息", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun sendMessage_blankContent_setsErrorWithoutAddingMessage() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 77L))
            val initialCount = viewModel.uiState.value.messages.size
            viewModel.handleIntent(MainIntent.SendMessage("   \n\t"))

            assertEquals(initialCount, viewModel.uiState.value.messages.size)
            assertEquals("訊息內容不可為空白", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun showCreateGuildDialog_updatesState() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.ShowCreateGuildDialog(true))
            assertTrue(viewModel.uiState.value.showCreateGuildDialog)

            viewModel.handleIntent(MainIntent.ShowCreateGuildDialog(false))
            assertFalse(viewModel.uiState.value.showCreateGuildDialog)
        }

    @Test
    fun showCreateChannelDialog_updatesState() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.ShowCreateChannelDialog(true))
            assertTrue(viewModel.uiState.value.showCreateChannelDialog)

            viewModel.handleIntent(MainIntent.ShowCreateChannelDialog(false))
            assertFalse(viewModel.uiState.value.showCreateChannelDialog)
        }

    @Test
    fun createGuild_success_appendsGuild_selectsIt_andClosesDialog() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))
            viewModel.handleIntent(MainIntent.ShowCreateGuildDialog(true))

            viewModel.handleIntent(MainIntent.CreateGuild("New Awesome Server"))

            val state = viewModel.uiState.value
            assertEquals(3, state.guilds.size)
            assertEquals("New Awesome Server", state.guilds.last().name)
            assertEquals(state.guilds.last(), state.selectedGuild)
            assertFalse(state.showCreateGuildDialog)
            assertNull(state.createGuildError)
            assertFalse(state.isCreatingGuild)
        }

    @Test
    fun createGuild_failure_setsCreateGuildError_andKeepsDialogOpen() =
        runTest {
            val viewModel =
                createViewModel(
                    fakeGuildRepo =
                        FakeGuildRepository(shouldSucceed = false, errorMessage = "伺服器名稱已被使用"),
                )

            viewModel.handleIntent(MainIntent.ShowCreateGuildDialog(true))
            viewModel.handleIntent(MainIntent.CreateGuild("Existing Server"))

            val state = viewModel.uiState.value
            assertEquals("伺服器名稱已被使用", state.createGuildError)
            assertTrue(state.showCreateGuildDialog)
            assertFalse(state.isCreatingGuild)
        }

    @Test
    fun createGuild_emptyName_setsError_doesNotCallRepo() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.ShowCreateGuildDialog(true))
            viewModel.handleIntent(MainIntent.CreateGuild("   "))

            val state = viewModel.uiState.value
            assertEquals("伺服器名稱不能為空", state.createGuildError)
            assertTrue(state.showCreateGuildDialog)
            assertFalse(state.isCreatingGuild)
        }

    @Test
    fun createChannel_success_appendsChannel_selectsIt_andClosesDialog() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))
            viewModel.handleIntent(MainIntent.ShowCreateChannelDialog(true))

            viewModel.handleIntent(MainIntent.CreateChannel(guildId = 1L, name = "dev-talk"))

            val state = viewModel.uiState.value
            assertEquals(3, state.channels.size)
            assertEquals("dev-talk", state.channels.last().name)
            assertEquals(state.channels.last(), state.selectedChannel)
            assertFalse(state.showCreateChannelDialog)
            assertNull(state.createChannelError)
            assertFalse(state.isCreatingChannel)
        }

    @Test
    fun createChannel_failure_setsCreateChannelError_andKeepsDialogOpen() =
        runTest {
            val viewModel =
                createViewModel(
                    fakeChannelRepo =
                        FakeChannelRepository(shouldSucceed = false, errorMessage = "頻道名稱已存在"),
                )

            viewModel.handleIntent(MainIntent.ShowCreateChannelDialog(true))
            viewModel.handleIntent(MainIntent.CreateChannel(guildId = 1L, name = "general"))

            val state = viewModel.uiState.value
            assertEquals("頻道名稱已存在", state.createChannelError)
            assertTrue(state.showCreateChannelDialog)
            assertFalse(state.isCreatingChannel)
        }

    @Test
    fun createChannel_emptyName_setsError_doesNotCallRepo() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleIntent(MainIntent.ShowCreateChannelDialog(true))
            viewModel.handleIntent(MainIntent.CreateChannel(guildId = 1L, name = ""))

            val state = viewModel.uiState.value
            assertEquals("頻道名稱不能為空", state.createChannelError)
            assertTrue(state.showCreateChannelDialog)
            assertFalse(state.isCreatingChannel)
        }

    // --- Role & Guild Settings Tests ---

    @Test
    fun showGuildSettingsDialog_togglesStateAndSetsDefaultSelectedRole() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            viewModel.handleIntent(MainIntent.ShowGuildSettingsDialog(true))
            assertTrue(viewModel.uiState.value.showGuildSettingsDialog)
            assertEquals(
                "@everyone",
                viewModel.uiState.value.selectedRoleForEdit
                    ?.name,
            )

            viewModel.handleIntent(MainIntent.ShowGuildSettingsDialog(false))
            assertFalse(viewModel.uiState.value.showGuildSettingsDialog)
            assertNull(viewModel.uiState.value.selectedRoleForEdit)
        }

    @Test
    fun createRole_success_appendsRoleAndSelectsIt() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            viewModel.handleIntent(
                MainIntent.CreateRole(
                    guildId = 1L,
                    name = "Moderator Tier 2",
                    permissions = PermissionFlags.MANAGE_MESSAGES,
                ),
            )

            val state = viewModel.uiState.value
            assertEquals(3, state.roles.size)
            assertEquals("Moderator Tier 2", state.roles.last().name)
            assertEquals(state.roles.last(), state.selectedRoleForEdit)
            assertNull(state.roleActionError)
        }

    @Test
    fun createRole_blankName_setsRoleActionError() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            viewModel.handleIntent(MainIntent.CreateRole(guildId = 1L, name = "  "))

            val state = viewModel.uiState.value
            assertEquals("身分組名稱不能為空白", state.roleActionError)
        }

    @Test
    fun updateRole_success_updatesRoleInListAndSelectedRole() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            viewModel.handleIntent(
                MainIntent.UpdateRole(
                    guildId = 1L,
                    roleId = 2L,
                    name = "Lead Moderator",
                    permissions = PermissionFlags.MANAGE_CHANNELS or PermissionFlags.MANAGE_MESSAGES,
                ),
            )

            val state = viewModel.uiState.value
            val updated = state.roles.first { it.id == 2L }
            assertEquals("Lead Moderator", updated.name)
            assertEquals(updated, state.selectedRoleForEdit)
        }

    @Test
    fun deleteRole_success_removesRoleFromList() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            viewModel.handleIntent(MainIntent.DeleteRole(guildId = 1L, roleId = 2L))

            val state = viewModel.uiState.value
            assertEquals(1, state.roles.size)
            assertEquals("@everyone", state.roles.first().name)
            assertNull(state.roleActionError)
        }

    // --- Channel Overwrites & Settings Tests ---

    @Test
    fun showChannelSettingsDialog_loadsOverwritesForChannel() =
        runTest {
            val fakeOverwriteRepo =
                FakeChannelOverwriteRepository(
                    shouldSucceed = true,
                    mockOverwrites =
                        mutableListOf(
                            ChannelOverwrite(
                                id = 1L,
                                channelId = 101L,
                                targetType = OverwriteTargetType.ROLE,
                                targetId = 1L,
                                allow = PermissionFlags.VIEW_CHANNEL,
                                deny = PermissionFlags.SEND_MESSAGES,
                            ),
                        ),
                )
            val viewModel = createViewModel(fakeChannelOverwriteRepo = fakeOverwriteRepo)
            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            val channel = Channel(id = 101L, name = "general")
            viewModel.handleIntent(MainIntent.ShowChannelSettingsDialog(show = true, channel = channel))

            val state = viewModel.uiState.value
            assertTrue(state.showChannelSettingsDialog)
            assertEquals(channel, state.selectedChannelForEdit)
            assertEquals(1, state.channelOverwrites.size)
            assertEquals(1L, state.channelOverwrites.first().id)
        }

    @Test
    fun setChannelOverwrite_success_updatesOverwritesList() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            viewModel.handleIntent(
                MainIntent.SetChannelOverwrite(
                    channelId = 101L,
                    targetType = OverwriteTargetType.ROLE,
                    targetId = 2L,
                    allow = PermissionFlags.VIEW_CHANNEL or PermissionFlags.SEND_MESSAGES,
                    deny = 0L,
                ),
            )

            val state = viewModel.uiState.value
            val ow = state.channelOverwrites.find { it.channelId == 101L && it.targetId == 2L }
            assertTrue(ow != null)
            assertEquals(PermissionFlags.VIEW_CHANNEL or PermissionFlags.SEND_MESSAGES, ow.allow)
        }

    @Test
    fun deleteChannelOverwrite_success_removesFromOverwritesList() =
        runTest {
            val fakeOverwriteRepo =
                FakeChannelOverwriteRepository(
                    shouldSucceed = true,
                    mockOverwrites =
                        mutableListOf(
                            ChannelOverwrite(
                                id = 1L,
                                channelId = 101L,
                                targetType = OverwriteTargetType.ROLE,
                                targetId = 1L,
                                allow = 0L,
                                deny = 32L,
                            ),
                        ),
                )
            val viewModel = createViewModel(fakeChannelOverwriteRepo = fakeOverwriteRepo)
            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 1L))

            viewModel.handleIntent(
                MainIntent.DeleteChannelOverwrite(
                    channelId = 101L,
                    targetType = OverwriteTargetType.ROLE,
                    targetId = 1L,
                ),
            )

            val state = viewModel.uiState.value
            assertTrue(state.channelOverwrites.isEmpty())
        }

    // --- Member Role Management Tests ---

    @Test
    fun showMemberRolesDialog_togglesStateAndSetsSelectedMember() =
        runTest {
            val viewModel = createViewModel()
            val sampleMember = MemberData(id = "1", name = "Alex")

            viewModel.handleIntent(MainIntent.ShowMemberRolesDialog(show = true, member = sampleMember))
            assertTrue(viewModel.uiState.value.showMemberRolesDialog)
            assertEquals(sampleMember, viewModel.uiState.value.selectedMemberForRoles)

            viewModel.handleIntent(MainIntent.ShowMemberRolesDialog(show = false))
            assertFalse(viewModel.uiState.value.showMemberRolesDialog)
            assertNull(viewModel.uiState.value.selectedMemberForRoles)
        }

    @Test
    fun assignMemberRole_success_callsRepoAndUpdatesState() =
        runTest {
            val fakeRoleRepo = FakeRoleRepository(shouldSucceed = true)
            val viewModel = createViewModel(fakeRoleRepo = fakeRoleRepo)
            val sampleMember = MemberData(id = "1", name = "Alex")
            viewModel.handleIntent(MainIntent.ShowMemberRolesDialog(show = true, member = sampleMember))

            viewModel.handleIntent(
                MainIntent.AssignMemberRole(
                    guildId = 1L,
                    userId = 1L,
                    roleId = 2L,
                ),
            )

            val state = viewModel.uiState.value
            assertEquals(listOf(2L), state.memberRoles["1"])
            assertEquals(1, fakeRoleRepo.assignedMemberRoles.size)
            assertEquals(Triple(1L, 1L, 2L), fakeRoleRepo.assignedMemberRoles.first())
            assertFalse(state.isModifyingMemberRole)
            assertNull(state.memberRoleActionError)
        }

    @Test
    fun removeMemberRole_success_callsRepoAndUpdatesState() =
        runTest {
            val fakeRoleRepo = FakeRoleRepository(shouldSucceed = true)
            fakeRoleRepo.assignedMemberRoles.add(Triple(1L, 1L, 2L))
            val viewModel = createViewModel(fakeRoleRepo = fakeRoleRepo)
            val sampleMember = MemberData(id = "1", name = "Alex", roleIds = listOf(2L))
            viewModel.handleIntent(MainIntent.ShowMemberRolesDialog(show = true, member = sampleMember))

            viewModel.handleIntent(
                MainIntent.RemoveMemberRole(
                    guildId = 1L,
                    userId = 1L,
                    roleId = 2L,
                ),
            )

            val state = viewModel.uiState.value
            assertEquals(emptyList(), state.memberRoles["1"])
            assertTrue(fakeRoleRepo.assignedMemberRoles.isEmpty())
            assertFalse(state.isModifyingMemberRole)
            assertNull(state.memberRoleActionError)
        }
}
