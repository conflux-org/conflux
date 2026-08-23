package io.github.conflux_org.conflux.features.main.presentation

import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.model.Message

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
)
