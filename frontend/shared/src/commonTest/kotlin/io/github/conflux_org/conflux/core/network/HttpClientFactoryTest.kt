package io.github.conflux_org.conflux.core.network

import io.github.conflux_org.conflux.core.auth.InMemoryAuthTokenProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HttpClientFactoryTest {
    @Test
    fun addsAuthorizationHeaderWhenTokenIsPresent() =
        runTest {
            val tokenProvider = InMemoryAuthTokenProvider().apply { setToken("test-jwt-token") }
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respondOk()
                }

            val client = HttpClientFactory.create(authTokenProvider = tokenProvider, engine = mockEngine)
            client.get("https://api.example.test/api/guild/1/channels/")

            assertEquals(
                "Bearer test-jwt-token",
                capturedRequest?.headers?.get(HttpHeaders.Authorization),
            )
        }

    @Test
    fun doesNotAddAuthorizationHeaderWhenTokenIsNull() =
        runTest {
            val tokenProvider = InMemoryAuthTokenProvider()
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respondOk()
                }

            val client = HttpClientFactory.create(authTokenProvider = tokenProvider, engine = mockEngine)
            client.get("https://api.example.test/api/auth/login/")

            assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))
        }

    @Test
    fun doesNotAddAuthorizationHeaderWhenTokenProviderIsNotProvided() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respondOk()
                }

            val client = HttpClientFactory.create(engine = mockEngine)
            client.get("https://api.example.test/api/auth/login/")

            assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))
        }
}
