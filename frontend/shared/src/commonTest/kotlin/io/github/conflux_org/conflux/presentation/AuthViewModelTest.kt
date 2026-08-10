package io.github.conflux_org.conflux.presentation

import io.github.conflux_org.conflux.data.FakeAuthRepository
import io.github.conflux_org.conflux.features.auth.presentation.AuthIntent
import io.github.conflux_org.conflux.features.auth.presentation.AuthPage
import io.github.conflux_org.conflux.features.auth.presentation.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun switchPage_updatesCurrentPageInState() {
        val viewModel = AuthViewModel(mainDispatcher = testDispatcher)
        assertEquals(AuthPage.LOGIN, viewModel.uiState.value.currentPage)

        viewModel.handleIntent(AuthIntent.SwitchPage(AuthPage.SIGN_UP))
        assertEquals(AuthPage.SIGN_UP, viewModel.uiState.value.currentPage)
    }

    @Test
    fun signUp_withShortPassword_setsSignUpError() {
        val viewModel = AuthViewModel(mainDispatcher = testDispatcher)
        viewModel.handleIntent(AuthIntent.SignUpUsernameChanged("user"))
        viewModel.handleIntent(AuthIntent.SignUpPasswordChanged("123"))

        viewModel.handleIntent(AuthIntent.SignUp)
        assertEquals("密碼至少需要 6 個字元", viewModel.uiState.value.signUpError)
    }

    @Test
    fun signUp_withValidInput_triggersSignUpSuccess() =
        runTest {
            val fakeRepo = FakeAuthRepository(shouldSucceed = true)
            val viewModel = AuthViewModel(mainDispatcher = testDispatcher, authRepository = fakeRepo)

            var successUserId: Long? = null
            viewModel.onSignUpSuccess = { userId ->
                successUserId = userId
            }

            viewModel.handleIntent(AuthIntent.SignUpUsernameChanged("valid_user"))
            viewModel.handleIntent(AuthIntent.SignUpPasswordChanged("password123"))
            viewModel.handleIntent(AuthIntent.SignUp)

            assertEquals(1L, successUserId)
            assertTrue(
                viewModel.uiState.value.signUpError
                    .isEmpty(),
            )
        }
}
