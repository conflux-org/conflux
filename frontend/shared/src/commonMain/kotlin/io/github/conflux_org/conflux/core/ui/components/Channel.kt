package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ChannelStatus {
    Idle, Hover, Selected, Unread
}

/**
 * Discord 風格文字頻道組件 (Text Channel Item)
 *
 * @param name 頻道名稱 (例如 "general", "welcome")
 * @param status 頻道狀態 (Idle, Hover, Selected, Unread)
 * @param modifier 外部 Modifier
 * @param onClick 點擊頻道時的回調函式
 */
@Composable
fun TextChannelItem(
    name: String,
    status: ChannelStatus = ChannelStatus.Idle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            ChannelStatus.Selected -> Color(0x33949BA4) // Discord 選中背景 (約 20% 灰度)
            ChannelStatus.Hover -> Color(0x1A949BA4)    // Discord 懸停背景 (約 10% 灰度)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
    )

    val contentColor by animateColorAsState(
        targetValue = when (status) {
            ChannelStatus.Selected, ChannelStatus.Unread -> Color(0xFFF2F3F5)
            ChannelStatus.Hover -> Color(0xFFDBDEE1)
            ChannelStatus.Idle -> Color(0xFF949BA4)
        },
        animationSpec = tween(durationMillis = 150),
    )

    val iconColor by animateColorAsState(
        targetValue = when (status) {
            ChannelStatus.Selected, ChannelStatus.Unread -> Color(0xFFF2F3F5)
            ChannelStatus.Hover -> Color(0xFFDBDEE1)
            ChannelStatus.Idle -> Color(0xFF80848E)
        },
        animationSpec = tween(durationMillis = 150),
    )

    val fontWeight = if (status == ChannelStatus.Selected || status == ChannelStatus.Unread) {
        FontWeight.Bold
    } else {
        FontWeight.Medium
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = Icons.Rounded.Tag,
            contentDescription = "Text Channel",
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = name,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun TextChannelItemPreview() {
    Column(
        modifier = Modifier
            .width(240.dp)
            .background(Color(0xFF2B2D31))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextChannelItem(name = "welcome", status = ChannelStatus.Idle)
        TextChannelItem(name = "general", status = ChannelStatus.Selected)
        TextChannelItem(name = "announcements", status = ChannelStatus.Unread)
        TextChannelItem(name = "random-chat", status = ChannelStatus.Hover)
    }
}
