package io.github.conflux_org.conflux.features.main.presentation

import androidx.lifecycle.ViewModel
import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.model.Message
import io.github.conflux_org.conflux.domain.model.User
import io.github.conflux_org.conflux.domain.repository.ChannelRepository
import io.github.conflux_org.conflux.domain.repository.GuildRepository
import io.github.conflux_org.conflux.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadInitialData -> loadInitialData(intent.userId)
            is MainIntent.SelectGuild -> selectGuild(intent.guild)
            is MainIntent.SelectChannel -> selectChannel(intent.channel)
            is MainIntent.SendMessage -> sendMessage(intent.content)
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
        CoroutineScope(mainDispatcher).launch {
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
                errorMessage = null,
                channels = emptyList(),
                selectedChannel = null,
                messages = emptyList(),
            )
        }
        CoroutineScope(mainDispatcher).launch {
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
        CoroutineScope(mainDispatcher).launch {
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
        val currentMessages = _uiState.value.messages
        val nextId = (currentMessages.maxOfOrNull { it.id } ?: 0L) + 1L
        val newMessage =
            Message(
                id = nextId,
                author = User(id = _uiState.value.currentUserId, name = "You"),
                content = content,
            )
        _uiState.update { it.copy(messages = currentMessages + newMessage) }
    }
}
