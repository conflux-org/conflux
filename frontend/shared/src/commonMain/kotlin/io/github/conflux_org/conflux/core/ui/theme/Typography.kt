package io.github.conflux_org.conflux.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val AppTextFieldStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp, // 鎖定行高，避免桌面端/跨平台字體計算波動
    color = Color.White,
)
