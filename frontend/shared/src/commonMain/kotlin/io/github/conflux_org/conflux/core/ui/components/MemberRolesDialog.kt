package io.github.conflux_org.conflux.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
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
import io.github.conflux_org.conflux.domain.model.Role

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberRolesDialog(
    member: MemberData,
    roles: List<Role>,
    memberRoleIds: List<Long>,
    canManageRoles: Boolean = true,
    isModifying: Boolean = false,
    errorMessage: String? = null,
    onAssignRole: (roleId: Long) -> Unit,
    onRemoveRole: (roleId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var showDropdown by remember { mutableStateOf(false) }

    val assignedRoles = roles.filter { it.id in memberRoleIds }
    val availableRoles = roles.filter { !it.isEveryone && it.id !in memberRoleIds }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .width(460.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF313338))
                    .padding(24.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 成員資訊標頭
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        UserAvatar(
                            name = member.name,
                            size = 48.dp,
                            backgroundColor = member.avatarColor,
                            status = member.status,
                            statusBorderColor = Color(0xFF313338),
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = member.name,
                                    color = Color(0xFFF2F3F5),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
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
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            if (!member.customStatus.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = member.customStatus,
                                    color = Color(0xFF949BA4),
                                    fontSize = 13.sp,
                                )
                            }
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
                HorizontalDivider(color = Color(0xFF3F4147))
                Spacer(modifier = Modifier.height(18.dp))

                // 身分組管理標題與狀態
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "身分組",
                        color = Color(0xFFB5BAC1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    if (isModifying) {
                        CircularProgressIndicator(
                            color = Color(0xFF5865F2),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 已指派身分組列表 (FlowRow 標籤顯示)
                if (assignedRoles.isEmpty()) {
                    Text(
                        text = "此成員目前尚未擁有任何特殊身分組",
                        color = Color(0xFF949BA4),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        assignedRoles.forEach { role ->
                            Row(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF2B2D31))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF5865F2)),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = role.name,
                                    color = Color(0xFFDBDEE1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )

                                if (canManageRoles && !role.isEveryone) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "移除身分組",
                                        tint = Color(0xFF949BA4),
                                        modifier =
                                            Modifier
                                                .size(14.dp)
                                                .clickable { onRemoveRole(role.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                // 新增身分組操作區 (DropdownMenu)
                if (canManageRoles) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Box {
                        Button(
                            onClick = { showDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2D31)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color(0xFFDBDEE1),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "新增身分組",
                                color = Color(0xFFDBDEE1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier.background(Color(0xFF2B2D31)),
                        ) {
                            if (availableRoles.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "無其他可指派的身分組",
                                            color = Color(0xFF949BA4),
                                            fontSize = 13.sp,
                                        )
                                    },
                                    onClick = { showDropdown = false },
                                )
                            } else {
                                availableRoles.forEach { role ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF5865F2)),
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = role.name,
                                                    color = Color(0xFFF2F3F5),
                                                    fontSize = 13.sp,
                                                )
                                            }
                                        },
                                        onClick = {
                                            onAssignRole(role.id)
                                            showDropdown = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFA777C),
                        fontSize = 13.sp,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 底部確認/關閉按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
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
