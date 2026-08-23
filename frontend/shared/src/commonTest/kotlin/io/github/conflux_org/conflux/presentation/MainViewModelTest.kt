package io.github.conflux_org.conflux.presentation

import io.github.conflux_org.conflux.data.FakeChannelRepository
import io.github.conflux_org.conflux.data.FakeGuildRepository
import io.github.conflux_org.conflux.data.FakeMessageRepository
import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.model.Message
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

    @Test
    fun loadInitialData_withGuildsChannelsMessages_cascadesAndSelectsFirstItems() =
        runTest {
            val fakeGuildRepo = FakeGuildRepository(shouldSucceed = true)
            val fakeChannelRepo = FakeChannelRepository(shouldSucceed = true)
            val fakeMessageRepo = FakeMessageRepository(shouldSucceed = true)

            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
                )

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 42L))

            val state = viewModel.uiState.value
            assertEquals(42L, state.currentUserId)
            assertEquals(2, state.guilds.size)
            assertEquals(Guild(1L, "General Guild"), state.selectedGuild)
            assertEquals(2, state.channels.size)
            assertEquals(Channel(101L, "general"), state.selectedChannel)
            assertEquals(2, state.messages.size)
            assertFalse(state.isLoadingGuilds)
            assertFalse(state.isLoadingChannels)
            assertFalse(state.isLoadingMessages)
            assertNull(state.errorMessage)
        }

    @Test
    fun loadInitialData_withEmptyGuilds_doesNotLoadChannelsOrMessages() =
        runTest {
            val fakeGuildRepo = FakeGuildRepository(shouldSucceed = true, mockGuilds = emptyList())
            val fakeChannelRepo = FakeChannelRepository(shouldSucceed = true)
            val fakeMessageRepo = FakeMessageRepository(shouldSucceed = true)

            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
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
            val fakeGuildRepo =
                FakeGuildRepository(shouldSucceed = false, errorMessage = "無法載入伺服器")
            val fakeChannelRepo = FakeChannelRepository(shouldSucceed = true)
            val fakeMessageRepo = FakeMessageRepository(shouldSucceed = true)

            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
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
            val fakeGuildRepo = FakeGuildRepository(shouldSucceed = true)
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
            val fakeChannelRepo =
                FakeChannelRepository(shouldSucceed = true, mockChannels = customChannels)
            val fakeMessageRepo =
                FakeMessageRepository(shouldSucceed = true, mockMessages = customMessages)

            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
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
            val fakeGuildRepo = FakeGuildRepository(shouldSucceed = true)
            val fakeChannelRepo =
                FakeChannelRepository(shouldSucceed = false, errorMessage = "無法載入頻道列表")
            val fakeMessageRepo = FakeMessageRepository(shouldSucceed = true)

            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
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
            val fakeGuildRepo = FakeGuildRepository(shouldSucceed = true)
            val fakeChannelRepo = FakeChannelRepository(shouldSucceed = true)
            val customMessages =
                listOf(
                    Message(
                        id = 5001L,
                        author = User(id = 10L, name = "Announcer"),
                        content = "Maintenance notice",
                    ),
                )
            val fakeMessageRepo =
                FakeMessageRepository(shouldSucceed = true, mockMessages = customMessages)

            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
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
            val fakeGuildRepo = FakeGuildRepository(shouldSucceed = true)
            val fakeChannelRepo = FakeChannelRepository(shouldSucceed = true)
            val fakeMessageRepo =
                FakeMessageRepository(shouldSucceed = false, errorMessage = "無法載入頻道訊息")

            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
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
            val fakeGuildRepo = FakeGuildRepository(shouldSucceed = true)
            val fakeChannelRepo = FakeChannelRepository(shouldSucceed = true)
            val fakeMessageRepo = FakeMessageRepository(shouldSucceed = true)

            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
                )

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
            val fakeGuildRepo = FakeGuildRepository(shouldSucceed = true)
            val fakeChannelRepo = FakeChannelRepository(shouldSucceed = true)
            val fakeMessageRepo = FakeMessageRepository(shouldSucceed = false, errorMessage = "無法發送訊息")
            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = fakeGuildRepo,
                    channelRepository = fakeChannelRepo,
                    messageRepository = fakeMessageRepo,
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
            val viewModel =
                MainViewModel(
                    mainDispatcher = testDispatcher,
                    guildRepository = FakeGuildRepository(shouldSucceed = true),
                    channelRepository = FakeChannelRepository(shouldSucceed = true),
                    messageRepository = FakeMessageRepository(shouldSucceed = true),
                )

            viewModel.handleIntent(MainIntent.LoadInitialData(userId = 77L))
            val initialCount = viewModel.uiState.value.messages.size
            viewModel.handleIntent(MainIntent.SendMessage("   \n\t"))

            assertEquals(initialCount, viewModel.uiState.value.messages.size)
            assertEquals("訊息內容不可為空白", viewModel.uiState.value.errorMessage)
        }
}
