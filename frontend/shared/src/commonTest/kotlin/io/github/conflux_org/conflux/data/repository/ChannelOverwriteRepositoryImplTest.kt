package io.github.conflux_org.conflux.data.repository

import io.github.conflux_org.conflux.core.auth.InMemoryAuthTokenProvider
import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType
import io.github.conflux_org.conflux.domain.model.PermissionFlags
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

class ChannelOverwriteRepositoryImplTest {
    @Test
    fun getChannelOverwrites_success_returnsMappedOverwritesAndSendsAuthHeader() =
        runTest {
            val tokenProvider = InMemoryAuthTokenProvider().apply { setToken("overwrite-jwt-token") }
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respondJson(
                        """[
                            {
                                "id": 1,
                                "channel_id": 101,
                                "target_type": "ROLE",
                                "target_id": 1,
                                "allow": 16,
                                "deny": 32,
                                "created_at": "2026-09-10T12:00:00"
                            },
                            {
                                "id": 2,
                                "channel_id": 101,
                                "target_type": "MEMBER",
                                "target_id": 42,
                                "allow": 32,
                                "deny": 0,
                                "created_at": "2026-09-10T12:00:00"
                            }
                        ]""",
                    )
                }
            val httpClient = HttpClientFactory.create(authTokenProvider = tokenProvider, engine = mockEngine)
            val repository = ChannelOverwriteRepositoryImpl(httpClient = httpClient)

            val result = repository.getChannelOverwrites(101L)

            assertTrue(result.isSuccess)
            val overwrites = result.getOrThrow()
            assertEquals(2, overwrites.size)
            assertEquals(OverwriteTargetType.ROLE, overwrites[0].targetType)
            assertEquals(16L, overwrites[0].allow)
            assertEquals(32L, overwrites[0].deny)
            assertEquals(OverwriteTargetType.MEMBER, overwrites[1].targetType)
            assertEquals(42L, overwrites[1].targetId)
            assertEquals("GET", capturedRequest?.method?.value)
            assertEquals("/api/channel/101/overwrites/", capturedRequest?.url?.encodedPath)
            assertEquals("Bearer overwrite-jwt-token", capturedRequest?.headers?.get(HttpHeaders.Authorization))
        }

    @Test
    fun getChannelOverwrites_missingPermission_returnsForbidden() =
        runTest {
            val repository =
                ChannelOverwriteRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson(
                                """{"error": "Forbidden: missing MANAGE_CHANNELS permission"}""",
                                HttpStatusCode.Forbidden,
                            )
                        },
                )

            val result = repository.getChannelOverwrites(101L)

            assertTrue(result.isFailure)
            assertEquals("Forbidden: missing MANAGE_CHANNELS permission", result.exceptionOrNull()?.message)
        }

    @Test
    fun setChannelOverwrite_success_sendsPutAndReturnsUpdatedOverwrite() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val repository =
                ChannelOverwriteRepositoryImpl(
                    httpClient =
                        mockClient { request ->
                            capturedRequest = request
                            respondJson(
                                """{
                                    "id": 1,
                                    "channel_id": 101,
                                    "target_type": "ROLE",
                                    "target_id": 5,
                                    "allow": 16,
                                    "deny": 32,
                                    "created_at": "2026-09-10T12:00:00"
                                }""",
                                HttpStatusCode.OK,
                            )
                        },
                )

            val result =
                repository.setChannelOverwrite(
                    channelId = 101L,
                    targetType = OverwriteTargetType.ROLE,
                    targetId = 5L,
                    allow = PermissionFlags.VIEW_CHANNEL,
                    deny = PermissionFlags.SEND_MESSAGES,
                )

            assertTrue(result.isSuccess)
            val overwrite = result.getOrThrow()
            assertEquals(101L, overwrite.channelId)
            assertEquals(OverwriteTargetType.ROLE, overwrite.targetType)
            assertEquals(5L, overwrite.targetId)
            assertEquals(16L, overwrite.allow)
            assertEquals(32L, overwrite.deny)
            assertEquals("PUT", capturedRequest?.method?.value)
            assertEquals("/api/channel/101/overwrites/role/5/", capturedRequest?.url?.encodedPath)
        }

    @Test
    fun setChannelOverwrite_overlappingAllowDeny_returnsBadRequest() =
        runTest {
            val repository =
                ChannelOverwriteRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson(
                                """{"error": "Permissions in allow and deny cannot overlap"}""",
                                HttpStatusCode.BadRequest,
                            )
                        },
                )

            val result =
                repository.setChannelOverwrite(
                    channelId = 101L,
                    targetType = OverwriteTargetType.ROLE,
                    targetId = 5L,
                    allow = PermissionFlags.SEND_MESSAGES,
                    deny = PermissionFlags.SEND_MESSAGES,
                )

            assertTrue(result.isFailure)
            assertEquals("Permissions in allow and deny cannot overlap", result.exceptionOrNull()?.message)
        }

    @Test
    fun deleteChannelOverwrite_success_sendsDeleteAndReturnsSuccess() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val repository =
                ChannelOverwriteRepositoryImpl(
                    httpClient =
                        mockClient { request ->
                            capturedRequest = request
                            respondJson("""{"message": "Overwrite deleted"}""")
                        },
                )

            val result =
                repository.deleteChannelOverwrite(
                    channelId = 101L,
                    targetType = OverwriteTargetType.ROLE,
                    targetId = 5L,
                )

            assertTrue(result.isSuccess)
            assertEquals("DELETE", capturedRequest?.method?.value)
            assertEquals("/api/channel/101/overwrites/role/5/", capturedRequest?.url?.encodedPath)
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
