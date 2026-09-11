package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
    var showAddTargetDialog by remember { mutableStateOf(false) }

    // 計算已有覆寫的身分組清單 (或已選中的身分組，@everyone 永遠置頂)
    val rolesWithOverwrites =
        remember(roles, overwrites, selectedTarget) {
            val list = mutableListOf<Role>()
            val everyoneRole = roles.find { it.isEveryone } ?: roles.firstOrNull()
            if (everyoneRole != null) {
                list.add(everyoneRole)
            }
            val roleOverwrites = overwrites.filter { it.targetType == OverwriteTargetType.ROLE }
            roleOverwrites.forEach { ow ->
                val found = roles.find { it.id == ow.targetId }
                if (found != null && !found.isEveryone) {
                    list.add(found)
                }
            }
            if (selectedTarget is OverwriteTarget.RoleTarget) {
                val current = (selectedTarget as OverwriteTarget.RoleTarget).role
                if (list.none { it.id == current.id }) {
                    list.add(current)
                }
            }
            list.distinctBy { it.id }
        }

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
                            IconButton(
                                onClick = { showAddTargetDialog = true },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "新增身分組或成員",
                                    tint = Color(0xFF949BA4),
                                    modifier = Modifier.size(18.dp),
                                )
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

                        // 身分組項目 (僅列出具備覆寫之身分組與預設 @everyone)
                        items(rolesWithOverwrites, key = { "role_${it.id}" }) { role ->
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
                                if (hasOverwrite && !role.isEveryone) {
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
                                            val everyoneRole = roles.firstOrNull { it.isEveryone }
                                            if (everyoneRole != null) {
                                                selectedTarget = OverwriteTarget.RoleTarget(everyoneRole)
                                            }
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
                                    val removeBtnText =
                                        when (selectedTarget?.type) {
                                            OverwriteTargetType.ROLE -> {
                                                val isEveryone = roles.firstOrNull { it.id == selectedTarget?.id }?.isEveryone == true
                                                if (isEveryone) "重置此覆寫" else "移除身分組權限"
                                            }
                                            OverwriteTargetType.MEMBER -> "移除成員權限"
                                            null -> "移除權限"
                                        }
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = removeBtnText,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(removeBtnText, fontSize = 13.sp)
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

    // 搜尋與新增身分組或成員覆寫對話框
    if (showAddTargetDialog) {
        var targetSearchQuery by remember { mutableStateOf("") }
        val configuredRoleIds =
            remember(overwrites) {
                overwrites.filter { it.targetType == OverwriteTargetType.ROLE }.map { it.targetId }.toSet()
            }
        val matchingRoles =
            remember(roles, configuredRoleIds, targetSearchQuery) {
                val availableRoles = roles.filter { !it.isEveryone && it.id !in configuredRoleIds }
                if (targetSearchQuery.isBlank()) {
                    availableRoles
                } else {
                    availableRoles.filter { it.name.contains(targetSearchQuery, ignoreCase = true) }
                }
            }
        val configuredMemberIds =
            remember(overwrites) {
                overwrites.filter { it.targetType == OverwriteTargetType.MEMBER }.map { it.targetId }.toSet()
            }
        val matchingMembers =
            remember(members, configuredMemberIds, targetSearchQuery) {
                val availableMembers =
                    members.filter {
                        val uid = it.id.removePrefix("m").toLongOrNull() ?: 1L
                        uid !in configuredMemberIds
                    }
                if (targetSearchQuery.isBlank()) {
                    availableMembers
                } else {
                    availableMembers.filter { it.name.contains(targetSearchQuery, ignoreCase = true) }
                }
            }

        Dialog(
            onDismissRequest = { showAddTargetDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.5f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF313338))
                        .padding(20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "新增身分組或成員覆寫",
                                color = Color(0xFFF2F3F5),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "頻道：#${channel.name}",
                                color = Color(0xFF949BA4),
                                fontSize = 13.sp,
                            )
                        }
                        IconButton(
                            onClick = { showAddTargetDialog = false },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "關閉",
                                tint = Color(0xFFB5BAC1),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = targetSearchQuery,
                        onValueChange = { targetSearchQuery = it },
                        placeholder = { Text("搜尋身分組或成員名稱...", color = Color(0xFF949BA4), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "搜尋",
                                tint = Color(0xFF949BA4),
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        trailingIcon = {
                            if (targetSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { targetSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "清除搜尋",
                                        tint = Color(0xFF949BA4),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFF2F3F5),
                                unfocusedTextColor = Color(0xFFDBDEE1),
                                focusedBorderColor = Color(0xFF5865F2),
                                unfocusedBorderColor = Color(0xFF4E5058),
                                focusedContainerColor = Color(0xFF1E1F22),
                                unfocusedContainerColor = Color(0xFF1E1F22),
                            ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (matchingRoles.isEmpty() && matchingMembers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "找不到符合「$targetSearchQuery」的身分組或成員",
                                color = Color(0xFF949BA4),
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (matchingRoles.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "身分組",
                                        color = Color(0xFF80848E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                                items(matchingRoles, key = { "role_${it.id}" }) { role ->
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2B2D31))
                                                .clickable {
                                                    selectedTarget = OverwriteTarget.RoleTarget(role)
                                                    showAddTargetDialog = false
                                                }.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = role.name,
                                            color = Color(0xFFF2F3F5),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }

                            if (matchingMembers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "個別成員",
                                        color = Color(0xFF80848E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                    )
                                }
                                items(matchingMembers, key = { "member_${it.id}" }) { member ->
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2B2D31))
                                                .clickable {
                                                    selectedTarget = OverwriteTarget.MemberTarget(member)
                                                    showAddTargetDialog = false
                                                }.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        UserAvatar(
                                            name = member.name,
                                            size = 28.dp,
                                            backgroundColor = member.avatarColor,
                                            status = member.status,
                                            statusBorderColor = Color(0xFF2B2D31),
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = member.name,
                                                    color = Color(0xFFF2F3F5),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                if (member.isBot) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier =
                                                            Modifier
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
                                                    color = Color(0xFF949BA4),
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { showAddTargetDialog = false },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF404249),
                                    contentColor = Color(0xFFF2F3F5),
                                ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            Text(text = "取消", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
