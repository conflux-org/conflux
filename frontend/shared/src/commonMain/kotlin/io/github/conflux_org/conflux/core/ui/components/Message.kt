package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 訊息資料模型
 */
data class MessageData(
    val id: String,
    val senderName: String,
    val avatarColor: Color = Color(0xFF5865F2),
    val timestamp: String,
    val content: String,
    val isBot: Boolean = false,
)

/**
 * Discord 風格單筆訊息列
 */
@Composable
fun MessageItem(
    message: MessageData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 使用者大頭貼 (使用通用 UserAvatar 組件)
        UserAvatar(
            name = message.senderName,
            size = 40.dp,
            backgroundColor = message.avatarColor,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            // 發送者資訊列 (姓名, BOT標籤, 時間)
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.senderName,
                    color = Color(0xFFF2F3F5),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                if (message.isBot) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF5865F2))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "BOT",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = message.timestamp,
                    color = Color(0xFF949BA4),
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 訊息內容
            Text(
                text = message.content,
                color = Color(0xFFDBDEE1),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

/**
 * Discord 風格底部訊息輸入框
 */
@Composable
fun MessageInputField(
    channelName: String,
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit = {},
) {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF383A40))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 附加檔案按鈕 (+)
            Icon(
                imageVector = Icons.Rounded.AddCircle,
                contentDescription = "Attach",
                tint = Color(0xFFB5BAC1),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { },
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 訊息輸入區域
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "發送訊息至 #$channelName",
                        color = Color(0xFF80848E),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        color = Color(0xFFF2F3F5),
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(Color(0xFFF2F3F5)),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右側按鈕: Emoji 與 發送
            Icon(
                imageVector = Icons.Rounded.EmojiEmotions,
                contentDescription = "Emoji",
                tint = Color(0xFFB5BAC1),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { },
            )

            if (text.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = "Send",
                    tint = Color(0xFF5865F2),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            onSendMessage(text)
                            text = ""
                        },
                )
            }
        }
    }
}

/**
 * Discord 風格頂部頻道標題列
 */
@Composable
fun MessageHeaderBar(
    channelName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF313338))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Tag,
                contentDescription = "Channel Icon",
                tint = Color(0xFF80848E),
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = channelName,
                color = Color(0xFFF2F3F5),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFF1F2023),
        )
    }
}

/**
 * Discord 風格 Message 區域完整畫面組件
 *
 * @param channelName 頻道名稱
 * @param messages 訊息列表
 * @param modifier 外部 Modifier
 * @param onSendMessage 發送訊息回調
 */
@Composable
fun MessageArea(
    channelName: String = "general",
    messages: List<MessageData> = emptyList(),
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF313338)),
    ) {
        // 頂部標題列
        MessageHeaderBar(channelName = channelName)

        val listState = androidx.compose.foundation.lazy.rememberLazyListState()

        androidx.compose.runtime.LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        // 訊息列表區域 (對齊底端，最新訊息在最下方)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            items(messages, key = { it.id }) { message ->
                MessageItem(message = message)
            }
        }

        // 底部輸入框
        MessageInputField(
            channelName = channelName,
            onSendMessage = onSendMessage,
        )
    }
}

@Preview
@Composable
private fun MessageAreaPreview() {
    val sampleMessages = listOf(
        MessageData(
            id = "1",
            senderName = "Alex",
            avatarColor = Color(0xFF5865F2),
            timestamp = "今天 17:20",
            content = "大家好！歡迎來到 Conflux 聊天室！",
        ),
        MessageData(
            id = "2",
            senderName = "ConfluxBot",
            avatarColor = Color(0xFF23A55A),
            timestamp = "今天 17:22",
            content = "系統提示：目前通道運行正常。",
            isBot = true,
        ),
        MessageData(
            id = "3",
            senderName = "Taylor",
            avatarColor = Color(0xFFF0B232),
            timestamp = "今天 17:25",
            content = "這個 Discord 風格的 UI 看起來真棒 👍",
        ),
    )

    MessageArea(
        channelName = "general",
        messages = sampleMessages,
        modifier = Modifier.fillMaxSize(),
    )
}
