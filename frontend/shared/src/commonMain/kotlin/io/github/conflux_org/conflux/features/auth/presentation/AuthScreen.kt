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

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.currentPage) {
        AuthPage.LOGIN -> {
            LoginPage(
                state = uiState,
                onIntent = viewModel::handleIntent,
            )
        }

        AuthPage.SIGN_UP -> {
            SignUpPage(
                state = uiState,
                onIntent = viewModel::handleIntent,
            )
        }

        AuthPage.FORGOT_PASSWORD -> {
            ForgotPasswordPage(
                onIntent = viewModel::handleIntent,
            )
        }
    }
}

@Composable
private fun LoginPage(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
) {
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
                    .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "登入你的帳號",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "歡迎回來，請登入你的帳號",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9A9A9A),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "帳號", color = Color.White)

                Spacer(modifier = Modifier.height(4.dp))

                AppOutlinedTextField(
                    value = state.username,
                    onValueChange = { onIntent(AuthIntent.LoginUsernameChanged(it)) },
                    placeholder = { Text("請輸入帳號", color = Color(0xFF8A8A8A)) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "密碼", color = Color.White)

                Spacer(modifier = Modifier.height(4.dp))

                AppOutlinedTextField(
                    value = state.password,
                    onValueChange = { onIntent(AuthIntent.LoginPasswordChanged(it)) },
                    placeholder = { Text("請輸入密碼", color = Color(0xFF8A8A8A)) },
                    visualTransformation =
                        if (state.showLoginPassword) {
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
                        checked = state.showLoginPassword,
                        onCheckedChange = { onIntent(AuthIntent.LoginToggleShowPassword(it)) },
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
                        modifier = Modifier.clickable { onIntent(AuthIntent.LoginToggleShowPassword(!state.showLoginPassword)) },
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = { onIntent(AuthIntent.SwitchPage(AuthPage.FORGOT_PASSWORD)) }) {
                        Text("忘記密碼")
                    }
                }

                AnimatedVisibility(
                    visible = state.loginError.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Text(
                        text = state.loginError,
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

                TextButton(onClick = { onIntent(AuthIntent.SwitchPage(AuthPage.SIGN_UP)) }) {
                    Text("立即註冊!")
                }
            }
        }
    }
}

/**
 * 精簡版註冊頁面（僅需求帳號與密碼）
 */
@Composable
private fun SignUpPage(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
) {
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
                    .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "註冊新帳號",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "請填寫以下資訊建立帳號",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9A9A9A),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "帳號", color = Color.White)

                Spacer(modifier = Modifier.height(4.dp))

                AppOutlinedTextField(
                    value = state.signUpUsername,
                    onValueChange = { onIntent(AuthIntent.SignUpUsernameChanged(it)) },
                    placeholder = { Text("請輸入帳號名稱", color = Color(0xFF8A8A8A)) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "密碼", color = Color.White)

                Spacer(modifier = Modifier.height(4.dp))

                AppOutlinedTextField(
                    value = state.signUpPassword,
                    onValueChange = { onIntent(AuthIntent.SignUpPasswordChanged(it)) },
                    placeholder = { Text("請輸入密碼 (至少 6 個字元)", color = Color(0xFF8A8A8A)) },
                    visualTransformation =
                        if (state.showSignUpPassword) {
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
                        checked = state.showSignUpPassword,
                        onCheckedChange = { onIntent(AuthIntent.ToggleSignUpShowPassword(it)) },
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
                        modifier = Modifier.clickable { onIntent(AuthIntent.ToggleSignUpShowPassword(!state.showSignUpPassword)) },
                    )
                }

                AnimatedVisibility(
                    visible = state.signUpError.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Text(
                        text = state.signUpError,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onIntent(AuthIntent.SignUp) },
                    enabled = state.isSignUpButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4A91)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isSignUpLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(text = "註冊", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { onIntent(AuthIntent.SwitchPage(AuthPage.LOGIN)) }) {
                Text("已經有帳號？返回登入")
            }
        }
    }
}

/**
 * 忘記密碼預留 UI 頁面 (使用 UI 本地 remember 變數，暫不安裝 ViewModel API)
 */
@Composable
private fun ForgotPasswordPage(
    onIntent: (AuthIntent) -> Unit,
) {
    var email by remember { mutableStateOf("") }

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
                    .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "忘記密碼",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "請輸入註冊帳號時使用的 Email",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9A9A9A),
            )

            Spacer(modifier = Modifier.height(24.dp))

            AppOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("請輸入 Email", color = Color(0xFF8A8A8A)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* 保留按鈕，暫不安裝 API 邏輯 */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4A91)),
            ) {
                Text(text = "寄送重設密碼信件", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { onIntent(AuthIntent.SwitchPage(AuthPage.LOGIN)) }) {
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
    )
}
