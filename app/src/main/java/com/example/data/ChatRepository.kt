package com.example.data

import android.content.Context
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ChatRepository(
    private val chatDao: ChatDao,
    private val context: Context
) {
    private val repoScope = CoroutineScope(Dispatchers.IO)

    suspend fun seedInitialDataIfEmpty() {
        val users = chatDao.getAllUsers()
        if (users.isEmpty()) {
            val defaultUsers = listOf(
                UserEntity(
                    username = "admin",
                    displayName = "مسؤول النظام / Admin",
                    password = "admin",
                    isAdmin = true,
                    isBlocked = false,
                    avatarColor = 0xFF1E293B,
                    statusMessage = "إدارة النظام والتحكم / System Administrator"
                ),
                UserEntity(
                    username = "sara",
                    displayName = "سارة أحمد / Sara",
                    password = "123",
                    isAdmin = false,
                    isBlocked = false,
                    avatarColor = 0xFFE91E63,
                    statusMessage = "متاحة دائماً للمحادثة 💬 / Always ready to chat"
                ),
                UserEntity(
                    username = "omar",
                    displayName = "عمر خالد / Omar",
                    password = "123",
                    isAdmin = false,
                    isBlocked = false,
                    avatarColor = 0xFF2196F3,
                    statusMessage = "في العمل حالياً 💻 / At work right now"
                ),
                UserEntity(
                    username = "layla",
                    displayName = "ليلى حسن / Layla",
                    password = "123",
                    isAdmin = false,
                    isBlocked = false,
                    avatarColor = 0xFF9C27B0,
                    statusMessage = "التصميم والبرمجة 🎨 / Designing & coding"
                ),
                UserEntity(
                    username = "ali",
                    displayName = "علي المنصور / Ali",
                    password = "123",
                    isAdmin = false,
                    isBlocked = false,
                    avatarColor = 0xFF009688,
                    statusMessage = "طالب تقنية معلومات / IT Student"
                )
            )
            chatDao.insertUsers(defaultUsers)

            // Seed initial sample messages
            val now = System.currentTimeMillis()
            val initialMessages = listOf(
                MessageEntity(
                    senderUsername = "sara",
                    receiverUsername = "admin",
                    content = "مرحباً بك في تطبيق DirectChat! نظام المراسلة يعمل بكفاءة.",
                    messageType = "TEXT",
                    timestamp = now - 3600000 * 2,
                    isRead = true
                ),
                MessageEntity(
                    senderUsername = "sara",
                    receiverUsername = "admin",
                    content = "أرفقت لك صورة المستند والمخطط هنا 📊",
                    messageType = "IMAGE",
                    fileUriOrPath = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80",
                    fileName = "project_chart.jpg",
                    fileSize = "1.2 MB",
                    timestamp = now - 3600000,
                    isRead = true
                ),
                MessageEntity(
                    senderUsername = "omar",
                    receiverUsername = "admin",
                    content = "تسجيل صوتي يوضح متطلبات العمل",
                    messageType = "AUDIO",
                    fileUriOrPath = "voice_note_1.aac",
                    fileName = "voice_memo_omar.aac",
                    fileSize = "450 KB",
                    audioDurationMs = 14000,
                    timestamp = now - 1800000,
                    isRead = false
                ),
                MessageEntity(
                    senderUsername = "layla",
                    receiverUsername = "admin",
                    content = "ملف المواصفات الفنية للمشروع",
                    messageType = "FILE",
                    fileUriOrPath = "specifications_doc.pdf",
                    fileName = "Technical_Specs_v2.pdf",
                    fileSize = "3.4 MB",
                    timestamp = now - 900000,
                    isRead = false
                ),
                MessageEntity(
                    senderUsername = "omar",
                    receiverUsername = "sara",
                    content = "أهلاً سارة، هل استلمتِ الملفات؟",
                    messageType = "TEXT",
                    timestamp = now - 1200000,
                    isRead = true
                )
            )
            chatDao.insertMessages(initialMessages)

            // Seed sample friend request
            chatDao.insertFriendRequest(
                FriendRequestEntity(
                    fromUsername = "ali",
                    toUsername = "admin",
                    status = "PENDING",
                    timestamp = now - 600000
                )
            )

            // Seed default notification prefs
            chatDao.setNotificationPrefs(
                NotificationPrefEntity(
                    username = "admin",
                    enabled = true,
                    sound = true,
                    vibrate = true,
                    showPreview = true,
                    friendRequestAlerts = true,
                    discreetMode = false
                )
            )
        }
    }

    // Authentication
    sealed class AuthResult {
        data class Success(val user: UserEntity) : AuthResult()
        data class Error(val message: String) : AuthResult()
        object Blocked : AuthResult()
    }

    suspend fun login(username: String, password: String): AuthResult {
        val user = chatDao.getUserByUsername(username.trim())
            ?: return AuthResult.Error("اسم المستخدم غير موجود / Username not found")

        if (user.password != password.trim()) {
            return AuthResult.Error("كلمة المرور غير صحيحة / Incorrect password")
        }

        if (user.isBlocked) {
            return AuthResult.Blocked
        }

        return AuthResult.Success(user)
    }

    suspend fun register(username: String, displayName: String, password: String): AuthResult {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.length < 3) {
            return AuthResult.Error("اسم المستخدم يجب ألا يقل عن 3 أحرف / Username too short")
        }
        if (password.trim().length < 3) {
            return AuthResult.Error("كلمة المرور يجب ألا تقل عن 3 أحرف / Password too short")
        }

        val existing = chatDao.getUserByUsername(cleanUsername)
        if (existing != null) {
            return AuthResult.Error("اسم المستخدم محجوز بالفعل / Username already taken")
        }

        // Palette generator for avatar
        val colors = listOf(0xFF2563EB, 0xFF7C3AED, 0xFFDB2777, 0xFF059669, 0xFFD97706, 0xFF0891B2)
        val assignedColor = colors[cleanUsername.hashCode().let { if (it < 0) -it else it } % colors.size]

        val newUser = UserEntity(
            username = cleanUsername,
            displayName = if (displayName.trim().isEmpty()) cleanUsername else displayName.trim(),
            password = password.trim(),
            isAdmin = false,
            isBlocked = false,
            avatarColor = assignedColor,
            statusMessage = "مستخدم جديد في DirectChat / New member"
        )
        chatDao.insertUser(newUser)

        chatDao.setNotificationPrefs(
            NotificationPrefEntity(
                username = cleanUsername,
                enabled = true,
                sound = true,
                vibrate = true,
                showPreview = true,
                friendRequestAlerts = true,
                discreetMode = false
            )
        )

        return AuthResult.Success(newUser)
    }

    suspend fun getUser(username: String): UserEntity? {
        return chatDao.getUserByUsername(username)
    }

    fun getAllUsersFlow(): Flow<List<UserEntity>> = chatDao.getAllUsersFlow()

    fun searchUsers(query: String): Flow<List<UserEntity>> = chatDao.searchUsers(query)

    suspend fun setBlockedStatus(username: String, isBlocked: Boolean) {
        chatDao.updateUserBlockedStatus(username, isBlocked)
    }

    // Messaging
    fun getConversationFlow(user1: String, user2: String): Flow<List<MessageEntity>> =
        chatDao.getConversationFlow(user1, user2)

    fun getRecentConversationsFlow(username: String): Flow<List<MessageEntity>> =
        chatDao.getRecentConversationsFlow(username)

    fun getAllMessagesFlow(): Flow<List<MessageEntity>> = chatDao.getAllMessagesFlow()

    fun getUnreadCountFlow(username: String): Flow<Int> = chatDao.getUnreadCountFlow(username)

    suspend fun markMessagesAsRead(currentUser: String, otherUser: String) {
        chatDao.markMessagesAsRead(currentUser, otherUser)
    }

    suspend fun sendMessage(
        sender: UserEntity,
        receiverUsername: String,
        content: String,
        type: String = "TEXT",
        fileUriOrPath: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        audioDurationMs: Long = 0
    ): Long {
        if (sender.isBlocked) {
            throw IllegalStateException("حسابك محظور من إرسال الرسائل / Account is blocked")
        }

        val msg = MessageEntity(
            senderUsername = sender.username,
            receiverUsername = receiverUsername,
            content = content,
            messageType = type,
            fileUriOrPath = fileUriOrPath,
            fileName = fileName,
            fileSize = fileSize,
            audioDurationMs = audioDurationMs,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        val id = chatDao.insertMessage(msg)

        // Simulate intelligent automated companion reply for demonstration & real-time notification
        if (receiverUsername in listOf("sara", "omar", "layla", "ali")) {
            simulateRecipientReply(sender.username, receiverUsername, content, type)
        }

        return id
    }

    private fun simulateRecipientReply(
        userToNotify: String,
        responderUsername: String,
        userMessage: String,
        userMsgType: String
    ) {
        repoScope.launch {
            // Wait 2.5 seconds to simulate real-time typing and delivery
            delay(2500)

            val responder = chatDao.getUserByUsername(responderUsername) ?: return@launch
            if (responder.isBlocked) return@launch

            val (replyText, replyType, fileUrl, fName, fSize, dur) = when (userMsgType) {
                "IMAGE" -> Tuple6(
                    "صورة رائعة جداً! تم استلامها وحفظها بنجاح 📸",
                    "TEXT", null, null, null, 0L
                )
                "AUDIO" -> Tuple6(
                    "استمعت للتسجيل الصوتي بوضوح، سأرد عليك بالتفصيل لاحقاً 🎙️",
                    "AUDIO", "voice_response.aac", "voice_reply_${responderUsername}.aac", "320 KB", 8500L
                )
                "FILE" -> Tuple6(
                    "تم تحميل الملف بنجاح، شكراً لك 📁",
                    "TEXT", null, null, null, 0L
                )
                else -> {
                    val responses = listOf(
                        "أهلاً وسهلاً بك! شكراً على رسالتك: '$userMessage'",
                        "تم استلام رسالتك، أنا متواجد الآن ويمكنك إرسال أي ملفات أو صور إضافية!",
                        "رسالة ممتازة! أرسل لي الملف الصوتي إذا أحببت.",
                        "مفهوم تماماً، يسعدني التواصل معك عبر تطبيق DirectChat."
                    )
                    Tuple6(
                        responses.random(),
                        "TEXT", null, null, null, 0L
                    )
                }
            }

            val replyMsg = MessageEntity(
                senderUsername = responderUsername,
                receiverUsername = userToNotify,
                content = replyText,
                messageType = replyType,
                fileUriOrPath = fileUrl,
                fileName = fName,
                fileSize = fSize,
                audioDurationMs = dur,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            chatDao.insertMessage(replyMsg)

            // Trigger immediate real-time push notification for the recipient!
            val prefs = chatDao.getNotificationPrefs(userToNotify)
            NotificationHelper.showMessageNotification(
                context = context,
                senderName = responder.displayName,
                messageContent = replyText,
                messageType = replyType,
                prefs = prefs
            )
        }
    }

    // Friend Requests
    fun getPendingFriendRequestsFlow(username: String): Flow<List<FriendRequestEntity>> =
        chatDao.getPendingFriendRequestsFlow(username)

    suspend fun sendFriendRequest(fromUser: UserEntity, toUsername: String): Boolean {
        if (fromUser.isBlocked) return false
        val target = chatDao.getUserByUsername(toUsername) ?: return false

        val existing = chatDao.getFriendRequestBetween(fromUser.username, toUsername)
        if (existing.any { it.status == "PENDING" }) return false

        chatDao.insertFriendRequest(
            FriendRequestEntity(
                fromUsername = fromUser.username,
                toUsername = toUsername,
                status = "PENDING",
                timestamp = System.currentTimeMillis()
            )
        )

        // Trigger real-time friend request notification
        val prefs = chatDao.getNotificationPrefs(toUsername)
        NotificationHelper.showFriendRequestNotification(
            context = context,
            senderName = fromUser.displayName,
            prefs = prefs
        )

        return true
    }

    suspend fun acceptFriendRequest(requestId: Long, currentUsername: String, senderUsername: String) {
        chatDao.updateFriendRequestStatus(requestId, "ACCEPTED")
        // Send a welcoming message
        chatDao.insertMessage(
            MessageEntity(
                senderUsername = currentUsername,
                receiverUsername = senderUsername,
                content = "تم قبول طلب التواصل، مرحباً بك! 👋",
                messageType = "TEXT",
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )
    }

    suspend fun rejectFriendRequest(requestId: Long) {
        chatDao.updateFriendRequestStatus(requestId, "REJECTED")
    }

    // Notification Preferences
    fun getNotificationPrefsFlow(username: String): Flow<NotificationPrefEntity?> =
        chatDao.getNotificationPrefsFlow(username)

    suspend fun updateNotificationPrefs(prefs: NotificationPrefEntity) {
        chatDao.setNotificationPrefs(prefs)
    }

    fun triggerTestNotification(username: String) {
        repoScope.launch {
            val prefs = chatDao.getNotificationPrefs(username)
            NotificationHelper.showMessageNotification(
                context = context,
                senderName = "سارة أحمد / Sara",
                messageContent = "هذا إشعار تجريبي لاختبار التنبيهات الفورية والمظهر السري 🔔",
                messageType = "TEXT",
                prefs = prefs
            )
        }
    }
}

// Helper tuple for response construction
private data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)
