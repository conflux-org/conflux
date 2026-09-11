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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.conflux_org.conflux.domain.model.PermissionFlags
import io.github.conflux_org.conflux.domain.model.Role

/**
 * 伺服器身分組設定標籤頁
 */
private enum class RoleSettingsTab {
    Permissions,
    Members,
}

/**
 * 權限顯示項目模型
 */
data class PermissionItem(
    val flag: Long,
    val title: String,
    val description: String,
)

val ALL_PERMISSIONS_LIST =
    listOf(
        PermissionItem(
            flag = PermissionFlags.ADMINISTRATOR,
            title = "管理員",
            description = "擁有伺服器的所有權限，並可繞過頻道專用權限。此權限具有最高危害性。",
        ),
        PermissionItem(
            flag = PermissionFlags.MANAGE_GUILD,
            title = "管理伺服器",
            description = "允許成員變更伺服器名稱與設定。",
        ),
        PermissionItem(
            flag = PermissionFlags.MANAGE_ROLES,
            title = "管理身分組",
            description = "允許成員建立新身分組並編輯低於此身分組的權限。",
        ),
        PermissionItem(
            flag = PermissionFlags.MANAGE_CHANNELS,
            title = "管理頻道",
            description = "允許成員建立、編輯或刪除頻道。",
        ),
        PermissionItem(
            flag = PermissionFlags.VIEW_CHANNEL,
            title = "檢視頻道",
            description = "允許成員讀取文字頻道訊息。",
        ),
        PermissionItem(
            flag = PermissionFlags.SEND_MESSAGES,
            title = "發送訊息",
            description = "允許成員在文字頻道發送訊息。",
        ),
        PermissionItem(
            flag = PermissionFlags.MANAGE_MESSAGES,
            title = "管理訊息",
            description = "允許成員刪除其他成員發布的訊息或置頂訊息。",
        ),
    )

