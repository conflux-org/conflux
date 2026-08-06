package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 使用者線上狀態標籤 (顯示於頭像右下角)
 */
@Composable
fun UserStatusBadge(
    status: UserStatus,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 14.dp,
    borderColor: Color = Color(0xFF2B2D31),
) {
    val badgeColor = when (status) {
        UserStatus.Online -> Color(0xFF23A55A)
        UserStatus.Idle -> Color(0xFFF0B232)
        UserStatus.Dnd -> Color(0xFFF23F43)
        UserStatus.Offline -> Color(0xFF80848E)
    }

    Box(
        modifier = modifier
            .size(badgeSize)
            .background(borderColor, CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (status == UserStatus.Offline) Color.Transparent else badgeColor)
            .then(
                if (status == UserStatus.Offline) {
                    Modifier.border(2.dp, badgeColor, CircleShape)
                } else {
                    Modifier
                }
            ),
    )
}

/**
 * 通用 Discord 風格使用者頭像組件 (UserAvatar)
 *
 * @param name 使用者名稱 (自動提取第一個字母作為字首)
 * @param modifier 外部 Modifier
 * @param size 頭像尺寸 (預設 40.dp)
 * @param backgroundColor 頭像底色 (預设 #5865F2)
 * @param status 線上狀態 (可選，非 null 時自動於右下角顯示 UserStatusBadge)
 * @param alpha 透明度 (用於離線成員淡化效果)
 * @param statusBorderColor 狀態徽章外圈扣邊底色
 */
@Composable
fun UserAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = Color(0xFF5865F2),
    status: UserStatus? = null,
    alpha: Float = 1.0f,
    statusBorderColor: Color = Color(0xFF2B2D31),
) {
    val initial = name.trim().take(1).uppercase()
    val fontSize = (size.value * 0.45f).sp

    Box(
        modifier = modifier.size(size),
    ) {
        // 頭像圓形主體
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = Color.White.copy(alpha = alpha),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            )
        }

        // 右下角線上狀態徽章
        if (status != null) {
            UserStatusBadge(
                status = status,
                badgeSize = (size.value * 0.35f).dp,
                borderColor = statusBorderColor,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Preview
@Composable
private fun UserAvatarPreview() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .background(Color(0xFF2B2D31))
            .padding(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(name = "Alex", size = 40.dp, status = UserStatus.Online)
        UserAvatar(name = "Taylor", size = 32.dp, status = UserStatus.Idle)
        UserAvatar(name = "Jordan", size = 32.dp, status = UserStatus.Dnd)
        UserAvatar(name = "Morgan", size = 32.dp, status = UserStatus.Offline)
    }
}
