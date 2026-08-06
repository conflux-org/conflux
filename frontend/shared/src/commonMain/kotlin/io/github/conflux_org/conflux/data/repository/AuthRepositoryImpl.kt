package io.github.conflux_org.conflux.data.repository

import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.data.model.ErrorResponse
import io.github.conflux_org.conflux.data.model.LoginRequest
import io.github.conflux_org.conflux.data.model.LoginResponse
import io.github.conflux_org.conflux.data.model.SignupRequest
import io.github.conflux_org.conflux.data.model.SignupResponse
import io.github.conflux_org.conflux.domain.model.User
import io.github.conflux_org.conflux.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AuthRepositoryImpl(
    private val httpClient: HttpClient = HttpClientFactory.create(),
    private val baseUrl: String = "http://127.0.0.1:8000",
) : AuthRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): Result<User> =
        try {
            val response =
                httpClient.post("$baseUrl/api/auth/login/") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(username = username, password = password))
                }
            if (response.status.isSuccess()) {
                val body = response.body<LoginResponse>()
                Result.success(User(id = body.id, name = body.name))
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "登入失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun signUp(
        username: String,
        password: String,
    ): Result<User> =
        try {
            val response =
                httpClient.post("$baseUrl/auth/signup/") {
                    contentType(ContentType.Application.Json)
                    setBody(SignupRequest(username = username, password = password))
                }
            if (response.status.isSuccess()) {
                val body = response.body<SignupResponse>()
                Result.success(User(id = body.id, name = body.name))
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "註冊失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
