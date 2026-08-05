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

class AuthRepositoryImplTest {
    @Test
    fun loginReturnsUserWhenApiRespondsSuccessfully() =
        runTest {
            var request: HttpRequestData? = null
            val repository =
                AuthRepositoryImpl(
                    httpClient =
                        HttpClient(
                            MockEngine { capturedRequest ->
                                request = capturedRequest
                                respondJson("{\"id\":1,\"name\":\"Ada\"}")
                            },
                        ) { installJsonContentNegotiation() },
                    baseUrl = "https://api.example.test",
                )

            val result = repository.login("ada", "secret")

            assertEquals(1, result.getOrThrow().id)
            assertEquals("Ada", result.getOrThrow().name)
            assertEquals("/api/auth/login/", request?.url?.encodedPath)
            assertEquals("POST", request?.method?.value)
        }

    @Test
    fun signUpReturnsApiErrorWhenRequestIsRejected() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    httpClient =
                        HttpClient(
                            MockEngine {
                                respondJson("{\"error\":\"username already exists\"}", HttpStatusCode.Conflict)
                            },
                        ) { installJsonContentNegotiation() },
                    baseUrl = "https://api.example.test",
                )

            val result = repository.signUp("ada", "secret")

            assertTrue(result.isFailure)
            assertEquals("username already exists", result.exceptionOrNull()?.message)
        }

    private fun io.ktor.client.HttpClientConfig<*>.installJsonContentNegotiation() {
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
