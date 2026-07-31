package io.github.conflux_org.conflux.features.auth.data

import io.github.conflux_org.conflux.domain.model.User
import io.github.conflux_org.conflux.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeAuthRepository(
    private val shouldSucceed: Boolean,
) : AuthRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): User? {
        if (username.isBlank() || password.isBlank()) return null
        return if (shouldSucceed) User(id = "1", name = username) else null
    }
}

class AuthRepositoryTest {
    @Test
    fun login_withValidCredentials_returnsUser() =
        runTest {
            val repository = FakeAuthRepository(shouldSucceed = true)
            val result = repository.login("user", "pass")
            assertEquals(User(id = "1", name = "user"), result)
        }

    @Test
    fun login_withInvalidCredentials_returnsNull() =
        runTest {
            val repository = FakeAuthRepository(shouldSucceed = false)
            val result = repository.login("user", "pass")
            assertNull(result)
        }

    @Test
    fun login_withEmptyUsername_returnsNull() =
        runTest {
            val repository = FakeAuthRepository(shouldSucceed = true)
            val result = repository.login("", "pass")
            assertNull(result)
        }
}
