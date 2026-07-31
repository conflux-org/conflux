package io.github.conflux_org.conflux.features.auth.data

import io.github.conflux_org.conflux.domain.model.User
import io.github.conflux_org.conflux.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeAuthRepository(
    private val shouldSucceed: Boolean,
) : AuthRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("帳號密碼不可為空"))
        }
        return if (shouldSucceed) {
            Result.success(User(id = 1L, name = username))
        } else {
            Result.failure(Exception("帳號或密碼錯誤"))
        }
    }
}

class AuthRepositoryTest {
    @Test
    fun login_withValidCredentials_returnsSuccessUser() =
        runTest {
            val repository = FakeAuthRepository(shouldSucceed = true)
            val result = repository.login("user", "pass")
            assertTrue(result.isSuccess)
            assertEquals(User(id = 1L, name = "user"), result.getOrNull())
        }

    @Test
    fun login_withInvalidCredentials_returnsFailure() =
        runTest {
            val repository = FakeAuthRepository(shouldSucceed = false)
            val result = repository.login("user", "pass")
            assertTrue(result.isFailure)
        }

    @Test
    fun login_withEmptyUsername_returnsFailure() =
        runTest {
            val repository = FakeAuthRepository(shouldSucceed = true)
            val result = repository.login("", "pass")
            assertTrue(result.isFailure)
        }
}
