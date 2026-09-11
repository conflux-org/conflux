package io.github.conflux_org.conflux.data.repository

import io.github.conflux_org.conflux.core.network.HttpClientFactory
import io.github.conflux_org.conflux.data.model.CreateRoleRequest
import io.github.conflux_org.conflux.data.model.ErrorResponse
import io.github.conflux_org.conflux.data.model.RoleDto
import io.github.conflux_org.conflux.data.model.UpdateRoleRequest
import io.github.conflux_org.conflux.domain.model.Role
import io.github.conflux_org.conflux.domain.repository.RoleRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class RoleRepositoryImpl(
    private val httpClient: HttpClient = HttpClientFactory.create(),
    private val baseUrl: String = "http://127.0.0.1:8000",
) : RoleRepository {
    override suspend fun getGuildRoles(guildId: Long): Result<List<Role>> =
        try {
            val response =
                httpClient.get("$baseUrl/api/guild/$guildId/roles/") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                val body = response.body<List<RoleDto>>()
                Result.success(body.map { it.toDomain() })
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "取得身份組失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun createRole(
        guildId: Long,
        name: String,
        permissions: Long,
        position: Int,
    ): Result<Role> =
        try {
            val response =
                httpClient.post("$baseUrl/api/guild/$guildId/roles/") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateRoleRequest(name = name, permissions = permissions, position = position))
                }

            if (response.status.isSuccess()) {
                val body = response.body<RoleDto>()
                Result.success(body.toDomain())
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "建立身份組失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun updateRole(
        guildId: Long,
        roleId: Long,
        name: String?,
        permissions: Long?,
        position: Int?,
    ): Result<Role> =
        try {
            val response =
                httpClient.patch("$baseUrl/api/guild/$guildId/roles/$roleId/") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateRoleRequest(name = name, permissions = permissions, position = position))
                }

            if (response.status.isSuccess()) {
                val body = response.body<RoleDto>()
                Result.success(body.toDomain())
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "更新身份組失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteRole(
        guildId: Long,
        roleId: Long,
    ): Result<Unit> =
        try {
            val response =
                httpClient.delete("$baseUrl/api/guild/$guildId/roles/$roleId/") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "刪除身份組失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun assignMemberRole(
        guildId: Long,
        userId: Long,
        roleId: Long,
    ): Result<Unit> =
        try {
            val response =
                httpClient.post("$baseUrl/api/guild/$guildId/members/$userId/roles/$roleId/") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "指派身份組失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun removeMemberRole(
        guildId: Long,
        userId: Long,
        roleId: Long,
    ): Result<Unit> =
        try {
            val response =
                httpClient.delete("$baseUrl/api/guild/$guildId/members/$userId/roles/$roleId/") {
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val body = response.body<ErrorResponse>()
                Result.failure(Exception(body.error ?: "移除身份組失敗 (${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
