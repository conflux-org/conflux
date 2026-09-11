package io.github.conflux_org.conflux.data.repository

import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.data.model.ChannelOverwriteDto
import io.github.conflux_org.conflux.data.model.ErrorResponse
import io.github.conflux_org.conflux.data.model.SetChannelOverwriteRequest
import io.github.conflux_org.conflux.domain.model.ChannelOverwrite
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType
import io.github.conflux_org.conflux.domain.repository.ChannelOverwriteRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ChannelOverwriteRepositoryImpl(
    private val httpClient: HttpClient = HttpClientFactory.create(),
    private val baseUrl: String = "http://127.0.0.1:8000",
) : ChannelOverwriteRepository {
    override suspend fun getChannelOverwrites(channelId: Long): Result<List<ChannelOverwrite>> =
        try {
            val response =
                httpClient.get("$baseUrl/api/channel/$channelId/overwrites/") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                val body = response.body<List<ChannelOverwriteDto>>()
                Result.success(body.map { it.toDomain() })
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "取得頻道覆寫失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun setChannelOverwrite(
        channelId: Long,
        targetType: OverwriteTargetType,
        targetId: Long,
        allow: Long,
        deny: Long,
    ): Result<ChannelOverwrite> =
        try {
            val response =
                httpClient.put("$baseUrl/api/channel/$channelId/overwrites/${targetType.toApiString()}/$targetId/") {
                    contentType(ContentType.Application.Json)
                    setBody(SetChannelOverwriteRequest(allow = allow, deny = deny))
                }

            if (response.status.isSuccess()) {
                val body = response.body<ChannelOverwriteDto>()
                Result.success(body.toDomain())
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "設定頻道覆寫失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteChannelOverwrite(
        channelId: Long,
        targetType: OverwriteTargetType,
        targetId: Long,
    ): Result<Unit> =
        try {
            val response =
                httpClient.delete("$baseUrl/api/channel/$channelId/overwrites/${targetType.toApiString()}/$targetId/") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "刪除頻道覆寫失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
