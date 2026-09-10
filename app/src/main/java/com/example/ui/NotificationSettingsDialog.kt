package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.NotificationPrefEntity
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.ChatViewModel

@Composable
fun NotificationSettingsDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val currentPrefs by viewModel.notificationPrefs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var enabled by remember(currentPrefs) { mutableStateOf(currentPrefs?.enabled ?: true) }
    var sound by remember(currentPrefs) { mutableStateOf(currentPrefs?.sound ?: true) }
    var vibrate by remember(currentPrefs) { mutableStateOf(currentPrefs?.vibrate ?: true) }
    var showPreview by remember(currentPrefs) { mutableStateOf(currentPrefs?.showPreview ?: true) }
    var friendRequests by remember(currentPrefs) { mutableStateOf(currentPrefs?.friendRequestAlerts ?: true) }
    var discreetMode by remember(currentPrefs) { mutableStateOf(currentPrefs?.discreetMode ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notification Settings",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تفضيلات الإشعارات الفورية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Push Notification Preferences",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Master Switch
                SettingSwitchItem(
                    title = "تفعيل الإشعارات / Enable Notifications",
                    subtitle = "استلام التنبيهات عند وصول رسائل أو طلبات جديدة",
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Discreet Mode (Special Requirement)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SettingSwitchItem(
                            title = "الوضع السري / Discreet Mode 🔒",
                            subtitle = "إخفاء اسم المرسل ونص الرسالة في شريط التنبيهات للخصوصية القصوى",
                            checked = discreetMode,
                            enabled = enabled,
                            onCheckedChange = { discreetMode = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitchItem(
                    title = "معاينة الرسائل / Message Preview",
                    subtitle = "عرض مقتطف من الرسالة داخل الإشعار",
                    checked = showPreview,
                    enabled = enabled && !discreetMode,
                    onCheckedChange = { showPreview = it }
                )

                SettingSwitchItem(
                    title = "الصوت / Notification Sound 🔊",
                    subtitle = "تشغيل نغمة عند ورود رسالة جديدة",
                    checked = sound,
                    enabled = enabled,
                    onCheckedChange = { sound = it }
                )

                SettingSwitchItem(
                    title = "الاهتزاز / Vibration 📳",
                    subtitle = "اهتزاز الجهاز عند وصول رسائل جديدة",
                    checked = vibrate,
                    enabled = enabled,
                    onCheckedChange = { vibrate = it }
                )

                SettingSwitchItem(
                    title = "طلبات التواصل / Friend Requests 👥",
                    subtitle = "تنبيه فوري عند قيام أي شخص بإرسال طلب مراسلة",
                    checked = friendRequests,
                    enabled = enabled,
                    onCheckedChange = { friendRequests = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Test notification button
                OutlinedButton(
                    onClick = {
                        // Save current first
                        val user = currentUser?.username ?: return@OutlinedButton
                        val updated = NotificationPrefEntity(
                            username = user,
                            enabled = enabled,
                            sound = sound,
                            vibrate = vibrate,
                            showPreview = showPreview,
                            friendRequestAlerts = friendRequests,
                            discreetMode = discreetMode
                        )
                        viewModel.updateNotificationPrefs(updated)
                        viewModel.triggerTestPushNotification()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("test_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SendToMobile,
                        contentDescription = "Test Notification",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال إشعار فوري تجريبي / Send Test Alert",
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء / Cancel")
                    }

                    Button(
                        onClick = {
                            val user = currentUser?.username ?: return@Button
                            val updated = NotificationPrefEntity(
                                username = user,
                                enabled = enabled,
                                sound = sound,
                                vibrate = vibrate,
                                showPreview = showPreview,
                                friendRequestAlerts = friendRequests,
                                discreetMode = discreetMode
                            )
                            viewModel.updateNotificationPrefs(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.weight(1f).testTag("save_notification_prefs_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ التفضيلات / Save")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
