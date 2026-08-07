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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.JoinInner
import androidx.compose.material.icons.rounded.JoinInner
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class GuildIconStatus {
    Idle,
    Notification,
    Hover,
    Selected,
}

@Composable
fun GuildIcon(
    status: GuildIconStatus,
    modifier: Modifier = Modifier,
    iconVector: ImageVector = Icons.Filled.JoinInner,
    onClick: () -> Unit = {},
) {
    val shapePercent by animateIntAsState(
        targetValue = if (status == GuildIconStatus.Hover || status == GuildIconStatus.Selected) 30 else 50,
        animationSpec = spring(Spring.StiffnessLow),
    )
    val backgroundColor by animateColorAsState(
        targetValue =
            if (status == GuildIconStatus.Selected) {
                Color(0xFF5865F2)
            } else {
                Color(0xFF2C2D31)
            },
        animationSpec = tween(200),
    )

    Box(
        modifier =
            modifier
                .height(48.dp)
                .width(72.dp)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            GuildIndicatorPill(status)
        }
        IconContainer(
            shapePercent = shapePercent,
            backgroundColor = backgroundColor,
            iconVector = iconVector,
        )
    }
}

private val GuildIndicatorPillShape =
    object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            if (size.width <= 0f || size.height <= 0f) {
                return Outline.Rectangle(Rect.Zero)
            }

            val path =
                Path().apply {
                    val r = minOf(size.width, size.height / 2f)
                    moveTo(0f, 0f)

                    arcTo(
                        rect =
                            Rect(
                                left = -r,
                                top = 0f,
                                right = r,
                                bottom = 2 * r,
                            ),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false,
                    )

                    if (size.height > 2 * r) {
                        lineTo(r, size.height - r)
                    }

                    arcTo(
                        rect =
                            Rect(
                                left = -r,
                                top = size.height - 2 * r,
                                right = r,
                                bottom = size.height,
                            ),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false,
                    )

                    lineTo(0f, 0f)
                    close()
                }
            return Outline.Generic(path)
        }
    }

@Composable
fun GuildIndicatorPill(status: GuildIconStatus) {
    val targetHeight =
        when (status) {
            GuildIconStatus.Idle -> 0.dp
            GuildIconStatus.Notification -> 8.dp
            GuildIconStatus.Hover -> 20.dp
            GuildIconStatus.Selected -> 40.dp
        }
    val height by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
    )
    val width = 4.dp
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .clip(GuildIndicatorPillShape)
                .background(Color.White),
    )
}

@Composable
fun IconContainer(
    shapePercent: Int = 50,
    backgroundColor: Color = Color.Unspecified,
    iconVector: ImageVector = Icons.Rounded.JoinInner,
    iconTint: Color = Color.White,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .then(
                    if (backgroundColor != Color.Unspecified && backgroundColor != Color.Transparent) {
                        Modifier
                            .clip(RoundedCornerShape(shapePercent))
                            .background(backgroundColor)
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
@Preview
private fun GuildIconPreview() {
    GuildIcon(status = GuildIconStatus.Hover)
}
