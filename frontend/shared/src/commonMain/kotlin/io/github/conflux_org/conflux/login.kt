package io.github.conflux_org.conflux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalMapOf

@Composable
@Preview
fun Login() {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .background(Color(0xFF00FFF4)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .background(Color(0xFF2E2E2E)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(20.dp))

                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center

                ){
                    Text(
                        text = "登入你的帳號",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "歡迎回來，請登入你的帳號",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF7D7C7C)
                    )
                }


                Spacer(modifier = Modifier.height(30.dp))

                Column (
                    modifier = Modifier.padding(horizontal = 20.dp)
                ){
                    Text(
                        text = "帳號",
                        modifier= Modifier
                            .align(Alignment.Start),
                        color = Color(0xFFFFFFFF)
                    )

                    OutlinedTextField(
                        value = account,
                        onValueChange = {
                            account = it
                            errorMessage = ""
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp),

                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),

                    )

                    Spacer(modifier = Modifier
                        .height(16.dp)
                    )

                    Text(
                        text="密碼",
                        modifier = Modifier.align(Alignment.Start),
                        color = Color(0xFFFFFFFF)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = ""
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showPassword,
                            onCheckedChange = {
                                showPassword = it
                            }
                        )

                        Text(
                            text = "顯示密碼",
                            modifier = Modifier.clickable {
                                showPassword = !showPassword
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = {

                            }
                        ) {
                            Text("忘記密碼")
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            when {
                                account.isBlank() -> {
                                    errorMessage = "請輸入帳號"
                                }

                                password.isBlank() -> {
                                    errorMessage = "請輸入密碼"
                                }

                                account == "admin" && password == "1234" -> {
                                    errorMessage = ""

                                    // 登入成功
                                    println("登入成功")

                                    // 之後在這裡加入跳轉主畫面的程式
                                }

                                else -> {
                                    errorMessage = "帳號或密碼錯誤"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A4A91)
                        )
                    ) {
                        Text(
                            text = "登入",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("還沒有帳號？")

                    TextButton(
                        onClick = {

                        }
                    ) {
                        Text("立即註冊!")
                    }
                }
            }
        }
    }
}