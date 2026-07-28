package io.github.conflux_org.conflux

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
