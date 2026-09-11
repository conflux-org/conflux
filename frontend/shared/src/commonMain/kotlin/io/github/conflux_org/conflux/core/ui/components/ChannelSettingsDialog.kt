package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.conflux_org.conflux.domain.model.Channel
import io.github.conflux_org.conflux.domain.model.ChannelOverwrite
import io.github.conflux_org.conflux.domain.model.OverwriteTargetType
import io.github.conflux_org.conflux.domain.model.PermissionFlags
import io.github.conflux_org.conflux.domain.model.Role

private data class ChannelPermissionItem(
    val flag: Long,
    val title: String,
    val description: String,
)

private val CHANNEL_PERMISSIONS =
    listOf(
        ChannelPermissionItem(
            flag = PermissionFlags.VIEW_CHANNEL,
            title = "檢視頻道",
            description = "允許或禁止該身分組/成員在此頻道中查看與讀取訊息。",
        ),
        ChannelPermissionItem(
            flag = PermissionFlags.SEND_MESSAGES,
            title = "發送訊息",
            description = "允許或禁止該身分組/成員在此頻道中發送文字訊息。",
        ),
        ChannelPermissionItem(
            flag = PermissionFlags.MANAGE_CHANNELS,
            title = "管理頻道",
            description = "允許或禁止編輯此頻道設定與刪除此頻道。",
        ),
        ChannelPermissionItem(
            flag = PermissionFlags.MANAGE_MESSAGES,
            title = "管理訊息",
            description = "允許或禁止刪除其他人的訊息。",
        ),
    )

private enum class TriStatePermission {
    DENY,
    INHERIT,
    ALLOW,
}

/**
 * 頻道權限覆寫之目標實體 (身分組或個別成員)
 */
sealed interface OverwriteTarget {
    val id: Long
    val name: String
    val type: OverwriteTargetType

    data class RoleTarget(
        val role: Role,
    ) : OverwriteTarget {
        override val id: Long = role.id
        override val name: String = role.name
        override val type: OverwriteTargetType = OverwriteTargetType.ROLE
    }

    data class MemberTarget(
        val member: MemberData,
    ) : OverwriteTarget {
        override val id: Long = member.id.removePrefix("m").toLongOrNull() ?: 1L
        override val name: String = member.name
        override val type: OverwriteTargetType = OverwriteTargetType.MEMBER
    }
}

