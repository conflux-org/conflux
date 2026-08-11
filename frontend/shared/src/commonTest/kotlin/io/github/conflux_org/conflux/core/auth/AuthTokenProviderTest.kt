package io.github.conflux_org.conflux.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthTokenProviderTest {
    @Test
    fun initialTokenIsNull() {
        val provider = InMemoryAuthTokenProvider()
        assertNull(provider.getToken())
    }

    @Test
    fun setTokenAndGetToken() {
        val provider = InMemoryAuthTokenProvider()
        provider.setToken("test-jwt-token")
        assertEquals("test-jwt-token", provider.getToken())
    }

    @Test
    fun updateToken() {
        val provider = InMemoryAuthTokenProvider()
        provider.setToken("token-1")
        assertEquals("token-1", provider.getToken())

        provider.setToken("token-2")
        assertEquals("token-2", provider.getToken())
    }

    @Test
    fun clearRemovesToken() {
        val provider = InMemoryAuthTokenProvider()
        provider.setToken("test-jwt-token")
        assertEquals("test-jwt-token", provider.getToken())

        provider.clear()
        assertNull(provider.getToken())
    }

    @Test
    fun setTokenToNullClearsToken() {
        val provider = InMemoryAuthTokenProvider()
        provider.setToken("test-jwt-token")
        assertEquals("test-jwt-token", provider.getToken())

        provider.setToken(null)
        assertNull(provider.getToken())
    }
}
