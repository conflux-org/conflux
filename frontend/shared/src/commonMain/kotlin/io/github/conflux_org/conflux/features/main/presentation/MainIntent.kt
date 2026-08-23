package io.github.conflux_org.conflux.features.main.presentation

import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.Guild

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
}
