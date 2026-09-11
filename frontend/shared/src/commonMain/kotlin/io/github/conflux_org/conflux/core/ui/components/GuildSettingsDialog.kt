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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import io.github.conflux_org.conflux.domain.model.PermissionFlags
import io.github.conflux_org.conflux.domain.model.Role

private data class PermissionItem(
    val flag: Long,
    val title: String,
    val description: String,
)

private val ALL_PERMISSIONS_LIST =
    listOf(
        PermissionItem(
            PermissionFlags.ADMINISTRATOR,
            "管理員 (Administrator)",
            "成員將擁有伺服器所有權限，並可無視頻道特定的限制與覆寫。",
        ),
        PermissionItem(
            PermissionFlags.MANAGE_GUILD,
            "管理伺服器 (Manage Server)",
            "允許成員編輯伺服器名稱、查看管理資訊。",
        ),
        PermissionItem(
            PermissionFlags.MANAGE_ROLES,
            "管理身分組 (Manage Roles)",
            "允許成員建立、修改與刪除伺服器身分組。",
        ),
        PermissionItem(
            PermissionFlags.MANAGE_CHANNELS,
            "管理頻道 (Manage Channels)",
            "允許成員建立、編輯或刪除文字與語音頻道。",
        ),
        PermissionItem(
            PermissionFlags.VIEW_CHANNEL,
            "檢視頻道 (View Channel)",
            "允許成員預設查看伺服器頻道。若未勾選，將無法查看任何內容。",
        ),
        PermissionItem(
            PermissionFlags.SEND_MESSAGES,
            "發送訊息 (Send Messages)",
            "允許成員在文字頻道中發送訊息。",
        ),
        PermissionItem(
            PermissionFlags.MANAGE_MESSAGES,
            "管理訊息 (Manage Messages)",
            "允許成員刪除或置頂其他成員發送的訊息。",
        ),
    )

@Composable
fun GuildSettingsDialog(
    roles: List<Role>,
    selectedRole: Role?,
    isSaving: Boolean,
    errorMessage: String?,
    canManageRoles: Boolean = true,
    onSelectRole: (Role) -> Unit,
    onCreateRole: (name: String) -> Unit,
    onUpdateRole: (roleId: Long, name: String, permissions: Long) -> Unit,
    onDeleteRole: (roleId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingName by remember(selectedRole) { mutableStateOf(selectedRole?.name ?: "") }
    var editingPermissions by remember(selectedRole) { mutableStateOf(selectedRole?.permissions ?: 0L) }
    var showNewRolePrompt by remember { mutableStateOf(false) }
    var newRoleName by remember { mutableStateOf("") }

    LaunchedEffect(selectedRole) {
        editingName = selectedRole?.name ?: ""
        editingPermissions = selectedRole?.permissions ?: 0L
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier =
                Modifier
                    .size(width = 720.dp, height = 560.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF313338)),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 左側身分組列表
                Column(
                    modifier =
                        Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF2B2D31))
                            .padding(12.dp),
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
                                onClick = { showNewRolePrompt = true },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "新增身分組",
                                    tint = Color(0xFFDBDEE1),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showNewRolePrompt) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1F22), RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                        ) {
                            OutlinedTextField(
                                value = newRoleName,
                                onValueChange = { newRoleName = it },
                                placeholder = { Text("身分組名稱", fontSize = 12.sp) },
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Button(
                                    onClick = {
                                        showNewRolePrompt = false
                                        newRoleName = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text("取消", fontSize = 11.sp, color = Color(0xFF949BA4))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        if (newRoleName.isNotBlank()) {
                                            onCreateRole(newRoleName)
                                            showNewRolePrompt = false
                                            newRoleName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text("建立", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
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
                                        fontSize = 10.sp,
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
                            .padding(20.dp),
                ) {
                    // 頂部關閉按鈕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (selectedRole != null) "編輯身分組 - ${selectedRole.name}" else "伺服器身分組設定",
                            color = Color(0xFFF2F3F5),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "關閉",
                                tint = Color(0xFFB5BAC1),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedRole == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("請選擇或新增一個身分組以編輯權限", color = Color(0xFF949BA4))
                        }
                    } else {
                        // 內容滾動區
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            // 身分組名稱
                            Text("身分組名稱", color = Color(0xFFB5BAC1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
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

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color(0xFF3F4147))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("權限設定", color = Color(0xFFB5BAC1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            ALL_PERMISSIONS_LIST.forEach { item ->
                                val isChecked = (editingPermissions and item.flag) == item.flag
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                        Text(
                                            text = item.title,
                                            color = Color(0xFFF2F3F5),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.description,
                                            color = Color(0xFF949BA4),
                                            fontSize = 12.sp,
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

                        Spacer(modifier = Modifier.height(12.dp))

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
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "刪除",
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("刪除身分組", fontSize = 13.sp)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                ) {
                                    Text("取消", color = Color(0xFF949BA4), fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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
