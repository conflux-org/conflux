package io.github.conflux_org.conflux.features.auth.data.repository

import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.features.auth.data.model.LoginRequest
import io.github.conflux_org.conflux.features.auth.data.model.LoginResponse
import io.github.conflux_org.conflux.features.auth.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AuthRepositoryImpl(
    private val httpClient: HttpClient = HttpClientFactory.create(),
    private val baseUrl: String = "http://127.0.0.1:8000"
) : AuthRepository {

    override suspend fun login(username: String, password: String): Boolean {
        return try {
            val response = httpClient.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username = username, password = password))
            }
            if (response.status.isSuccess()) {
                try {
                    val body = response.body<LoginResponse>()
                    body.success ?: (body.status?.lowercase() != "error" && body.status?.lowercase() != "fail")
                } catch (_: Exception) {
                    true
                }
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
