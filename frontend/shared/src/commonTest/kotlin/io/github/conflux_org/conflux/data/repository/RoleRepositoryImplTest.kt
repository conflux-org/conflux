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

class RoleRepositoryImplTest {
    @Test
    fun getGuildRoles_success_returnsMappedRolesAndSendsAuthHeader() =
        runTest {
            val tokenProvider = InMemoryAuthTokenProvider().apply { setToken("test-jwt-token") }
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respondJson(
                        """[
                            {
                                "id": 1,
                                "guild_id": 10,
                                "name": "@everyone",
                                "permissions": 48,
                                "position": 0,
                                "is_everyone": true,
                                "created_at": "2026-09-10T12:00:00"
                            }
                        ]""",
                    )
                }
            val httpClient = HttpClientFactory.create(authTokenProvider = tokenProvider, engine = mockEngine)
            val repository = RoleRepositoryImpl(httpClient = httpClient)

            val result = repository.getGuildRoles(10L)

            assertTrue(result.isSuccess)
            val roles = result.getOrThrow()
            assertEquals(1, roles.size)
            assertEquals("@everyone", roles[0].name)
            assertEquals(48L, roles[0].permissions)
            assertTrue(roles[0].isEveryone)
            assertEquals("GET", capturedRequest?.method?.value)
            assertEquals("/api/guild/10/roles/", capturedRequest?.url?.encodedPath)
            assertEquals("Bearer test-jwt-token", capturedRequest?.headers?.get(HttpHeaders.Authorization))
        }

    @Test
    fun getGuildRoles_forbidden_returnsApiError() =
        runTest {
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson("""{"error": "Forbidden"}""", HttpStatusCode.Forbidden)
                        },
                )

            val result = repository.getGuildRoles(10L)

            assertTrue(result.isFailure)
            assertEquals("Forbidden", result.exceptionOrNull()?.message)
        }

    @Test
    fun createRole_success_sendsPostAndReturnsCreatedRole() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient { request ->
                            capturedRequest = request
                            respondJson(
                                """{
                                    "id": 5,
                                    "guild_id": 10,
                                    "name": "Moderator",
                                    "permissions": 8,
                                    "position": 1,
                                    "is_everyone": false,
                                    "created_at": "2026-09-10T12:00:00"
                                }""",
                                HttpStatusCode.Created,
                            )
                        },
                )

            val result = repository.createRole(guildId = 10L, name = "Moderator", permissions = 8L, position = 1)

            assertTrue(result.isSuccess)
            val role = result.getOrThrow()
            assertEquals(5L, role.id)
            assertEquals("Moderator", role.name)
            assertEquals(8L, role.permissions)
            assertEquals("POST", capturedRequest?.method?.value)
            assertEquals("/api/guild/10/roles/", capturedRequest?.url?.encodedPath)
        }

    @Test
    fun createRole_missingPermission_returnsForbiddenError() =
        runTest {
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson(
                                """{"error": "Forbidden: missing MANAGE_ROLES permission"}""",
                                HttpStatusCode.Forbidden,
                            )
                        },
                )

            val result = repository.createRole(guildId = 10L, name = "Moderator")

            assertTrue(result.isFailure)
            assertEquals("Forbidden: missing MANAGE_ROLES permission", result.exceptionOrNull()?.message)
        }

    @Test
    fun updateRole_success_sendsPatchAndReturnsUpdatedRole() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient { request ->
                            capturedRequest = request
                            respondJson(
                                """{
                                    "id": 5,
                                    "guild_id": 10,
                                    "name": "Super Moderator",
                                    "permissions": 12,
                                    "position": 2,
                                    "is_everyone": false,
                                    "created_at": "2026-09-10T12:00:00"
                                }""",
                            )
                        },
                )

            val result = repository.updateRole(guildId = 10L, roleId = 5L, name = "Super Moderator", permissions = 12L)

            assertTrue(result.isSuccess)
            val role = result.getOrThrow()
            assertEquals("Super Moderator", role.name)
            assertEquals(12L, role.permissions)
            assertEquals("PATCH", capturedRequest?.method?.value)
            assertEquals("/api/guild/10/roles/5/", capturedRequest?.url?.encodedPath)
        }

    @Test
    fun updateRole_renameEveryone_returnsBadRequestError() =
        runTest {
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson(
                                """{"error": "Cannot rename @everyone role"}""",
                                HttpStatusCode.BadRequest,
                            )
                        },
                )

            val result = repository.updateRole(guildId = 10L, roleId = 1L, name = "Renamed")

            assertTrue(result.isFailure)
            assertEquals("Cannot rename @everyone role", result.exceptionOrNull()?.message)
        }

    @Test
    fun deleteRole_success_sendsDeleteAndReturnsSuccess() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient { request ->
                            capturedRequest = request
                            respondJson("""{"message": "Role deleted"}""")
                        },
                )

            val result = repository.deleteRole(guildId = 10L, roleId = 5L)

            assertTrue(result.isSuccess)
            assertEquals("DELETE", capturedRequest?.method?.value)
            assertEquals("/api/guild/10/roles/5/", capturedRequest?.url?.encodedPath)
        }

    @Test
    fun deleteRole_deleteEveryone_returnsBadRequestError() =
        runTest {
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient {
                            respondJson(
                                """{"error": "Cannot delete @everyone role"}""",
                                HttpStatusCode.BadRequest,
                            )
                        },
                )

            val result = repository.deleteRole(guildId = 10L, roleId = 1L)

            assertTrue(result.isFailure)
            assertEquals("Cannot delete @everyone role", result.exceptionOrNull()?.message)
        }

    @Test
    fun assignMemberRole_success_sendsPost() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient { request ->
                            capturedRequest = request
                            respondJson("""{"message": "Role assigned successfully"}""")
                        },
                )

            val result = repository.assignMemberRole(guildId = 10L, userId = 42L, roleId = 5L)

            assertTrue(result.isSuccess)
            assertEquals("POST", capturedRequest?.method?.value)
            assertEquals("/api/guild/10/members/42/roles/5/", capturedRequest?.url?.encodedPath)
        }

    @Test
    fun removeMemberRole_success_sendsDelete() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val repository =
                RoleRepositoryImpl(
                    httpClient =
                        mockClient { request ->
                            capturedRequest = request
                            respondJson("""{"message": "Role removed successfully"}""")
                        },
                )

            val result = repository.removeMemberRole(guildId = 10L, userId = 42L, roleId = 5L)

            assertTrue(result.isSuccess)
            assertEquals("DELETE", capturedRequest?.method?.value)
            assertEquals("/api/guild/10/members/42/roles/5/", capturedRequest?.url?.encodedPath)
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
