package io.github.conflux_org.conflux.features.auth.domain.repository

interface AuthRepository {
    suspend fun login(username: String, password: String): Boolean
}
