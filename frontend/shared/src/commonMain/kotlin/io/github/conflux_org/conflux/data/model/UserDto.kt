package io.github.conflux_org.conflux.data.model

import io.github.conflux_org.conflux.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
) {
    fun toDomain(): User = User(id, name)
}
