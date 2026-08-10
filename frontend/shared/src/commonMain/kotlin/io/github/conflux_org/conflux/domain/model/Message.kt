package io.github.conflux_org.conflux.domain.model

data class Message(
    val id: Long,
    val author: User,
    val content: String,
)
