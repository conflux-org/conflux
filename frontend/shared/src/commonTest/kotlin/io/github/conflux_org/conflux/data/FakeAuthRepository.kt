package io.github.conflux_org.conflux.data

import io.github.conflux_org.conflux.domain.model.User
import io.github.conflux_org.conflux.domain.repository.AuthRepository

class FakeAuthRepository(
    var shouldSucceed: Boolean = true,
    var mockUser: User = User(id = 1L, name = "test_user"),
    var errorMessage: String = "驗證失敗",
) : AuthRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("帳號密碼不可為空"))
        }
        return if (shouldSucceed) {
            Result.success(mockUser.copy(name = username))
        } else {
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun signUp(
        username: String,
        password: String,
    ): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("帳號密碼不可為空"))
        }
        return if (shouldSucceed) {
            Result.success(mockUser.copy(name = username))
        } else {
            Result.failure(Exception(errorMessage))
        }
    }
}