@Composable
fun ChannelSettingsDialog(
    channel: Channel,
    roles: List<Role>,
    members: List<MemberData> = emptyList(),
    overwrites: List<ChannelOverwrite>,
    isLoading: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    canManageChannels: Boolean = true,
    onSetOverwrite: (targetType: OverwriteTargetType, targetId: Long, allow: Long, deny: Long) -> Unit,
    onDeleteOverwrite: (targetType: OverwriteTargetType, targetId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultTarget =
        remember(roles) {
            val everyoneRole = roles.firstOrNull { it.isEveryone } ?: roles.firstOrNull()
            everyoneRole?.let { OverwriteTarget.RoleTarget(it) }
        }

    var selectedTarget by remember(roles) { mutableStateOf<OverwriteTarget?>(defaultTarget) }
    var currentAllow by remember { mutableStateOf(0L) }
    var currentDeny by remember { mutableStateOf(0L) }
    var showAddTargetDropdown by remember { mutableStateOf(false) }

    // 找出所有已有覆寫的成員清單
    val memberOverwrites = overwrites.filter { it.targetType == OverwriteTargetType.MEMBER }
    val membersWithOverwrites =
        remember(memberOverwrites, members, selectedTarget) {
            val list = mutableListOf<MemberData>()
            // 已經有覆寫紀錄的成員
            memberOverwrites.forEach { ow ->
                val found =
                    members.find { (it.id.removePrefix("m").toLongOrNull() ?: 1L) == ow.targetId }
                        ?: MemberData(
                            id = "m${ow.targetId}",
                            name = "成員 #${ow.targetId}",
                            avatarColor = Color(0xFF5865F2),
                        )
                list.add(found)
            }
            // 若目前選取的目標是成員且尚未在清單中，也納入顯示
            if (selectedTarget is OverwriteTarget.MemberTarget) {
                val current = (selectedTarget as OverwriteTarget.MemberTarget).member
                if (list.none { it.id == current.id }) {
                    list.add(current)
                }
            }
            list.distinctBy { it.id }
        }

    LaunchedEffect(selectedTarget, overwrites) {
        val ow =
            selectedTarget?.let { target ->
                overwrites.firstOrNull { it.targetType == target.type && it.targetId == target.id }
            }
        currentAllow = ow?.allow ?: 0L
        currentDeny = ow?.deny ?: 0L
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF313338)),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 左側身分組與個別成員清單
                Column(
                    modifier =
                        Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF2B2D31))
                            .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "身分組與成員",
                            color = Color(0xFF949BA4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        if (canManageChannels) {
                            Box {
                                IconButton(
                                    onClick = { showAddTargetDropdown = true },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "新增身分組或成員",
                                        tint = Color(0xFF949BA4),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }

                                DropdownMenu(
                                    expanded = showAddTargetDropdown,
                                    onDismissRequest = { showAddTargetDropdown = false },
                                    modifier = Modifier.background(Color(0xFF2B2D31)),
                                ) {
                                    // 身分組區塊
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "身分組",
                                                color = Color(0xFF949BA4),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        },
                                        onClick = {},
                                        enabled = false,
                                    )
                                    roles.forEach { role ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = role.name,
                                                    color = Color(0xFFF2F3F5),
                                                    fontSize = 13.sp,
                                                )
                                            },
                                            onClick = {
                                                selectedTarget = OverwriteTarget.RoleTarget(role)
                                                showAddTargetDropdown = false
                                            },
                                        )
                                    }

                                    HorizontalDivider(
                                        color = Color(0xFF3F4147),
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )

                                    // 個別成員區塊
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "個別成員",
                                                color = Color(0xFF949BA4),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        },
                                        onClick = {},
                                        enabled = false,
                                    )
                                    if (members.isEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "無可用成員",
                                                    color = Color(0xFF80848E),
                                                    fontSize = 12.sp,
                                                )
                                            },
                                            onClick = { showAddTargetDropdown = false },
                                        )
                                    } else {
                                        members.forEach { member ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        UserAvatar(
                                                            name = member.name,
                                                            size = 20.dp,
                                                            backgroundColor = member.avatarColor,
                                                            status = member.status,
                                                            statusBorderColor = Color(0xFF2B2D31),
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = member.name,
                                                            color = Color(0xFFF2F3F5),
                                                            fontSize = 13.sp,
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedTarget = OverwriteTarget.MemberTarget(member)
                                                    showAddTargetDropdown = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // 身分組區塊標題
                        item {
                            Text(
                                text = "身分組",
                                color = Color(0xFF80848E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }

                        // 身分組項目
                        items(roles, key = { "role_${it.id}" }) { role ->
                            val isSelected =
                                (selectedTarget as? OverwriteTarget.RoleTarget)?.role?.id == role.id
                            val hasOverwrite =
                                overwrites.any {
                                    it.targetType == OverwriteTargetType.ROLE && it.targetId == role.id
                                }
                            val bg = if (isSelected) Color(0xFF404249) else Color.Transparent

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(bg)
                                        .clickable { selectedTarget = OverwriteTarget.RoleTarget(role) }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = role.name,
                                    color = if (isSelected) Color(0xFFF2F3F5) else Color(0xFF949BA4),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                )
                                if (hasOverwrite) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF5865F2)),
                                    )
                                }
                            }
                        }

                        // 成員區塊標題
                        item {
                            Text(
                                text = "個別成員",
                                color = Color(0xFF80848E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }

                        // 成員項目清單
                        if (membersWithOverwrites.isEmpty()) {
                            item {
                                Text(
                                    text = "尚未加入個別成員覆寫",
                                    color = Color(0xFF6D6F78),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        } else {
                            items(membersWithOverwrites, key = { "member_${it.id}" }) { member ->
                                val memberId = member.id.removePrefix("m").toLongOrNull() ?: 1L
                                val isSelected =
                                    (selectedTarget as? OverwriteTarget.MemberTarget)?.id == memberId
                                val hasOverwrite =
                                    overwrites.any {
                                        it.targetType == OverwriteTargetType.MEMBER && it.targetId == memberId
                                    }
                                val bg = if (isSelected) Color(0xFF404249) else Color.Transparent

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(bg)
                                            .clickable {
                                                selectedTarget = OverwriteTarget.MemberTarget(member)
                                            }.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        UserAvatar(
                                            name = member.name,
                                            size = 22.dp,
                                            backgroundColor = member.avatarColor,
                                            status = member.status,
                                            statusBorderColor = Color(0xFF2B2D31),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = member.name,
                                            color = if (isSelected) Color(0xFFF2F3F5) else Color(0xFF949BA4),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                    if (hasOverwrite) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF5865F2)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 右側三態權限設定區
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "頻道設定 - #${channel.name}",
                                color = Color(0xFFF2F3F5),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (selectedTarget != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                val targetDescription =
                                    when (selectedTarget) {
                                        is OverwriteTarget.RoleTarget -> "覆寫身分組：${selectedTarget?.name}"
                                        is OverwriteTarget.MemberTarget -> "覆寫個別成員：${selectedTarget?.name}"
                                        null -> ""
                                    }
                                Text(
                                    text = targetDescription,
                                    color = Color(0xFF949BA4),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "關閉",
                                tint = Color(0xFFB5BAC1),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (selectedTarget == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("請選擇身分組或成員以設定頻道覆寫", color = Color(0xFF949BA4), fontSize = 15.sp)
                        }
                    } else {
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CHANNEL_PERMISSIONS.forEach { item ->
                                val triState =
                                    when {
                                        (currentAllow and item.flag) == item.flag -> TriStatePermission.ALLOW
                                        (currentDeny and item.flag) == item.flag -> TriStatePermission.DENY
                                        else -> TriStatePermission.INHERIT
                                    }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                        Text(
                                            text = item.title,
                                            color = Color(0xFFF2F3F5),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.description,
                                            color = Color(0xFF949BA4),
                                            fontSize = 13.sp,
                                        )
                                    }

                                    // 三態 Segmented Button 切換 (拒絕 ✕ / 繼承 ─ / 允許 ✓)
                                    Row(
                                        modifier =
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2B2D31))
                                                .padding(2.dp),
                                    ) {
                                        // 拒絕按鈕 (✕)
                                        val isDeny = triState == TriStatePermission.DENY
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(width = 38.dp, height = 30.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (isDeny) Color(0xFFDA373C) else Color.Transparent)
                                                    .clickable(enabled = canManageChannels) {
                                                        currentDeny = currentDeny or item.flag
                                                        currentAllow = currentAllow and item.flag.inv()
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "拒絕",
                                                tint = if (isDeny) Color.White else Color(0xFF949BA4),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }

                                        // 繼承按鈕 (─)
                                        val isInherit = triState == TriStatePermission.INHERIT
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(width = 38.dp, height = 30.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (isInherit) Color(0xFF4E5058) else Color.Transparent)
                                                    .clickable(enabled = canManageChannels) {
                                                        currentAllow = currentAllow and item.flag.inv()
                                                        currentDeny = currentDeny and item.flag.inv()
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Remove,
                                                contentDescription = "繼承",
                                                tint = if (isInherit) Color.White else Color(0xFF949BA4),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }

                                        // 允許按鈕 (✓)
                                        val isAllow = triState == TriStatePermission.ALLOW
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(width = 38.dp, height = 30.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (isAllow) Color(0xFF23A55A) else Color.Transparent)
                                                    .clickable(enabled = canManageChannels) {
                                                        currentAllow = currentAllow or item.flag
                                                        currentDeny = currentDeny and item.flag.inv()
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "允許",
                                                tint = if (isAllow) Color.White else Color(0xFF949BA4),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFA777C),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        // 底部按鈕
                        val existingOverwrite =
                            selectedTarget?.let { target ->
                                overwrites.firstOrNull { it.targetType == target.type && it.targetId == target.id }
                            }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (canManageChannels && existingOverwrite != null) {
                                Button(
                                    onClick = {
                                        selectedTarget?.let { target ->
                                            onDeleteOverwrite(target.type, target.id)
                                        }
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFDA373C),
                                            contentColor = Color.White,
                                        ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "刪除覆寫",
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("重置此覆寫", fontSize = 13.sp)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text("取消", color = Color(0xFF949BA4), fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        selectedTarget?.let { target ->
                                            onSetOverwrite(
                                                target.type,
                                                target.id,
                                                currentAllow,
                                                currentDeny,
                                            )
                                        }
                                    },
                                    enabled = canManageChannels && !isSaving,
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF5865F2),
                                            contentColor = Color.White,
                                        ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Text("儲存變更", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
