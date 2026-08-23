package io.github.conflux_org.conflux.data.repository

import io.github.conflux_org.conflux.core.auth.InMemoryAuthTokenProvider
import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentRepositoryImplTest {
    @Test
    fun channelRepositoryGetsAndMapsChannels() =
        runTest {
            var request: HttpRequestData? = null
            val repository =
                ChannelRepositoryImpl(
                    httpClient =
                        mockClient { capturedRequest ->
                            request = capturedRequest
                            respondJson("[{\"id\":1,\"name\":\"general\"}]")
                        },
                )

            val result = repository.getChannelsByGuildId(7)

            assertEquals("GET", request?.method?.value)
            assertEquals("/api/guild/7/channels/", request?.url?.encodedPath)
            assertEquals("general", result.getOrThrow().single().name)
        }

    @Test
    fun guildRepositoryGetsAndMapsGuilds() =
        runTest {
            var request: HttpRequestData? = null
            val repository =
                GuildRepositoryImpl(
                    httpClient =
                        mockClient { capturedRequest ->
                            request = capturedRequest
                            respondJson("[{\"id\":2,\"name\":\"Conflux\"}]")
                        },
                )

            val result = repository.getGuildsByUserId(9)

            assertEquals("GET", request?.method?.value)
            assertEquals("/api/user/9/guilds/", request?.url?.encodedPath)
            assertEquals("Conflux", result.getOrThrow().single().name)
        }

    @Test
    fun messageRepositoryGetsAndMapsMessages() =
        runTest {
            var request: HttpRequestData? = null
            val repository =
                MessageRepositoryImpl(
                    httpClient =
                        mockClient { capturedRequest ->
                            request = capturedRequest
                            respondJson(
                                "[{\"id\":3,\"author\":{\"id\":4,\"name\":\"Ada\"},\"content\":\"Hello\"}]",
                            )
                        },
                )

            val result = repository.getMessagesByChannelId(11)

            assertEquals("GET", request?.method?.value)
            assertEquals("/api/channel/11/messages/", request?.url?.encodedPath)
            assertEquals(
                "Ada",
                result
                    .getOrThrow()
                    .single()
                    .author.name,
            )
            assertEquals("Hello", result.getOrThrow().single().content)
        }

    @Test
    fun repositoriesReturnApiErrorMessageOnFailure() =
        runTest {
            val repository =
                ChannelRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson("{}", HttpStatusCode.InternalServerError)
                        },
                )

            val result = repository.getChannelsByGuildId(7)

            assertTrue(result.isFailure)
            assertEquals("取得頻道失敗 (500)", result.exceptionOrNull()?.message)
        }

    @Test
    fun channelRepositoryIncludesAuthorizationHeaderWhenTokenPresent() =
        runTest {
            val tokenProvider = InMemoryAuthTokenProvider().apply { setToken("content-jwt-token") }
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respondJson("[{\"id\":1,\"name\":\"general\"}]")
                }
            val httpClient = HttpClientFactory.create(authTokenProvider = tokenProvider, engine = mockEngine)
            val repository = ChannelRepositoryImpl(httpClient = httpClient)

            val result = repository.getChannelsByGuildId(7)

            assertTrue(result.isSuccess)
            assertEquals("Bearer content-jwt-token", capturedRequest?.headers?.get(HttpHeaders.Authorization))
        }

    @Test
    fun guildRepositoryIncludesAuthorizationHeaderWhenTokenPresent() =
        runTest {
            val tokenProvider = InMemoryAuthTokenProvider().apply { setToken("content-jwt-token") }
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respondJson("[{\"id\":2,\"name\":\"Conflux\"}]")
                }
            val httpClient = HttpClientFactory.create(authTokenProvider = tokenProvider, engine = mockEngine)
            val repository = GuildRepositoryImpl(httpClient = httpClient)

            val result = repository.getGuildsByUserId(9)

            assertTrue(result.isSuccess)
            assertEquals("Bearer content-jwt-token", capturedRequest?.headers?.get(HttpHeaders.Authorization))
        }

    @Test
    fun messageRepositoryIncludesAuthorizationHeaderWhenTokenPresent() =
        runTest {
            val tokenProvider = InMemoryAuthTokenProvider().apply { setToken("content-jwt-token") }
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respondJson(
                        "[{\"id\":3,\"author\":{\"id\":4,\"name\":\"Ada\"},\"content\":\"Hello\"}]",
                    )
                }
            val httpClient = HttpClientFactory.create(authTokenProvider = tokenProvider, engine = mockEngine)
            val repository = MessageRepositoryImpl(httpClient = httpClient)

            val result = repository.getMessagesByChannelId(11)

            assertTrue(result.isSuccess)
            assertEquals("Bearer content-jwt-token", capturedRequest?.headers?.get(HttpHeaders.Authorization))
        }

    @Test
    fun messageRepositorySendsMessageAndMapsCreatedResponse() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val httpClient =
                HttpClient(
                    MockEngine { request ->
                        capturedRequest = request
                        respondJson(
                            "{\"id\":12,\"channel_id\":11,\"author\":{\"id\":4,\"name\":\"Ada\"},\"content\":\"Hello\",\"created_at\":\"2026-08-23T00:00:00Z\"}",
                            HttpStatusCode.Created,
                        )
                    },
                ) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }
            val repository = MessageRepositoryImpl(httpClient = httpClient)

            val result = repository.sendMessage(11, "Hello")

            assertEquals("POST", capturedRequest?.method?.value)
            assertEquals("/api/channel/11/messages/", capturedRequest?.url?.encodedPath)
            assertEquals(12, result.getOrThrow().id)
            assertEquals("Hello", result.getOrThrow().content)
        }

    @Test
    fun messageRepositoryReturnsApiErrorWhenSendFails() =
        runTest {
            val repository =
                MessageRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson("{\"error\":\"Content cannot be empty\"}", HttpStatusCode.BadRequest)
                        },
                )

            val result = repository.sendMessage(11, "")

            assertTrue(result.isFailure)
            assertEquals("Content cannot be empty", result.exceptionOrNull()?.message)
        }

    @Test
    fun messageRepositoryReturnsFallbackErrorWhenServerFails() =
        runTest {
            val repository =
                MessageRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson("{}", HttpStatusCode.InternalServerError)
                        },
                )

            val result = repository.sendMessage(11, "Hello")

            assertTrue(result.isFailure)
            assertEquals("發送訊息失敗 (500)", result.exceptionOrNull()?.message)
        }

    private fun mockClient(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ) = HttpClient(MockEngine { request -> handler(request) }) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}