@Composable
fun GuildSettingsDialog(
    roles: List<Role>,
    selectedRole: Role?,
    isSaving: Boolean,
    errorMessage: String?,
    canManageRoles: Boolean = true,
    members: List<MemberData> = emptyList(),
    memberRoles: Map<String, List<Long>> = emptyMap(),
    onSelectRole: (Role) -> Unit,
    onCreateRole: (name: String) -> Unit,
    onUpdateRole: (roleId: Long, name: String, permissions: Long) -> Unit,
    onDeleteRole: (roleId: Long) -> Unit,
    onAssignMemberRole: (userId: Long, roleId: Long) -> Unit = { _, _ -> },
    onRemoveMemberRole: (userId: Long, roleId: Long) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
) {
    var editingName by remember(selectedRole) { mutableStateOf(selectedRole?.name ?: "") }
    var editingPermissions by remember(selectedRole) { mutableStateOf(selectedRole?.permissions ?: 0L) }
    var selectedTab by remember(selectedRole) { mutableStateOf(RoleSettingsTab.Permissions) }
    var showCreateRoleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedRole) {
        editingName = selectedRole?.name ?: ""
        editingPermissions = selectedRole?.permissions ?: 0L
        if (selectedRole?.isEveryone == true) {
            selectedTab = RoleSettingsTab.Permissions
        }
    }

    // 計算當前選中身分組的成員列表
    val roleMembers =
        remember(selectedRole, members, memberRoles) {
            if (selectedRole == null) {
                emptyList()
            } else if (selectedRole.isEveryone) {
                members
            } else {
                members.filter { member ->
                    val assigned =
                        memberRoles[member.id]
                            ?: memberRoles[member.id.removePrefix("m")]
                            ?: member.roleIds
                    selectedRole.id in assigned
                }
            }
        }

    // 計算尚未擁有此身分組的成員列表 (供新增時選擇)
    val availableMembers =
        remember(selectedRole, members, memberRoles) {
            if (selectedRole == null || selectedRole.isEveryone) {
                emptyList()
            } else {
                members.filter { member ->
                    val assigned =
                        memberRoles[member.id]
                            ?: memberRoles[member.id.removePrefix("m")]
                            ?: member.roleIds
                    selectedRole.id !in assigned
                }
            }
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
                // 左側身分組列表
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
                            text = "身分組",
                            color = Color(0xFF949BA4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        if (canManageRoles) {
                            IconButton(
                                onClick = { showCreateRoleDialog = true },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "新增身分組",
                                    tint = Color(0xFF949BA4),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(roles, key = { it.id }) { role ->
                            val isSelected = selectedRole?.id == role.id
                            val bg = if (isSelected) Color(0xFF404249) else Color.Transparent
                            val textColor = if (isSelected) Color(0xFFF2F3F5) else Color(0xFF949BA4)

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(bg)
                                        .clickable { onSelectRole(role) }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = role.name,
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (role.isEveryone) {
                                    Text(
                                        text = "預設",
                                        color = Color(0xFF80848E),
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // 右側編輯區
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp),
                ) {
                    // 標題列 (取消右上角叉叉，由底部按鈕負責取消)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (selectedRole != null) "編輯身分組 - ${selectedRole.name}" else "伺服器身分組設定",
                            color = Color(0xFFF2F3F5),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (selectedRole == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("請選擇或新增一個身分組以編輯權限與成員", color = Color(0xFF949BA4), fontSize = 15.sp)
                        }
                    } else {
                        // 標籤頁切換列 (預設 everyone 不顯示管理成員 tab)
                        if (!selectedRole.isEveryone) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { selectedTab = RoleSettingsTab.Permissions },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (selectedTab == RoleSettingsTab.Permissions) {
                                                    Color(0xFF404249)
                                                } else {
                                                    Color.Transparent
                                                },
                                        ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text(
                                        text = "權限",
                                        color =
                                            if (selectedTab == RoleSettingsTab.Permissions) {
                                                Color(0xFFF2F3F5)
                                            } else {
                                                Color(0xFF949BA4)
                                            },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                Button(
                                    onClick = { selectedTab = RoleSettingsTab.Members },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (selectedTab == RoleSettingsTab.Members) {
                                                    Color(0xFF404249)
                                                } else {
                                                    Color.Transparent
                                                },
                                        ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text(
                                        text = "管理成員",
                                        color =
                                            if (selectedTab == RoleSettingsTab.Members) {
                                                Color(0xFFF2F3F5)
                                            } else {
                                                Color(0xFF949BA4)
                                            },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        // 依標籤切換顯示區塊
                        if (selectedRole.isEveryone || selectedTab == RoleSettingsTab.Permissions) {
                            // 權限設定滾動區
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                            ) {
                                // 身分組名稱
                                Text("身分組名稱", color = Color(0xFFB5BAC1), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = editingName,
                                    onValueChange = { editingName = it },
                                    enabled = canManageRoles && !selectedRole.isEveryone,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors =
                                        OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFFF2F3F5),
                                            unfocusedTextColor = Color(0xFFDBDEE1),
                                            disabledTextColor = Color(0xFF80848E),
                                            focusedBorderColor = Color(0xFF5865F2),
                                            unfocusedBorderColor = Color(0xFF4E5058),
                                        ),
                                )
                                if (selectedRole.isEveryone) {
                                    Text(
                                        text = "@everyone 為系統預設身分組，無法修改名稱",
                                        color = Color(0xFF80848E),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = Color(0xFF3F4147))
                                Spacer(modifier = Modifier.height(20.dp))

                                Text("權限設定", color = Color(0xFFB5BAC1), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))

                                ALL_PERMISSIONS_LIST.forEach { item ->
                                    val isChecked = (editingPermissions and item.flag) == item.flag
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 20.dp)) {
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
                                        Switch(
                                            checked = isChecked,
                                            enabled = canManageRoles,
                                            onCheckedChange = { checked ->
                                                editingPermissions =
                                                    if (checked) {
                                                        editingPermissions or item.flag
                                                    } else {
                                                        editingPermissions and item.flag.inv()
                                                    }
                                            },
                                            colors =
                                                SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF23A55A),
                                                    uncheckedThumbColor = Color(0xFF80848E),
                                                    uncheckedTrackColor = Color(0xFF4E5058),
                                                ),
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFF35363C), thickness = 0.5.dp)
                                }
                            }

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = errorMessage, color = Color(0xFFFA7777), fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 底部操作列
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (canManageRoles && !selectedRole.isEveryone) {
                                    Button(
                                        onClick = { onDeleteRole(selectedRole.id) },
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
                                            contentDescription = "刪除",
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("刪除身分組", fontSize = 13.sp)
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
                                            onUpdateRole(selectedRole.id, editingName, editingPermissions)
                                        },
                                        enabled = canManageRoles && !isSaving,
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
                        } else {
                            // 管理成員標籤頁 (無篩選時顯示所有成員，輸入時即時過濾)
                            var memberSearchQuery by remember { mutableStateOf("") }

                            val matchingRoleMembers =
                                remember(roleMembers, memberSearchQuery) {
                                    if (memberSearchQuery.isBlank()) {
                                        roleMembers
                                    } else {
                                        roleMembers.filter { it.name.contains(memberSearchQuery, ignoreCase = true) }
                                    }
                                }

                            val matchingAvailableMembers =
                                remember(availableMembers, memberSearchQuery) {
                                    if (memberSearchQuery.isBlank()) {
                                        availableMembers
                                    } else {
                                        availableMembers.filter { it.name.contains(memberSearchQuery, ignoreCase = true) }
                                    }
                                }

                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = memberSearchQuery,
                                    onValueChange = { memberSearchQuery = it },
                                    placeholder = { Text("搜尋成員名稱...", color = Color(0xFF949BA4), fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = "搜尋",
                                            tint = Color(0xFF949BA4),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    trailingIcon = {
                                        if (memberSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { memberSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
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

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (matchingRoleMembers.isEmpty() && matchingAvailableMembers.isEmpty()) {
                                        item {
                                            val emptyPrompt =
                                                if (memberSearchQuery.isBlank()) {
                                                    "伺服器目前沒有任何成員"
                                                } else {
                                                    "找不到符合「$memberSearchQuery」的成員"
                                                }
                                            Text(
                                                text = emptyPrompt,
                                                color = Color(0xFF80848E),
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(vertical = 12.dp),
                                            )
                                        }
                                    } else {
                                        // 1. 已指派成員區塊
                                        if (matchingRoleMembers.isNotEmpty() || memberSearchQuery.isBlank()) {
                                            item {
                                                Text(
                                                    text = "已指派成員 (${matchingRoleMembers.size})",
                                                    color = Color(0xFFB5BAC1),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }

                                            if (matchingRoleMembers.isEmpty()) {
                                                item {
                                                    Text(
                                                        text = "此身分組目前沒有任何成員",
                                                        color = Color(0xFF80848E),
                                                        fontSize = 13.sp,
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                    )
                                                }
                                            } else {
                                                items(matchingRoleMembers, key = { "role_member_${it.id}" }) { member ->
                                                    Row(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFF2B2D31))
                                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f),
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

                                                        if (canManageRoles) {
                                                            Button(
                                                                onClick = {
                                                                    val uid = member.id.removePrefix("m").toLongOrNull() ?: 1L
                                                                    onRemoveMemberRole(uid, selectedRole.id)
                                                                },
                                                                colors =
                                                                    ButtonDefaults.buttonColors(
                                                                        containerColor = Color(0xFFDA373C),
                                                                        contentColor = Color.White,
                                                                    ),
                                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                                                shape = RoundedCornerShape(4.dp),
                                                                modifier = Modifier.height(28.dp),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                            ) {
                                                                Text(text = "移除", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 2. 可新增成員區塊
                                        if (canManageRoles &&
                                            (
                                                matchingAvailableMembers.isNotEmpty() ||
                                                    (memberSearchQuery.isBlank() && availableMembers.isNotEmpty())
                                            )
                                        ) {
                                            item {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                HorizontalDivider(color = Color(0xFF3F4147))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "可新增成員 (${matchingAvailableMembers.size})",
                                                    color = Color(0xFF5865F2),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }

                                            items(matchingAvailableMembers, key = { "avail_${it.id}" }) { member ->
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF2B2D31))
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f),
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

                                                    Button(
                                                        onClick = {
                                                            val uid = member.id.removePrefix("m").toLongOrNull() ?: 1L
                                                            onAssignMemberRole(uid, selectedRole.id)
                                                        },
                                                        colors =
                                                            ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF5865F2),
                                                                contentColor = Color.White,
                                                            ),
                                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.height(28.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    ) {
                                                        Text(text = "新增", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                    onClick = onDismiss,
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
                                    Text("關閉", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 建立新身分組對話框
    if (showCreateRoleDialog) {
        var newRoleName by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { showCreateRoleDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.35f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF313338))
                        .padding(20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "建立身分組",
                        color = Color(0xFFF2F3F5),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newRoleName,
                        onValueChange = { newRoleName = it },
                        placeholder = { Text("身分組名稱", color = Color(0xFF949BA4), fontSize = 14.sp) },
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { showCreateRoleDialog = false },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF949BA4),
                                ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text("取消", fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (newRoleName.isNotBlank()) {
                                    onCreateRole(newRoleName.trim())
                                    showCreateRoleDialog = false
                                }
                            },
                            enabled = newRoleName.isNotBlank(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5865F2),
                                    contentColor = Color.White,
                                ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text("建立身分組", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
