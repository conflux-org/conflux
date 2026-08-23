package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.Guild
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateGuildResponseDto(
    val id: Long,
    val name: String,
    @SerialName("owner_id")
    val ownerId: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
) {
    fun toDomain(): Guild = Guild(id = id, name = name)
}
