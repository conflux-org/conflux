package io.github.conflux_org.conflux

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
