package io.github.conflux_org.conflux.core.auth

import kotlin.concurrent.Volatile

interface AuthTokenProvider {
    fun getToken(): String?

    fun setToken(token: String?)

    fun clear()
}

class InMemoryAuthTokenProvider : AuthTokenProvider {
    @Volatile
    private var token: String? = null

    override fun getToken(): String? = token

    override fun setToken(token: String?) {
        this.token = token
    }

    override fun clear() {
        this.token = null
    }
}
