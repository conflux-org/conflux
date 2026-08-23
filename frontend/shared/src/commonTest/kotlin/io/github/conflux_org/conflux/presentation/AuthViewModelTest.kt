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

    @Test
    fun login_withSuccessfulCredentials_triggersLoginSuccess() =
        runTest {
            val fakeRepo = FakeAuthRepository(shouldSucceed = true)
            val viewModel = AuthViewModel(mainDispatcher = testDispatcher, authRepository = fakeRepo)

            var successUserId: Long? = null
            viewModel.onLoginSuccess = { userId ->
                successUserId = userId
            }

            viewModel.handleIntent(AuthIntent.LoginUsernameChanged("valid_user"))
            viewModel.handleIntent(AuthIntent.LoginPasswordChanged("password123"))
            viewModel.handleIntent(AuthIntent.Login)

            assertEquals(1L, successUserId)
            assertTrue(
                viewModel.uiState.value.loginError
                    .isEmpty(),
            )
        }

    @Test
    fun login_withFailedCredentials_setsLoginError() =
        runTest {
            val fakeRepo = FakeAuthRepository(shouldSucceed = false, errorMessage = "帳號或密碼錯誤")
            val viewModel = AuthViewModel(mainDispatcher = testDispatcher, authRepository = fakeRepo)

            var successUserId: Long? = null
            viewModel.onLoginSuccess = { userId ->
                successUserId = userId
            }

            viewModel.handleIntent(AuthIntent.LoginUsernameChanged("invalid_user"))
            viewModel.handleIntent(AuthIntent.LoginPasswordChanged("wrong_password"))
            viewModel.handleIntent(AuthIntent.Login)

            assertEquals(null, successUserId)
            assertEquals("帳號或密碼錯誤", viewModel.uiState.value.loginError)
        }

    @Test
    fun signUp_withFailedCredentials_setsSignUpError() =
        runTest {
            val fakeRepo = FakeAuthRepository(shouldSucceed = false, errorMessage = "使用者名稱已被使用")
            val viewModel = AuthViewModel(mainDispatcher = testDispatcher, authRepository = fakeRepo)

            var successUserId: Long? = null
            viewModel.onSignUpSuccess = { userId ->
                successUserId = userId
            }

            viewModel.handleIntent(AuthIntent.SignUpUsernameChanged("duplicate_user"))
            viewModel.handleIntent(AuthIntent.SignUpPasswordChanged("password123"))
            viewModel.handleIntent(AuthIntent.SignUp)

            assertEquals(null, successUserId)
            assertEquals("使用者名稱已被使用", viewModel.uiState.value.signUpError)
        }
}
