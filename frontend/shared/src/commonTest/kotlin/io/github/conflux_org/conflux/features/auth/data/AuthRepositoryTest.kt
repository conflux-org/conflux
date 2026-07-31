package io.github.conflux_org.conflux.features.auth.data

import io.github.conflux_org.conflux.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeAuthRepository(private val shouldSucceed: Boolean) : AuthRepository {
    override suspend fun login(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) return false
        return shouldSucceed
    }
}

class AuthRepositoryTest {

    @Test
    fun login_withValidCredentials_returnsTrue() = runBlocking {
        val repository = FakeAuthRepository(shouldSucceed = true)
        val result = repository.login("user", "pass")
        assertTrue(result)
    }

    @Test
    fun login_withInvalidCredentials_returnsFalse() = runBlocking {
        val repository = FakeAuthRepository(shouldSucceed = false)
        val result = repository.login("user", "pass")
        assertFalse(result)
    }

    @Test
    fun login_withEmptyUsername_returnsFalse() = runBlocking {
        val repository = FakeAuthRepository(shouldSucceed = true)
        val result = repository.login("", "pass")
        assertFalse(result)
    }
}
