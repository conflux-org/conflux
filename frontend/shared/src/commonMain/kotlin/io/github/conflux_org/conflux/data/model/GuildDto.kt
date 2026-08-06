package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.Guild
import kotlinx.serialization.Serializable

@Serializable
data class GuildDto(
    val id: Long,
    val name: String,
) {
    fun toDomain(): Guild = Guild(id, name)
}
