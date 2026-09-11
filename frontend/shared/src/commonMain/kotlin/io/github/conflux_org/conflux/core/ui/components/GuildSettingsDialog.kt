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
    var showNewRolePrompt by remember { mutableStateOf(false) }
    var newRoleName by remember { mutableStateOf("") }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedRole) {
        editingName = selectedRole?.name ?: ""
        editingPermissions = selectedRole?.permissions ?: 0L
    }

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
                    Text(
                        text = "身分組",
                        color = Color(0xFF949BA4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (canManageRoles && !showNewRolePrompt) {
                        Button(
                            onClick = { showNewRolePrompt = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "新增身分組",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (showNewRolePrompt) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1F22), RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                        ) {
                            OutlinedTextField(
                                value = newRoleName,
                                onValueChange = { newRoleName = it },
                                placeholder = { Text("身分組名稱", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFFF2F3F5),
                                        unfocusedTextColor = Color(0xFFDBDEE1),
                                        focusedBorderColor = Color(0xFF5865F2),
                                        unfocusedBorderColor = Color(0xFF4E5058),
                                    ),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = {
                                        showNewRolePrompt = false
                                        newRoleName = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                ) {
                                    Text("取消", fontSize = 12.sp, color = Color(0xFF949BA4))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (newRoleName.isNotBlank()) {
                                            onCreateRole(newRoleName)
                                            showNewRolePrompt = false
                                            newRoleName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text("建立身分組", fontSize = 12.sp, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

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
                    // 頂部標題與關閉按鈕
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
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "關閉",
                                tint = Color(0xFFB5BAC1),
                            )
                        }
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
                        // 標籤頁切換列 (權限 / 管理成員)
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

                        // 依標籤切換顯示區塊
                        if (selectedTab == RoleSettingsTab.Permissions) {
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

                            // 底部操作按鈕列
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
                            // 管理成員標籤頁
                            Column(modifier = Modifier.weight(1f)) {
                                if (selectedRole.isEveryone) {
                                    Text(
                                        text = "@everyone 為伺服器預設身分組，所有成員均自動擁有此身分組，無需手動指派或移除。",
                                        color = Color(0xFF949BA4),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(bottom = 12.dp),
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "擁有此身分組的成員 (${roleMembers.size})",
                                            color = Color(0xFFB5BAC1),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )

                                        if (canManageRoles) {
                                            Button(
                                                onClick = { showAddMemberDialog = true },
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF5865F2),
                                                    ),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "新增成員",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                    }
                                }

                                if (roleMembers.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "目前尚未有成員加入此身分組。可點擊右上角「新增成員」加入成員。",
                                            color = Color(0xFF949BA4),
                                            fontSize = 14.sp,
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        items(roleMembers, key = { it.id }) { member ->
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF2B2D31))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                UserAvatar(
                                                    name = member.name,
                                                    size = 32.dp,
                                                    backgroundColor = member.avatarColor,
                                                    status = member.status,
                                                    statusBorderColor = Color(0xFF2B2D31),
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
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

                                                if (canManageRoles && !selectedRole.isEveryone) {
                                                    Button(
                                                        onClick = {
                                                            val uid = member.id.removePrefix("m").toLongOrNull() ?: 1L
                                                            onRemoveMemberRole(uid, selectedRole.id)
                                                        },
                                                        colors =
                                                            ButtonDefaults.buttonColors(
                                                                containerColor = Color(0x26DA373C),
                                                            ),
                                                        elevation =
                                                            ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                                        shape = RoundedCornerShape(4.dp),
                                                        contentPadding =
                                                            PaddingValues(
                                                                horizontal = 10.dp,
                                                                vertical = 4.dp,
                                                            ),
                                                        modifier = Modifier.height(28.dp),
                                                    ) {
                                                        Text(
                                                            text = "移除",
                                                            color = Color(0xFFFA777C),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                        )
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
                                                containerColor = Color(0xFF5865F2),
                                            ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                    ) {
                                        Text("完成", fontSize = 13.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 搜尋與新增成員對話框
    if (showAddMemberDialog && selectedRole != null) {
        var memberSearchQuery by remember { mutableStateOf("") }
        val filteredMembers =
            remember(availableMembers, memberSearchQuery) {
                if (memberSearchQuery.isBlank()) {
                    availableMembers
                } else {
                    availableMembers.filter {
                        it.name.contains(memberSearchQuery, ignoreCase = true)
                    }
                }
            }

        Dialog(
            onDismissRequest = { showAddMemberDialog = false },
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
                                text = "新增成員至身分組",
                                color = Color(0xFFF2F3F5),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "身分組：${selectedRole.name}",
                                color = Color(0xFF5865F2),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        IconButton(
                            onClick = { showAddMemberDialog = false },
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
                        value = memberSearchQuery,
                        onValueChange = { memberSearchQuery = it },
                        placeholder = { Text("搜尋成員名稱...", color = Color(0xFF949BA4), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "搜尋",
                                tint = Color(0xFF949BA4),
                                modifier = Modifier.size(20.dp),
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

                    if (availableMembers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "所有成員皆已加入此身分組",
                                color = Color(0xFF949BA4),
                                fontSize = 14.sp,
                            )
                        }
                    } else if (filteredMembers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "找不到符合「$memberSearchQuery」的成員",
                                color = Color(0xFF949BA4),
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(filteredMembers, key = { it.id }) { member ->
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { showAddMemberDialog = false },
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
                            Text(text = "完成", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
