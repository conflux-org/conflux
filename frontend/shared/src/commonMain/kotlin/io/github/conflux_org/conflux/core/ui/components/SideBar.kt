package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.conflux_org.conflux.domain.model.Guild

@Composable
fun Sidebar(
    guilds: List<Guild> = emptyList(),
    selectedGuildId: Long? = null,
    modifier: Modifier = Modifier,
    onGuildClick: (Guild) -> Unit = {},
    onAddGuildClick: () -> Unit = {},
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxHeight()
                .width(72.dp)
                .background(Color(0xFF1C1C1F)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            SidebarActionIcon(iconVector = Icons.Rounded.Search)
        }
        item {
            SidebarActionIcon(iconVector = Icons.Rounded.ChatBubbleOutline)
        }
        item {
            HorizontalDivider(
                modifier =
                    Modifier
                        .width(32.dp)
                        .padding(vertical = 6.dp)
                        .clip(CircleShape),
                thickness = 2.dp,
                color = Color(0xFF2C2D31),
            )
        }
        item {
            SidebarActionIcon(iconVector = Icons.Rounded.Dashboard)
        }
        items(guilds, key = { it.id }) { guild ->
            val status =
                if (guild.id == selectedGuildId) {
                    GuildIconStatus.Selected
                } else {
                    GuildIconStatus.Idle
                }
            GuildIcon(
                status = status,
                onClick = { onGuildClick(guild) },
            )
        }
        item {
            SidebarActionIcon(
                iconVector = Icons.Rounded.Add,
                iconTint = Color(0xFF23A55A),
                onClick = onAddGuildClick,
            )
        }
    }
}

@Composable
private fun SidebarActionIcon(
    iconVector: ImageVector,
    iconTint: Color = Color(0xFFB5BFE7),
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) Color(0xFF2C2D31) else Color.Transparent,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
    )

    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).hoverable(interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = if (isHovered) Color.White else iconTint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1F)
@Composable
fun SidebarPreview() {
    Sidebar(
        guilds =
            listOf(
                Guild(1L, "Conflux"),
                Guild(2L, "Gaming"),
            ),
        selectedGuildId = 1L,
    )
}
