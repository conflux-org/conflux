package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.conflux_org.conflux.domain.model.Guild

@Composable
fun Sidebar(
    guilds: List<Guild> = emptyList(),
    selectedGuildId: Long? = null,
    modifier: Modifier = Modifier,
    onGuildClick: (Guild) -> Unit = {},
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
            IconContainer(iconVector = Icons.Rounded.Search, iconTint = Color(0xFFB5BFE7))
        }
        item {
            IconContainer(
                iconVector = Icons.Rounded.ChatBubbleOutline,
                iconTint = Color(0xFFB5BFE7),
            )
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
            IconContainer(iconVector = Icons.Rounded.Dashboard, iconTint = Color(0xFFB5BFE7))
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
