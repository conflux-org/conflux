package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.JoinInner
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class ServerIconStatus {
    Idle, Notification, Hover, Selected
}

@Composable
fun ServerIcon(
    status: ServerIconStatus,
    modifier: Modifier = Modifier,
    iconVector: ImageVector = Icons.Filled.JoinInner,
    onClick: () -> Unit = {},
) {
    val shapePercent by animateIntAsState(
        targetValue = if (status == ServerIconStatus.Hover || status == ServerIconStatus.Selected) 30 else 50,
        animationSpec = spring(Spring.StiffnessLow),
    )
    val backgroundColor by animateColorAsState(
        targetValue =
            if (status == ServerIconStatus.Selected) {
                Color(0xFF5865F2)
            } else {
                Color(0xFF2C2D31)
            },
        animationSpec = tween(200),
    )

    Row(
        modifier =
            modifier
                .height(48.dp)
                .width(72.dp)
                .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServerIndicatorPill(status)
        Spacer(Modifier.width(8.dp))
        IconContainer(
            shapePercent = shapePercent,
            backgroundColor = backgroundColor,
            iconVector = iconVector,
        )
    }
}

@Composable
fun ServerIndicatorPill(status: ServerIconStatus) {
    val targetHeight =
        when (status) {
            ServerIconStatus.Idle -> 0.dp
            ServerIconStatus.Notification -> 8.dp
            ServerIconStatus.Hover -> 20.dp
            ServerIconStatus.Selected -> 40.dp
        }
    val height by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
    )
    val width = 8
    Box(
        modifier =
            Modifier
                .width(width.dp)
                .height(height)
                .offset(x = (-width / 2).dp)
                .clip(CircleShape)
                .background(Color.White),
    )
}

@Composable
private fun IconContainer(
    shapePercent: Int = 50,
    backgroundColor: Color = Color(0xFF2C2D31),
    iconVector: ImageVector = Icons.Filled.JoinInner,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(shapePercent))
                .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
@Preview
private fun ServerIconPreview() {
    ServerIcon(status = ServerIconStatus.Hover)
}
