package io.github.conflux_org.conflux.domain.model

data class Guild(
    val name: String,
    val memberId: String,
    val memberName: String,
    val ownerId: String
)
