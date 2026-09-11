package io.github.conflux_org.conflux.domain.model

enum class OverwriteTargetType {
    ROLE,
    MEMBER,
    ;

    fun toApiString(): String = name.lowercase()
}
