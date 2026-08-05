package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.Channel
import kotlinx.serialization.Serializable

@Serializable
data class ChannelDto(
    val id: Long,
    val name: String,
) {
    fun toDomain(): Channel = Channel(id, name)
}
