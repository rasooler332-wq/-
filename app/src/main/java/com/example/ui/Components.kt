package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.MessageEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserAvatar(
    name: String,
    colorLong: Long,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isOnline: Boolean = true,
    isBlocked: Boolean = false
) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "U"
    val avatarBg = if (isBlocked) BlockedRed else Color(colorLong)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }

        if (isBlocked) {
            Box(
                modifier = Modifier
                    .size(size * 0.36f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(BlockedRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Blocked",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.22f)
                )
            }
        } else if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.32f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
            )
        }
    }
}

@Composable
fun MessageBubbleItem(
    message: MessageEntity,
    isCurrentUser: Boolean,
    onImageClick: (String) -> Unit,
    onFileDownload: (String) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    val bubbleColor = if (isCurrentUser) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface
    val metaColor = if (isCurrentUser) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    val shape = if (isCurrentUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(10.dp)
        ) {
            when (message.messageType) {
                "IMAGE" -> {
                    ImageMessageContent(
                        message = message,
                        onImageClick = onImageClick,
                        onDownload = { onFileDownload(message.fileName ?: "photo.jpg") }
                    )
                }
                "AUDIO" -> {
                    AudioMessageContent(
                        message = message,
                        isCurrentUser = isCurrentUser
                    )
                }
                "FILE" -> {
                    FileMessageContent(
                        message = message,
                        isCurrentUser = isCurrentUser,
                        onDownload = { onFileDownload(message.fileName ?: "document.pdf") }
                    )
                }
                else -> {
                    Text(
                        text = message.content,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = metaColor
                )
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = "Status",
                        tint = if (message.isRead) Color(0xFF67E8F9) else metaColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ImageMessageContent(
    message: MessageEntity,
    onImageClick: (String) -> Unit,
    onDownload: () -> Unit
) {
    val imageUrl = message.fileUriOrPath ?: ""

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onImageClick(imageUrl) }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = message.fileName ?: "Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Fullscreen preview hint overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Preview",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.fileName ?: "صورة مرفقة / Photo",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = message.fileSize ?: "1.8 MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(32.dp).testTag("download_image_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Image",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AudioMessageContent(
    message: MessageEntity,
    isCurrentUser: Boolean
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (progress < 1f) {
                kotlinx.coroutines.delay(100)
                progress += 0.02f
            }
            isPlaying = false
            progress = 0f
        }
    }

    val iconTint = if (isCurrentUser) Color.White else PrimaryBlue
    val waveTint = if (isCurrentUser) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isCurrentUser) Color.White.copy(alpha = 0.2f) else PrimaryBlueLight)
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Animated Audio Waveform Simulation
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                val barCount = 18
                val infiniteTransition = rememberInfiniteTransition(label = "wave")
                val animatedScale by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "waveScale"
                )

                for (i in 0 until barCount) {
                    val baseHeight = (8 + ((i * 7) % 18)).dp
                    val activeHeight = if (isPlaying) baseHeight * animatedScale else baseHeight
                    val isPast = (i.toFloat() / barCount) <= progress

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(activeHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPast) (if (isCurrentUser) Color.White else PrimaryBlue)
                                else waveTint.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPlaying) "جاري التشغيل..." else "تسجيل صوتي",
                    fontSize = 11.sp,
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
                )
                val durationSec = (message.audioDurationMs / 1000).coerceAtLeast(3)
                Text(
                    text = "0:${String.format(Locale.getDefault(), "%02d", durationSec)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FileMessageContent(
    message: MessageEntity,
    isCurrentUser: Boolean,
    onDownload: () -> Unit
) {
    val fileName = message.fileName ?: "Document.pdf"
    val isPdf = fileName.endsWith(".pdf", ignoreCase = true)
    val isZip = fileName.endsWith(".zip", ignoreCase = true)

    val fileBg = if (isCurrentUser) Color.White.copy(alpha = 0.18f) else PrimaryBlueLight
    val textColor = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(fileBg)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isPdf) BlockedRed else if (isZip) WarningAmber else PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPdf) Icons.Default.PictureAsPdf else if (isZip) Icons.Default.FolderZip else Icons.Default.InsertDriveFile,
                contentDescription = "File Type",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = message.fileSize ?: "2.4 MB",
                color = textColor.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall
            )
        }

        IconButton(
            onClick = onDownload,
            modifier = Modifier.size(36.dp).testTag("download_file_button")
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download File",
                tint = textColor
            )
        }
    }
}

@Composable
fun FullscreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Fullscreen Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
