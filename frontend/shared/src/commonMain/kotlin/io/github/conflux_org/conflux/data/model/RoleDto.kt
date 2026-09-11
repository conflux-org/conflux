package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.Role
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoleDto(
    val id: Long,
    @SerialName("guild_id")
    val guildId: Long,
    val name: String,
    val permissions: Long,
    val position: Int,
    @SerialName("is_everyone")
    val isEveryone: Boolean,
    @SerialName("created_at")
    val createdAt: String? = null,
) {
    fun toDomain(): Role =
        Role(
            id = id,
            guildId = guildId,
            name = name,
            permissions = permissions,
            position = position,
            isEveryone = isEveryone,
            createdAt = createdAt.orEmpty(),
        )
}
