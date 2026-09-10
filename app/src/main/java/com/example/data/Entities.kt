package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val password: String,
    val isAdmin: Boolean = false,
    val isBlocked: Boolean = false,
    val avatarColor: Long = 0xFF4A90E2,
    val statusMessage: String = "متاح الآن / Available",
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderUsername: String,
    val receiverUsername: String,
    val content: String,
    val messageType: String, // "TEXT", "IMAGE", "AUDIO", "FILE"
    val fileUriOrPath: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val audioDurationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUsername: String,
    val toUsername: String,
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_prefs")
data class NotificationPrefEntity(
    @PrimaryKey val username: String,
    val enabled: Boolean = true,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val showPreview: Boolean = true,
    val friendRequestAlerts: Boolean = true,
    val discreetMode: Boolean = false
)
