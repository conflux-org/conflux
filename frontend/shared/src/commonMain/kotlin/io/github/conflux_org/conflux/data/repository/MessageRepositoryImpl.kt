package io.github.conflux_org.conflux.data.repository

import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.data.model.ErrorResponse
import io.github.conflux_org.conflux.data.model.MessageDto
import io.github.conflux_org.conflux.domain.model.Message
import io.github.conflux_org.conflux.domain.repository.MessageRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class MessageRepositoryImpl(
    private val httpClient: HttpClient = HttpClientFactory.create(),
    private val baseUrl: String = "http://127.0.0.1:8000",
) : MessageRepository {
    override suspend fun getMessageByChannelId(channelId: Long): Result<List<Message>> =
        try {
            val response =
                httpClient.post("$baseUrl/api/channel/$channelId/messages/") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                val body = response.body<List<MessageDto>>()
                Result.success(body.map { it.toDomain() })
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "登入失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
