package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MessageEntity
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: ChatViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val partner by viewModel.activeChatPartner.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val isRecording by viewModel.isRecordingAudio.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val showAttachmentOptions by viewModel.showAttachmentOptions.collectAsState()
    val selectedImagePreview by viewModel.selectedImagePreview.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val isBlocked = currentUser?.isBlocked == true || partner?.isBlocked == true

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CHATS) },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        partner?.let { p ->
                            UserAvatar(
                                name = p.displayName,
                                colorLong = p.avatarColor,
                                size = 40.dp,
                                isBlocked = p.isBlocked
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = p.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (p.isBlocked) "⛔ مستخدم محظور" else p.statusMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (p.isBlocked) BlockedRed else OnlineGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.toastMessage.value = "جاري الاتصال الصوتي الآمن... / Calling"
                    }) {
                        Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = PrimaryBlue)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Block Warning Notice Banner
            if (isBlocked) {
                Surface(
                    color = BlockedLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Blocked",
                            tint = BlockedRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentUser?.isBlocked == true)
                                "حسابك محظور من قبل الإدارة، لا يمكن إرسال الرسائل."
                            else
                                "هذا المستخدم محظور حالياً، لا يمكن تبادل الرسائل معه.",
                            color = BlockedRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Message Bubble History List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.senderUsername == currentUser?.username
                    MessageBubbleItem(
                        message = msg,
                        isCurrentUser = isMe,
                        onImageClick = { url -> viewModel.selectedImagePreview.value = url },
                        onFileDownload = { fName -> viewModel.downloadOrOpenFile(fName) }
                    )
                }
            }

            // Bottom Composer Area
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    // If in voice recording mode
                    if (isRecording) {
                        VoiceRecordingBar(
                            recordingSeconds = recordingSeconds,
                            onCancel = { viewModel.cancelAudioRecording() },
                            onSend = { viewModel.sendAudioRecording() }
                        )
                    } else {
                        // Regular message input bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Attachment Picker Button (+)
                            IconButton(
                                onClick = {
                                    if (!isBlocked) {
                                        viewModel.showAttachmentOptions.value = true
                                    }
                                },
                                enabled = !isBlocked,
                                modifier = Modifier.testTag("attach_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Attach File or Photo",
                                    tint = if (isBlocked) Color.Gray else PrimaryBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Text Field
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                placeholder = {
                                    Text(
                                        text = if (isBlocked) "المراسلة معطلة (حساب محظور)" else "اكتب رسالتك هنا...",
                                        fontSize = 14.sp
                                    )
                                },
                                enabled = !isBlocked,
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .testTag("message_input_field")
                            )

                            // Mic (Voice note) button
                            IconButton(
                                onClick = {
                                    if (!isBlocked) {
                                        viewModel.startAudioRecording()
                                    }
                                },
                                enabled = !isBlocked,
                                modifier = Modifier.testTag("record_audio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice Note",
                                    tint = if (isBlocked) Color.Gray else AccentTeal,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Send Button
                            IconButton(
                                onClick = {
                                    if (messageInput.trim().isNotEmpty() && !isBlocked) {
                                        viewModel.sendTextMessage(messageInput)
                                        messageInput = ""
                                    }
                                },
                                enabled = messageInput.trim().isNotEmpty() && !isBlocked,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (messageInput.trim().isNotEmpty() && !isBlocked)
                                            PrimaryBlue
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send Message",
                                    tint = if (messageInput.trim().isNotEmpty() && !isBlocked)
                                        Color.White
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Attachment Bottom Sheet / Dialog
    if (showAttachmentOptions) {
        AttachmentPickerDialog(
            onDismiss = { viewModel.showAttachmentOptions.value = false },
            onSendImage = { url, name ->
                viewModel.sendImageMessage(url, name)
                viewModel.showAttachmentOptions.value = false
            },
            onSendFile = { name, size, uri ->
                viewModel.sendFileMessage(name, size, uri)
                viewModel.showAttachmentOptions.value = false
            }
        )
    }

    // Fullscreen Image Dialog
    selectedImagePreview?.let { url ->
        FullscreenImageDialog(
            imageUrl = url,
            onDismiss = { viewModel.selectedImagePreview.value = null }
        )
    }
}

@Composable
fun VoiceRecordingBar(
    recordingSeconds: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BlockedLight)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(BlockedRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "جاري التسجيل: 0:${String.format("%02d", recordingSeconds)}",
                color = BlockedRed,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) {
                Text("إلغاء / Cancel", color = BlockedRed)
            }

            Spacer(modifier = Modifier.width(6.dp))

            FilledIconButton(
                onClick = onSend,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryBlue),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Voice Note",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AttachmentPickerDialog(
    onDismiss: () -> Unit,
    onSendImage: (String, String) -> Unit,
    onSendFile: (String, String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إرسال ملف أو وسائط / Send Attachment",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "اختر نوع الملف المراد مشاركته في المحادثة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Photos option
                AttachmentOptionRow(
                    icon = Icons.Default.PhotoLibrary,
                    iconBg = PrimaryBlue,
                    title = "مشاركة صورة فوتوغرافية / Photo",
                    subtitle = "إرسال صور وتصميمات بدقة عالية",
                    onClick = {
                        val samplePhotos = listOf(
                            "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=800&auto=format&fit=crop&q=80" to "gradient_design.jpg",
                            "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&auto=format&fit=crop&q=80" to "circuit_tech.jpg",
                            "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&auto=format&fit=crop&q=80" to "landscape_view.jpg"
                        )
                        val selected = samplePhotos.random()
                        onSendImage(selected.first, selected.second)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // PDF Document option
                AttachmentOptionRow(
                    icon = Icons.Default.PictureAsPdf,
                    iconBg = BlockedRed,
                    title = "مستند PDF / Document",
                    subtitle = "تقرير العمل والمواصفات (3.4 MB)",
                    onClick = {
                        onSendFile("Project_Report_Final.pdf", "3.4 MB", "sample_report.pdf")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Zip archive option
                AttachmentOptionRow(
                    icon = Icons.Default.FolderZip,
                    iconBg = WarningAmber,
                    title = "ملف مضغوط / ZIP Archive",
                    subtitle = "حزمة البيانات البرمجية (12.8 MB)",
                    onClick = {
                        onSendFile("Source_Assets_Package.zip", "12.8 MB", "assets.zip")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // SpreadSheet option
                AttachmentOptionRow(
                    icon = Icons.Default.InsertDriveFile,
                    iconBg = AccentTeal,
                    title = "جدول بيانات / Spreadsheet",
                    subtitle = "ميزانية المشروع والإحصائيات (850 KB)",
                    onClick = {
                        onSendFile("Monthly_Budget_2026.xlsx", "850 KB", "budget.xlsx")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء / Cancel")
            }
        }
    )
}

@Composable
fun AttachmentOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
