package io.github.conflux_org.conflux.features.main.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.conflux_org.conflux.core.ui.components.ServerIcon
import io.github.conflux_org.conflux.core.ui.components.ServerIconStatus

@Composable
fun MainScreen(
    onLogoutClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF141416))
                .safeContentPadding(),
    ) {
        // Top Navigation Header
        TopAppBarHeader(onLogoutClick = onLogoutClick)

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Left Server Icons Bar (Discord-style)
            ServerIconsRail()

            // Left Sidebar
            SidebarMenu()

            // Main Content Area
            MainContentDashboard()
        }
    }
}

@Composable
private fun ServerIconsRail() {
    var selectedIndex by remember { mutableStateOf(0) }

    Column(
        modifier =
            Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E1F22))
                .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ServerIcon(
            status = if (selectedIndex == 0) ServerIconStatus.Selected else ServerIconStatus.Idle,
            iconVector = Icons.Filled.Home,
            onClick = { selectedIndex = 0 },
        )

        Spacer(modifier = Modifier.height(8.dp))

        ServerIcon(
            status = if (selectedIndex == 1) ServerIconStatus.Selected else ServerIconStatus.Notification,
            iconVector = Icons.Filled.Hub,
            onClick = { selectedIndex = 1 },
        )

        Spacer(modifier = Modifier.height(8.dp))

        ServerIcon(
            status = if (selectedIndex == 2) ServerIconStatus.Selected else ServerIconStatus.Group,
            iconVector = Icons.Filled.CorporateFare,
            onClick = { selectedIndex = 2 },
        )
    }
}

@Composable
private fun TopAppBarHeader(onLogoutClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF1F1F23))
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Conflux",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6C63FF),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier =
                Modifier
                    .background(Color(0xFF2D2D35), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(text = "Workspace: Default", color = Color.White, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Profile & Logout
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(Color(0xFF4A4A91), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("U", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "登出",
                color = Color(0xFFFF5252),
                modifier = Modifier.clickable { onLogoutClick() },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SidebarMenu() {
    Column(
        modifier =
            Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(Color(0xFF1A1A1E))
                .padding(16.dp),
    ) {
        Text("主選單", color = Color(0xFF8A8A8A), style = MaterialTheme.typography.labelMedium)

        Spacer(modifier = Modifier.height(12.dp))

        SidebarItem(label = "📊 Dashboard", isSelected = true)
        SidebarItem(label = "📁 Repositories", isSelected = false)
        SidebarItem(label = "💬 Channels", isSelected = false)
        SidebarItem(label = "⚙️ Settings", isSelected = false)
    }
}

@Composable
private fun SidebarItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit = {},
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(
                    color = if (isSelected) Color(0xFF2D2D35) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                ).clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MainContentDashboard() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        item {
            Text(
                text = "歡迎回來！",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Text(
                text = "這裏是您的工作區總覽與專案管理中心",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9A9A9A),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Repositories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF23232A)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                ) {
                    Text("Conflux Monorepo", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Branch: master", color = Color(0xFF8A8A8A), style = MaterialTheme.typography.bodySmall)
                    Text("Status: Active", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
