package io.github.conflux_org.conflux.features.main.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.conflux_org.conflux.core.ui.components.ChannelStatus
import io.github.conflux_org.conflux.core.ui.components.MemberCategoryData
import io.github.conflux_org.conflux.core.ui.components.MemberData
import io.github.conflux_org.conflux.core.ui.components.MemberSidebar
import io.github.conflux_org.conflux.core.ui.components.MessageArea
import io.github.conflux_org.conflux.core.ui.components.MessageData
import io.github.conflux_org.conflux.core.ui.components.Sidebar
import io.github.conflux_org.conflux.core.ui.components.TextChannelItem
import io.github.conflux_org.conflux.core.ui.components.UserStatus

/**
 * Conflux 主畫面 - 組合所有 Discord 風格組件 (Guild Sidebar, Channel List, Message Feed, Member Sidebar)
 */
@Composable
fun MainScreen() {
    // 假資料狀態 (State)
    var selectedChannel by remember { mutableStateOf("general") }

    val channelList =
        remember {
            listOf("welcome", "general", "announcements", "random-chat")
        }

    val sampleMessages =
        remember {
            mutableStateListOf(
                MessageData(
                    id = "1",
                    senderName = "Alex",
                    avatarColor = Color(0xFFE91E63),
                    timestamp = "今天 17:20",
                    content = "大家好！歡迎來到 Conflux 聊天室！",
                ),
                MessageData(
                    id = "2",
                    senderName = "ConfluxBot",
                    avatarColor = Color(0xFF5865F2),
                    timestamp = "今天 17:22",
                    content = "系統提示：頻道已被成功創建，狀態正常。",
                    isBot = true,
                ),
                MessageData(
                    id = "3",
                    senderName = "Taylor",
                    avatarColor = Color(0xFF2ECC71),
                    timestamp = "今天 17:25",
                    content = "這個介面設計完全就是 Discord 的感覺！真的很讚 🔥",
                ),
            )
        }

    val sampleCategories =
        remember {
            listOf(
                MemberCategoryData(
                    roleName = "管理員",
                    members =
                        listOf(
                            MemberData(
                                id = "m1",
                                name = "Alex (Owner)",
                                nameColor = Color(0xFFF1C40F),
                                avatarColor = Color(0xFFE91E63),
                                status = UserStatus.Online,
                                customStatus = "Coding KMP UI",
                            ),
                            MemberData(
                                id = "m2",
                                name = "ConfluxBot",
                                avatarColor = Color(0xFF5865F2),
                                status = UserStatus.Online,
                                isBot = true,
                            ),
                        ),
                ),
                MemberCategoryData(
                    roleName = "線上成員",
                    members =
                        listOf(
                            MemberData(
                                id = "m3",
                                name = "Taylor",
                                avatarColor = Color(0xFF2ECC71),
                                status = UserStatus.Idle,
                                customStatus = "AFK - Getting coffee",
                            ),
                            MemberData(
                                id = "m4",
                                name = "Jordan",
                                avatarColor = Color(0xFF9B59B6),
                                status = UserStatus.Dnd,
                                customStatus = "Do Not Disturb / Busy",
                            ),
                        ),
                ),
                MemberCategoryData(
                    roleName = "離線成員",
                    members =
                        listOf(
                            MemberData(
                                id = "m5",
                                name = "Morgan",
                                avatarColor = Color(0xFF95A5A6),
                                status = UserStatus.Offline,
                            ),
                        ),
                ),
            )
        }

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1F22)),
    ) {
        // 1. 最左側伺服器 Guild 側邊欄 (寬度 72.dp)
        Sidebar()

        // 2. 頻道列表欄 (寬度 240.dp)
        Column(
            modifier =
                Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2B2D31)),
        ) {
            // 伺服器名稱 Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Conflux Server",
                    color = Color(0xFFF2F3F5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFF1F2023),
            )

            // 頻道清單
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                items(channelList) { channelName ->
                    val status =
                        when {
                            channelName == selectedChannel -> ChannelStatus.Selected
                            channelName == "announcements" -> ChannelStatus.Unread
                            else -> ChannelStatus.Idle
                        }

                    TextChannelItem(
                        name = channelName,
                        status = status,
                        onClick = { selectedChannel = channelName },
                    )
                }
            }
        }

        // 3. 中央訊息區塊 (權重 1f 佔滿剩餘寬度)
        MessageArea(
            channelName = selectedChannel,
            messages = sampleMessages,
            modifier = Modifier.weight(1f),
            onSendMessage = { text ->
                sampleMessages.add(
                    MessageData(
                        id = (sampleMessages.size + 1).toString(),
                        senderName = "You",
                        avatarColor = Color(0xFF3498DB),
                        timestamp = "剛剛",
                        content = text,
                    ),
                )
            },
        )

        // 4. 右側成員側邊欄 (寬度 240.dp)
        MemberSidebar(
            categories = sampleCategories,
        )
    }
}

@Preview
@Composable
fun Preview() {
    MainScreen()
}
