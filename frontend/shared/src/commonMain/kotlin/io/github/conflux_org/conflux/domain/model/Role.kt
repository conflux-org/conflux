package io.github.conflux_org.conflux.domain.model

data class Role(
    val id: Long,
    val guildId: Long,
    val name: String,
    val permissions: Long,
    val position: Int,
    val isEveryone: Boolean,
    val createdAt: String = "",
)
