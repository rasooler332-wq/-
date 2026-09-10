package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: ChatViewModel) {
    val allUsers by viewModel.allRegisteredUsers.collectAsState()
    val allMessages by viewModel.allSystemMessages.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var userSearchQuery by remember { mutableStateOf("") }
    var messageTypeFilter by remember { mutableStateOf("ALL") }

    val blockedUsersCount = remember(allUsers) { allUsers.count { it.isBlocked } }
    val timeFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CHATS) },
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return to Chats")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AdminGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Admin Shield",
                                tint = AdminNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "لوحة تحكم الإدارة الشاملة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Admin Control & Audit Center",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminGold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.toastMessage.value = "النظام يعمل بكفاءة وأمان تام 🛡️"
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
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
            // KPI Overview Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminMetricCard(
                    title = "إجمالي المستخدمين",
                    value = "${allUsers.size}",
                    icon = Icons.Default.People,
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )

                AdminMetricCard(
                    title = "جميع الرسائل",
                    value = "${allMessages.size}",
                    icon = Icons.Default.Forum,
                    color = AccentTeal,
                    modifier = Modifier.weight(1f)
                )

                AdminMetricCard(
                    title = "المستخدمين المحظورين",
                    value = "$blockedUsersCount",
                    icon = Icons.Default.Block,
                    color = BlockedRed,
                    modifier = Modifier.weight(1f)
                )
            }

            // Navigation Tabs (Users Management vs All Messages Audit)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إدارة وحظر المستخدمين (${allUsers.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("قراءة جميع الرسائل (${allMessages.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // USER MANAGEMENT TAB
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            placeholder = { Text("فلترة وتصفية المستخدمين...") },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_user_filter_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredUsers = remember(allUsers, userSearchQuery) {
                            if (userSearchQuery.trim().isEmpty()) allUsers
                            else allUsers.filter {
                                it.displayName.contains(userSearchQuery, ignoreCase = true) ||
                                it.username.contains(userSearchQuery, ignoreCase = true)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(filteredUsers) { user ->
                                AdminUserCard(
                                    user = user,
                                    onToggleBlock = { viewModel.toggleUserBlock(user) },
                                    onOpenChat = {
                                        viewModel.openChat(user)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // ALL MESSAGES AUDIT TAB
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Filter by type chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "ALL" to "الكل",
                                "TEXT" to "نصية",
                                "IMAGE" to "صور 📷",
                                "AUDIO" to "صوتيات 🎙️",
                                "FILE" to "ملفات 📁"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = messageTypeFilter == key,
                                    onClick = { messageTypeFilter = key },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val filteredMessages = remember(allMessages, messageTypeFilter) {
                            if (messageTypeFilter == "ALL") allMessages
                            else allMessages.filter { it.messageType == messageTypeFilter }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(filteredMessages) { msg ->
                                val sender = allUsers.find { it.username == msg.senderUsername }
                                val receiver = allUsers.find { it.username == msg.receiverUsername }

                                AdminMessageAuditCard(
                                    message = msg,
                                    senderName = sender?.displayName ?: msg.senderUsername,
                                    receiverName = receiver?.displayName ?: msg.receiverUsername,
                                    timeString = timeFormat.format(Date(msg.timestamp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AdminUserCard(
    user: UserEntity,
    onToggleBlock: () -> Unit,
    onOpenChat: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isBlocked) BlockedLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = user.displayName,
                colorLong = user.avatarColor,
                size = 46.dp,
                isBlocked = user.isBlocked
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = AdminNavy,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Admin",
                                color = AdminGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Surface(
                    color = if (user.isBlocked) BlockedRed else OnlineGreen,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (user.isBlocked) "محظور / Blocked" else "نشط / Active",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onOpenChat,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = PrimaryBlue)
                }

                // Block / Unblock Button (Disabled for admin user to protect system integrity)
                if (!user.isAdmin) {
                    Button(
                        onClick = onToggleBlock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.isBlocked) OnlineGreen else BlockedRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_toggle_block_${user.username}")
                    ) {
                        Text(
                            text = if (user.isBlocked) "إلغاء الحظر" else "حظر",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMessageAuditCard(
    message: MessageEntity,
    senderName: String,
    receiverName: String,
    timeString: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "to",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).padding(horizontal = 2.dp)
                    )
                    Text(
                        text = receiverName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                }

                Surface(
                    color = PrimaryBlueLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = message.messageType,
                        color = PrimaryBlueDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (message.messageType) {
                "IMAGE" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مرفق صورة: ${message.fileName ?: "photo.jpg"} (${message.fileSize ?: ""})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                "AUDIO" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تسجيل صوتي: ${message.content} (${message.fileSize ?: ""})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                "FILE" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ملف: ${message.fileName ?: "doc"} (${message.fileSize ?: ""})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
