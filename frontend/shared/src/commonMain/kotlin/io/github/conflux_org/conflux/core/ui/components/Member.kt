package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 使用者線上狀態
 */
enum class UserStatus {
    Online,   // 線上 (綠點)
    Idle,     // 閒置 (黃點)
    Dnd,      // 請勿打擾 (紅點)
    Offline   // 離線 (灰圓環)
}

/**
 * 成員資料結構
 */
data class MemberData(
    val id: String,
    val name: String,
    val nameColor: Color = Color.Unspecified,
    val avatarColor: Color = Color(0xFF5865F2),
    val status: UserStatus = UserStatus.Online,
    val customStatus: String? = null,
    val isBot: Boolean = false,
)

/**
 * 成員分類群組
 */
data class MemberCategoryData(
    val roleName: String,
    val members: List<MemberData>,
)



/**
 * Discord 風格單筆成員列
 */
@Composable
fun MemberItem(
    member: MemberData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) Color(0x1A949BA4) else Color.Transparent,
        animationSpec = tween(150),
    )

    val defaultTextColor = if (member.status == UserStatus.Offline) {
        Color(0xFF949BA4)
    } else {
        Color(0xFFF2F3F5)
    }

    val textColor = if (member.nameColor != Color.Unspecified) member.nameColor else defaultTextColor
    val alpha = if (member.status == UserStatus.Offline) 0.6f else 1.0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
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
    ) {
        // 頭像 + 線上狀態標籤 (使用通用 UserAvatar 組件)
        UserAvatar(
            name = member.name,
            size = 32.dp,
            backgroundColor = member.avatarColor,
            status = member.status,
            alpha = alpha,
            statusBorderColor = Color(0xFF2B2D31),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 名稱與自訂狀態
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = member.name,
                    color = textColor.copy(alpha = alpha),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (member.isBot) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF5865F2))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "BOT",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (!member.customStatus.isNullOrBlank()) {
                Text(
                    text = member.customStatus,
                    color = Color(0xFF949BA4).copy(alpha = alpha),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 成員分類標題列 (例如: "線上 — 3")
 */
@Composable
fun MemberCategoryHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "$title — $count".uppercase(),
        color = Color(0xFF949BA4),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

/**
 * Discord 風格右側成員列表側邊欄
 */
@Composable
fun MemberSidebar(
    categories: List<MemberCategoryData>,
    modifier: Modifier = Modifier,
    onMemberClick: (MemberData) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(Color(0xFF2B2D31))
            .padding(vertical = 8.dp),
    ) {
        categories.forEach { category ->
            item(key = "header_${category.roleName}") {
                MemberCategoryHeader(
                    title = category.roleName,
                    count = category.members.size,
                )
            }

            items(category.members, key = { it.id }) { member ->
                MemberItem(
                    member = member,
                    onClick = { onMemberClick(member) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun MemberSidebarPreview() {
    val sampleCategories = listOf(
        MemberCategoryData(
            roleName = "管理員",
            members = listOf(
                MemberData(
                    id = "1",
                    name = "Alex (Owner)",
                    nameColor = Color(0xFFF1C40F),
                    avatarColor = Color(0xFFE91E63),
                    status = UserStatus.Online,
                    customStatus = "Coding Kotlin Multiplatform",
                ),
                MemberData(
                    id = "2",
                    name = "ConfluxBot",
                    avatarColor = Color(0xFF5865F2),
                    status = UserStatus.Online,
                    isBot = true,
                )
            )
        ),
        MemberCategoryData(
            roleName = "線上",
            members = listOf(
                MemberData(
                    id = "3",
                    name = "Taylor",
                    avatarColor = Color(0xFF2ECC71),
                    status = UserStatus.Idle,
                    customStatus = "AFK - Getting coffee",
                ),
                MemberData(
                    id = "4",
                    name = "Jordan",
                    avatarColor = Color(0xFF9B59B6),
                    status = UserStatus.Dnd,
                    customStatus = "Do Not Disturb / In a Meeting",
                )
            )
        ),
        MemberCategoryData(
            roleName = "離線",
            members = listOf(
                MemberData(
                    id = "5",
                    name = "Morgan",
                    avatarColor = Color(0xFF95A5A6),
                    status = UserStatus.Offline,
                )
            )
        )
    )

    MemberSidebar(categories = sampleCategories)
}
