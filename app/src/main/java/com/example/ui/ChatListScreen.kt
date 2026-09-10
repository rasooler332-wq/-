package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.FriendRequestEntity
import com.example.data.MessageEntity
import com.example.data.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(viewModel: ChatViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentConversations by viewModel.recentConversations.collectAsState()
    val pendingRequests by viewModel.pendingFriendRequests.collectAsState()
    val allUsers by viewModel.allRegisteredUsers.collectAsState()
    val showSettings by viewModel.showSettingsDialog.collectAsState()

    var activeSearchFocus by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        currentUser?.let { user ->
                            UserAvatar(
                                name = user.displayName,
                                colorLong = user.avatarColor,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (user.isAdmin) "👑 مسؤول النظام" else "@${user.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (user.isAdmin) WarningAmber else AccentTeal
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Distinct Admin Access Button (Prominent for admins or easy access)
                    if (currentUser?.isAdmin == true) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.ADMIN) },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AdminNavy)
                                .testTag("admin_portal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Portal",
                                tint = WarningAmber
                            )
                        }
                    }

                    // Notification Preferences Button
                    IconButton(
                        onClick = { viewModel.showSettingsDialog.value = true },
                        modifier = Modifier.testTag("notification_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notification Settings",
                            tint = PrimaryBlue
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            // User Search Bar (Only by Name requirement)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.performSearch(it)
                        activeSearchFocus = it.isNotEmpty()
                    },
                    placeholder = {
                        Text(
                            text = "ابحث عن أي مستخدم بالاسم فقط...",
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Users",
                            tint = PrimaryBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.performSearch("")
                                activeSearchFocus = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_user_input")
                )
            }

            // If Search Query is Active: Show Search Results
            if (searchQuery.trim().isNotEmpty()) {
                SearchResultsSection(
                    query = searchQuery,
                    results = searchResults,
                    onChatClick = { partner ->
                        viewModel.performSearch("")
                        viewModel.openChat(partner)
                    },
                    onFriendRequest = { target ->
                        viewModel.sendFriendRequest(target.username)
                    }
                )
            } else {
                // Regular Home Screen: Pending Requests + Recent Chats
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Pending Friend Requests Banner
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            PendingRequestsSection(
                                requests = pendingRequests,
                                allUsers = allUsers,
                                onAccept = { req -> viewModel.acceptFriendRequest(req) },
                                onReject = { req -> viewModel.rejectFriendRequest(req) }
                            )
                        }
                    }

                    // Section Title
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المحادثات الأخيرة / Recent Chats",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "${recentConversations.size} محادثة",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue
                            )
                        }
                    }

                    // If no recent conversations: Show Quick Contact Suggestions
                    if (recentConversations.isEmpty()) {
                        item {
                            EmptyConversationsPlaceholder(
                                allUsers = allUsers,
                                currentUsername = currentUser?.username ?: "",
                                onStartChat = { partner -> viewModel.openChat(partner) }
                            )
                        }
                    } else {
                        items(recentConversations) { lastMsg ->
                            val partnerUsername = if (lastMsg.senderUsername == currentUser?.username) {
                                lastMsg.receiverUsername
                            } else {
                                lastMsg.senderUsername
                            }
                            val partnerUser = allUsers.find { it.username == partnerUsername }

                            RecentChatItem(
                                message = lastMsg,
                                partner = partnerUser ?: UserEntity(
                                    username = partnerUsername,
                                    displayName = partnerUsername,
                                    password = ""
                                ),
                                isLastMessageFromMe = lastMsg.senderUsername == currentUser?.username,
                                onClick = {
                                    partnerUser?.let { viewModel.openChat(it) }
                                }
                            )
                        }
                    }

                    // Direct All Users Discovery List
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جميع جهات الاتصال المتاحة / Available Contacts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    val otherUsers = allUsers.filter { it.username != currentUser?.username }
                    items(otherUsers) { user ->
                        ContactListItem(
                            user = user,
                            onChatClick = { viewModel.openChat(user) },
                            onSendRequest = { viewModel.sendFriendRequest(user.username) }
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        NotificationSettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showSettingsDialog.value = false }
        )
    }
}

@Composable
fun SearchResultsSection(
    query: String,
    results: List<UserEntity>,
    onChatClick: (UserEntity) -> Unit,
    onFriendRequest: (UserEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "نتائج البحث عن: \"$query\" (${results.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (results.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لم يتم العثور على أي مستخدم بهذا الاسم",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(results) { user ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            size = 48.dp,
                            isBlocked = user.isBlocked
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = user.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.isBlocked) BlockedRed else AccentTealDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { onFriendRequest(user) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            }

                            Button(
                                onClick = { onChatClick(user) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("مراسلة")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingRequestsSection(
    requests: List<FriendRequestEntity>,
    allUsers: List<UserEntity>,
    onAccept: (FriendRequestEntity) -> Unit,
    onReject: (FriendRequestEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "طلبات المراسلة المعلقة (${requests.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            requests.forEach { req ->
                val senderUser = allUsers.find { it.username == req.fromUsername }
                val senderName = senderUser?.displayName ?: req.fromUsername

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = senderName,
                        colorLong = senderUser?.avatarColor ?: 0xFF4A90E2,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = senderName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "أرسل لك طلب مراسلة وتواصل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onReject(req) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Decline", tint = BlockedRed)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    FilledTonalButton(
                        onClick = { onAccept(req) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("قبول", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RecentChatItem(
    message: MessageEntity,
    partner: UserEntity,
    isLastMessageFromMe: Boolean,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = partner.displayName,
                colorLong = partner.avatarColor,
                size = 50.dp,
                isBlocked = partner.isBlocked
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = partner.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLastMessageFromMe) {
                            Text(
                                text = "أنت: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                        }

                        // Icon for attachments
                        when (message.messageType) {
                            "IMAGE" -> {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Photo",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "صورة مرفقة / Photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            "AUDIO" -> {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تسجيل صوتي / Voice note",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            "FILE" -> {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "File",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = message.fileName ?: "ملف مرفق / File",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (!message.isRead && !isLastMessageFromMe) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactListItem(
    user: UserEntity,
    onChatClick: () -> Unit,
    onSendRequest: () -> Unit
) {
    Surface(
        onClick = onChatClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = user.displayName,
                colorLong = user.avatarColor,
                size = 42.dp,
                isBlocked = user.isBlocked
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (user.isBlocked) "⛔ محظور من المراسلة" else user.statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.isBlocked) BlockedRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onSendRequest) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Send Friend Request",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyConversationsPlaceholder(
    allUsers: List<UserEntity>,
    currentUsername: String,
    onStartChat: (UserEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "لا توجد محادثات سابقة حتى الآن",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ابدأ محادثة جديدة مع أحد المستخدمين أو ابحث عنهم بالاسم أعلاه",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
