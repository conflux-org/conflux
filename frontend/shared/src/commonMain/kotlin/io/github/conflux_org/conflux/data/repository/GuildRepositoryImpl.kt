package io.github.conflux_org.conflux.data.repository

import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.data.model.CreateGuildRequest
import io.github.conflux_org.conflux.data.model.CreateGuildResponseDto
import io.github.conflux_org.conflux.data.model.ErrorResponse
import io.github.conflux_org.conflux.data.model.GuildDto
import io.github.conflux_org.conflux.domain.model.Guild
import io.github.conflux_org.conflux.domain.repository.GuildRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class GuildRepositoryImpl(
    private val httpClient: HttpClient = HttpClientFactory.create(),
    private val baseUrl: String = "http://127.0.0.1:8000",
) : GuildRepository {
    override suspend fun getGuildsByUserId(userId: Long): Result<List<Guild>> =
        try {
            val response =
                httpClient.get("$baseUrl/api/user/$userId/guilds/") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                val body = response.body<List<GuildDto>>()
                Result.success(body.map { it.toDomain() })
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "取得伺服器列表失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun createGuild(name: String): Result<Guild> =
        try {
            val response =
                httpClient.post("$baseUrl/api/guild/") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateGuildRequest(name = name))
                }

            if (response.status.isSuccess()) {
                val body = response.body<CreateGuildResponseDto>()
                Result.success(body.toDomain())
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "建立伺服器失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
