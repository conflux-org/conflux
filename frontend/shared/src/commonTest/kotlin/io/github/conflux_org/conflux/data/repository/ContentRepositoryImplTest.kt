package io.github.conflux_org.conflux.data.repository

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
