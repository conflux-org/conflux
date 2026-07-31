package io.github.conflux_org.conflux.features.auth.domain.repository

import io.github.conflux_org.conflux.domain.model.User

interface AuthRepository {
    suspend fun login(
        username: String,
        password: String,
    ): Result<User>
}
