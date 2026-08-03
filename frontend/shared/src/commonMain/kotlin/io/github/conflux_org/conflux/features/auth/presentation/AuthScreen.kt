package io.github.conflux_org.conflux.features.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.conflux_org.conflux.core.ui.components.AppOutlinedTextField

enum class AuthPage {
    LOGIN,
    SIGN_UP,
    FORGOT_PASSWORD,
}

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentPage by remember { mutableStateOf(AuthPage.LOGIN) }
    when (currentPage) {
        AuthPage.LOGIN -> {
            LoginPage(
                state = uiState,
                onIntent = viewModel::handleIntent,
                onForgotPasswordClick = { currentPage = AuthPage.FORGOT_PASSWORD },
                onSignUpClick = { currentPage = AuthPage.SIGN_UP },
            )
        }

        AuthPage.SIGN_UP -> {
            SignUpPage(
                onBackClick = { currentPage = AuthPage.LOGIN },
                onSignUpSuccess = { currentPage = AuthPage.LOGIN },
            )
        }

        AuthPage.FORGOT_PASSWORD -> {
            ForgotPasswordPage(onBackClick = { currentPage = AuthPage.LOGIN })
        }
    }
}

@Composable
private fun LoginPage(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
) {
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1F)).safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .background(color = Color(0xFF2E2E2E), shape = RoundedCornerShape(16.dp))
                    .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "登入你的帳號",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(
                modifier = Modifier.height(8.dp),
            )

            Text(
                text = "歡迎回來，請登入你的帳號",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF9A9A9A),
            )

            Spacer(
                modifier = Modifier.height(30.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                Text(text = "帳號", color = Color.White)

                Spacer(modifier = Modifier.height(4.dp))

                AppOutlinedTextField(
                    value = state.username,
                    onValueChange = { onIntent(AuthIntent.UsernameChanged(it)) },
                    placeholder = { Text("請輸入帳號", color = Color(0xFF8A8A8A)) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "密碼", color = Color.White)

                Spacer(modifier = Modifier.height(4.dp))

                AppOutlinedTextField(
                    value = state.password,
                    onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
                    placeholder = { Text("請輸入密碼", color = Color(0xFF8A8A8A)) },
                    visualTransformation =
                        if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = showPassword,
                        onCheckedChange = { showPassword = it },
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = Color(0xFF4A4A91),
                                uncheckedColor = Color.White,
                                checkmarkColor = Color.White,
                            ),
                    )

                    Text(
                        text = "顯示密碼",
                        color = Color.White,
                        modifier = Modifier.clickable { showPassword = !showPassword },
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onForgotPasswordClick) {
                        Text("忘記密碼")
                    }
                }

                AnimatedVisibility(
                    visible = state.errorMessage.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onIntent(AuthIntent.Login) },
                    enabled = state.isLoginButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4A91)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isLoginLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(text = "登入", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "還沒有帳號？", color = Color.White)

                TextButton(onClick = onSignUpClick) {
                    Text("立即註冊!")
                }
            }
        }
    }
}

/**
 * 註冊完整頁面
 */
@Composable
private fun SignUpPage(
    onBackClick: () -> Unit,
    onSignUpSuccess: () -> Unit,
) {
    var userName by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF00FFF4)).safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .background(
                        color = Color(0xFF2E2E2E),
                        shape = RoundedCornerShape(16.dp),
                    ).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "註冊新帳號",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "請填寫以下資料以建立帳號",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF9A9A9A),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                AppOutlinedTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        errorMessage = ""
                    },
                    label = { Text("用戶名稱") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.width(12.dp))

                AppOutlinedTextField(
                    value = userId,
                    onValueChange = {
                        userId = it
                        errorMessage = ""
                    },
                    label = { Text("#ID") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppOutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppOutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                },
                label = { Text("密碼") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppOutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = ""
                },
                label = { Text("再次輸入密碼") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = showPassword,
                    onCheckedChange = { showPassword = it },
                    colors =
                        CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4A4A91),
                            uncheckedColor = Color.White,
                            checkmarkColor = Color.White,
                        ),
                )

                Text(
                    text = "顯示密碼",
                    color = Color.White,
                    modifier = Modifier.clickable { showPassword = !showPassword },
                )
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        userName.isBlank() -> errorMessage = "請輸入用戶名稱"
                        userId.isBlank() -> errorMessage = "請輸入 ID"
                        email.isBlank() -> errorMessage = "請輸入 Email"
                        password.isBlank() -> errorMessage = "請輸入密碼"
                        password.length < 6 -> errorMessage = "密碼至少需要 6 個字元"
                        confirmPassword.isBlank() -> errorMessage = "請再次輸入密碼"
                        password != confirmPassword -> errorMessage = "兩次輸入的密碼不一致"
                        else -> {
                            errorMessage = ""
                            println("註冊成功")
                            onSignUpSuccess()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4A91)),
            ) {
                Text(text = "註冊", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onBackClick) {
                Text("已經有帳號？返回登入")
            }
        }
    }
}

@Composable
private fun ForgotPasswordPage(onBackClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF00FFF4)).safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .background(color = Color(0xFF2E2E2E), shape = RoundedCornerShape(16.dp))
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "忘記密碼",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "請輸入註冊帳號時使用的 Email",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF9A9A9A),
            )

            Spacer(modifier = Modifier.height(24.dp))

            AppOutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                    successMessage = ""
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (successMessage.isNotEmpty()) {
                Text(
                    text = successMessage,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = Color(0xFF65D46E),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank()) {
                        errorMessage = "請輸入 Email"
                        successMessage = ""
                    } else {
                        errorMessage = ""
                        println("寄送重設密碼信件至：$email")
                        successMessage = "重設密碼信件已寄出"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4A91)),
            ) {
                Text(text = "寄送重設密碼信件", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onBackClick) {
                Text("返回登入")
            }
        }
    }
}

@Preview
@Composable
private fun AuthScreenPreview() {
    LoginPage(
        state = AuthUiState(),
        onIntent = {},
        onForgotPasswordClick = {},
        onSignUpClick = {},
    )
}
