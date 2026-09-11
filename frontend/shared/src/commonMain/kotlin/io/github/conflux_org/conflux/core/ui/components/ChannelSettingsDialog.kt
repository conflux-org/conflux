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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun ChannelSettingsDialog(
    channel: Channel,
    roles: List<Role>,
    overwrites: List<ChannelOverwrite>,
    isLoading: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    canManageChannels: Boolean = true,
    onSetOverwrite: (targetType: OverwriteTargetType, targetId: Long, allow: Long, deny: Long) -> Unit,
    onDeleteOverwrite: (targetType: OverwriteTargetType, targetId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedRole by remember(roles) { mutableStateOf(roles.firstOrNull { it.isEveryone } ?: roles.firstOrNull()) }
    var currentAllow by remember { mutableStateOf(0L) }
    var currentDeny by remember { mutableStateOf(0L) }

    LaunchedEffect(selectedRole, overwrites) {
        val ow =
            selectedRole?.let { role ->
                overwrites.firstOrNull { it.targetType == OverwriteTargetType.ROLE && it.targetId == role.id }
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
                // 左側身分組覆寫清單
                Column(
                    modifier =
                        Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF2B2D31))
                            .padding(16.dp),
                ) {
                    Text(
                        text = "身分組 / 成員權限",
                        color = Color(0xFF949BA4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(roles, key = { it.id }) { role ->
                            val isSelected = selectedRole?.id == role.id
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
                                        .clickable { selectedRole = role }
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
                            if (selectedRole != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "覆寫身分組：${selectedRole?.name}",
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

                    if (selectedRole == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("請選擇身分組以設定頻道覆寫", color = Color(0xFF949BA4), fontSize = 15.sp)
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

                                    // 三態 Segmented Button 切換 (拒絕 ✕ / 繼承 ─ / 允許 ✓) 無文字純圖示
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
                            selectedRole?.let { role ->
                                overwrites.firstOrNull { it.targetType == OverwriteTargetType.ROLE && it.targetId == role.id }
                            }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (canManageChannels && existingOverwrite != null) {
                                Button(
                                    onClick = {
                                        onDeleteOverwrite(OverwriteTargetType.ROLE, selectedRole!!.id)
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
                                        selectedRole?.let { role ->
                                            onSetOverwrite(
                                                OverwriteTargetType.ROLE,
                                                role.id,
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
                                        Text("儲存頻道權限", fontSize = 13.sp)
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
