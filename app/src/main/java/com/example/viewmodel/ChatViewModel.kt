package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.data.FriendRequestEntity
import com.example.data.MessageEntity
import com.example.data.NotificationPrefEntity
import com.example.data.UserEntity
import com.example.notification.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    AUTH,
    CHATS,
    CHAT_DETAIL,
    ADMIN
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ChatRepository(db.chatDao(), application)

    private val _currentScreen = MutableStateFlow(AppScreen.AUTH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _activeChatPartner = MutableStateFlow<UserEntity?>(null)
    val activeChatPartner: StateFlow<UserEntity?> = _activeChatPartner.asStateFlow()

    // Auth Form State
    var usernameInput = MutableStateFlow("admin")
    var passwordInput = MutableStateFlow("admin")
    var displayNameInput = MutableStateFlow("")
    var isRegisterMode = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)
    val isBlockedDialog = MutableStateFlow(false)
    val toastMessage = MutableStateFlow<String?>(null)

    // Search
    val searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<UserEntity>>(emptyList())
    val searchResults: StateFlow<List<UserEntity>> = _searchResults.asStateFlow()

    // Conversation state
    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages: StateFlow<List<MessageEntity>> = _activeMessages.asStateFlow()

    private val _recentConversations = MutableStateFlow<List<MessageEntity>>(emptyList())
    val recentConversations: StateFlow<List<MessageEntity>> = _recentConversations.asStateFlow()

    private val _allRegisteredUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val allRegisteredUsers: StateFlow<List<UserEntity>> = _allRegisteredUsers.asStateFlow()

    private val _allSystemMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val allSystemMessages: StateFlow<List<MessageEntity>> = _allSystemMessages.asStateFlow()

    private val _pendingFriendRequests = MutableStateFlow<List<FriendRequestEntity>>(emptyList())
    val pendingFriendRequests: StateFlow<List<FriendRequestEntity>> = _pendingFriendRequests.asStateFlow()

    private val _notificationPrefs = MutableStateFlow<NotificationPrefEntity?>(null)
    val notificationPrefs: StateFlow<NotificationPrefEntity?> = _notificationPrefs.asStateFlow()

    val showSettingsDialog = MutableStateFlow(false)

    // Voice recording simulation
    val isRecordingAudio = MutableStateFlow(false)
    val recordingSeconds = MutableStateFlow(0)
    private var recordTimerJob: Job? = null

    // Attachment dialog
    val showAttachmentOptions = MutableStateFlow(false)
    val selectedImagePreview = MutableStateFlow<String?>(null)

    init {
        NotificationHelper.createNotificationChannels(application)
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
        observeGlobalData()
    }

    private fun observeGlobalData() {
        viewModelScope.launch {
            repository.getAllUsersFlow().collect { users ->
                _allRegisteredUsers.value = users
            }
        }
        viewModelScope.launch {
            repository.getAllMessagesFlow().collect { msgs ->
                _allSystemMessages.value = msgs
            }
        }
    }

    private var activeChatJob: Job? = null
    private var recentConversationsJob: Job? = null
    private var friendRequestsJob: Job? = null
    private var notificationPrefsJob: Job? = null

    fun login() {
        authError.value = null
        val u = usernameInput.value.trim()
        val p = passwordInput.value.trim()
        if (u.isEmpty() || p.isEmpty()) {
            authError.value = "يرجى كتابة اسم المستخدم وكلمة السر / Please fill all fields"
            return
        }

        viewModelScope.launch {
            when (val res = repository.login(u, p)) {
                is ChatRepository.AuthResult.Success -> {
                    _currentUser.value = res.user
                    onUserAuthenticated(res.user)
                }
                is ChatRepository.AuthResult.Blocked -> {
                    isBlockedDialog.value = true
                }
                is ChatRepository.AuthResult.Error -> {
                    authError.value = res.message
                }
            }
        }
    }

    fun register() {
        authError.value = null
        val u = usernameInput.value.trim()
        val p = passwordInput.value.trim()
        val d = displayNameInput.value.trim()
        if (u.isEmpty() || p.isEmpty()) {
            authError.value = "يرجى كتابة اسم المستخدم وكلمة السر / Please fill all fields"
            return
        }

        viewModelScope.launch {
            when (val res = repository.register(u, d, p)) {
                is ChatRepository.AuthResult.Success -> {
                    _currentUser.value = res.user
                    onUserAuthenticated(res.user)
                }
                is ChatRepository.AuthResult.Error -> {
                    authError.value = res.message
                }
                is ChatRepository.AuthResult.Blocked -> {}
            }
        }
    }

    private fun onUserAuthenticated(user: UserEntity) {
        _currentScreen.value = AppScreen.CHATS
        subscribeUserData(user.username)
    }

    private fun subscribeUserData(username: String) {
        recentConversationsJob?.cancel()
        recentConversationsJob = viewModelScope.launch {
            repository.getRecentConversationsFlow(username).collect {
                _recentConversations.value = it
            }
        }

        friendRequestsJob?.cancel()
        friendRequestsJob = viewModelScope.launch {
            repository.getPendingFriendRequestsFlow(username).collect {
                _pendingFriendRequests.value = it
            }
        }

        notificationPrefsJob?.cancel()
        notificationPrefsJob = viewModelScope.launch {
            repository.getNotificationPrefsFlow(username).collect {
                _notificationPrefs.value = it
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeChatPartner.value = null
        _currentScreen.value = AppScreen.AUTH
        usernameInput.value = ""
        passwordInput.value = ""
        displayNameInput.value = ""
        authError.value = null
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openChat(partner: UserEntity) {
        val current = _currentUser.value ?: return
        _activeChatPartner.value = partner
        _currentScreen.value = AppScreen.CHAT_DETAIL

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            repository.markMessagesAsRead(current.username, partner.username)
            repository.getConversationFlow(current.username, partner.username).collect {
                _activeMessages.value = it
            }
        }
    }

    fun performSearch(query: String) {
        searchQuery.value = query
        val current = _currentUser.value ?: return
        val clean = query.trim().lowercase()

        if (clean.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        val filtered = _allRegisteredUsers.value.filter {
            it.username != current.username &&
            (it.displayName.lowercase().contains(clean) || it.username.lowercase().contains(clean))
        }
        _searchResults.value = filtered
    }

    fun sendTextMessage(text: String) {
        val current = _currentUser.value ?: return
        val partner = _activeChatPartner.value ?: return
        if (text.trim().isEmpty()) return

        if (current.isBlocked) {
            toastMessage.value = "لا يمكن إرسال الرسالة، حسابك محظور / You are blocked"
            return
        }

        viewModelScope.launch {
            repository.sendMessage(
                sender = current,
                receiverUsername = partner.username,
                content = text.trim(),
                type = "TEXT"
            )
        }
    }

    fun sendImageMessage(imageUrl: String, imageName: String = "photo.jpg") {
        val current = _currentUser.value ?: return
        val partner = _activeChatPartner.value ?: return
        if (current.isBlocked) {
            toastMessage.value = "حسابك محظور / You are blocked"
            return
        }

        viewModelScope.launch {
            repository.sendMessage(
                sender = current,
                receiverUsername = partner.username,
                content = "صورة مرفقة / Photo",
                type = "IMAGE",
                fileUriOrPath = imageUrl,
                fileName = imageName,
                fileSize = "1.8 MB"
            )
            toastMessage.value = "تم إرسال الصورة بنجاح 📷 / Image sent"
        }
    }

    fun sendFileMessage(fileName: String, fileSize: String, fileUri: String = "document.pdf") {
        val current = _currentUser.value ?: return
        val partner = _activeChatPartner.value ?: return
        if (current.isBlocked) {
            toastMessage.value = "حسابك محظور / You are blocked"
            return
        }

        viewModelScope.launch {
            repository.sendMessage(
                sender = current,
                receiverUsername = partner.username,
                content = fileName,
                type = "FILE",
                fileUriOrPath = fileUri,
                fileName = fileName,
                fileSize = fileSize
            )
            toastMessage.value = "تم إرسال الملف بنجاح 📁 / File sent"
        }
    }

    fun startAudioRecording() {
        val current = _currentUser.value ?: return
        if (current.isBlocked) {
            toastMessage.value = "حسابك محظور / You are blocked"
            return
        }
        isRecordingAudio.value = true
        recordingSeconds.value = 0
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            while (isRecordingAudio.value) {
                delay(1000)
                recordingSeconds.value += 1
            }
        }
    }

    fun cancelAudioRecording() {
        isRecordingAudio.value = false
        recordingSeconds.value = 0
        recordTimerJob?.cancel()
    }

    fun sendAudioRecording() {
        val current = _currentUser.value ?: return
        val partner = _activeChatPartner.value ?: return
        val duration = recordingSeconds.value
        isRecordingAudio.value = false
        recordTimerJob?.cancel()

        if (duration < 1) return

        val durationMs = duration * 1000L
        viewModelScope.launch {
            repository.sendMessage(
                sender = current,
                receiverUsername = partner.username,
                content = "تسجيل صوتي ($duration ثانية)",
                type = "AUDIO",
                fileUriOrPath = "voice_recording_${System.currentTimeMillis()}.aac",
                fileName = "voice_memo_${System.currentTimeMillis() % 1000}.aac",
                fileSize = "${duration * 32} KB",
                audioDurationMs = durationMs
            )
            toastMessage.value = "تم إرسال التسجيل الصوتي 🎙️ / Voice note sent"
        }
    }

    fun sendFriendRequest(targetUsername: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val success = repository.sendFriendRequest(current, targetUsername)
            if (success) {
                toastMessage.value = "تم إرسال طلب المراسلة بنجاح! / Friend request sent"
            } else {
                toastMessage.value = "طلب المراسلة تم إرساله مسبقاً أو غير متاح / Request already pending"
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequestEntity) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.acceptFriendRequest(request.id, current.username, request.fromUsername)
            toastMessage.value = "تم قبول طلب المراسلة وبدء المحادثة / Request accepted"
        }
    }

    fun rejectFriendRequest(request: FriendRequestEntity) {
        viewModelScope.launch {
            repository.rejectFriendRequest(request.id)
            toastMessage.value = "تم رفض طلب المراسلة / Request declined"
        }
    }

    fun toggleUserBlock(user: UserEntity) {
        viewModelScope.launch {
            val newStatus = !user.isBlocked
            repository.setBlockedStatus(user.username, newStatus)
            toastMessage.value = if (newStatus) {
                "تم حظر المستخدم ${user.displayName} ⛔ / User blocked"
            } else {
                "تم إلغاء حظر المستخدم ${user.displayName} ✅ / User unblocked"
            }
        }
    }

    fun updateNotificationPrefs(newPrefs: NotificationPrefEntity) {
        viewModelScope.launch {
            repository.updateNotificationPrefs(newPrefs)
            _notificationPrefs.value = newPrefs
            toastMessage.value = "تم حفظ تفضيلات الإشعارات بنجاح / Preferences saved"
        }
    }

    fun triggerTestPushNotification() {
        val current = _currentUser.value ?: return
        repository.triggerTestNotification(current.username)
        toastMessage.value = "تم إرسال إشعار فوري تجريبي إلى جهازك 📲 / Test push sent"
    }

    fun downloadOrOpenFile(fileName: String?) {
        toastMessage.value = "تم تنزيل وحفظ $fileName بنجاح في التنزيلات ⬇️ / Downloaded successfully"
    }
}
