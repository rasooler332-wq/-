package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // User operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY displayName ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE LOWER(displayName) LIKE '%' || LOWER(:query) || '%' OR LOWER(username) LIKE '%' || LOWER(:query) || '%'")
    fun searchUsers(query: String): Flow<List<UserEntity>>

    @Query("UPDATE users SET isBlocked = :isBlocked WHERE username = :username")
    suspend fun updateUserBlockedStatus(username: String, isBlocked: Boolean)

    // Message operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("""
        SELECT * FROM messages 
        WHERE (senderUsername = :user1 AND receiverUsername = :user2) 
           OR (senderUsername = :user2 AND receiverUsername = :user1)
        ORDER BY timestamp ASC
    """)
    fun getConversationFlow(user1: String, user2: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE senderUsername = :user OR receiverUsername = :user ORDER BY timestamp DESC")
    fun getUserAllMessagesFlow(user: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE id IN (
            SELECT MAX(id) FROM messages 
            WHERE senderUsername = :username OR receiverUsername = :username
            GROUP BY CASE 
                WHEN senderUsername = :username THEN receiverUsername 
                ELSE senderUsername 
            END
        )
        ORDER BY timestamp DESC
    """)
    fun getRecentConversationsFlow(username: String): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET isRead = 1 WHERE receiverUsername = :currentUser AND senderUsername = :otherUser")
    suspend fun markMessagesAsRead(currentUser: String, otherUser: String)

    @Query("SELECT COUNT(*) FROM messages WHERE receiverUsername = :username AND isRead = 0")
    fun getUnreadCountFlow(username: String): Flow<Int>

    // Friend requests
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendRequest(request: FriendRequestEntity): Long

    @Query("SELECT * FROM friend_requests WHERE toUsername = :username AND status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingFriendRequestsFlow(username: String): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE (fromUsername = :user1 AND toUsername = :user2) OR (fromUsername = :user2 AND toUsername = :user1)")
    suspend fun getFriendRequestBetween(user1: String, user2: String): List<FriendRequestEntity>

    @Query("UPDATE friend_requests SET status = :status WHERE id = :id")
    suspend fun updateFriendRequestStatus(id: Long, status: String)

    // Notification Preferences
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setNotificationPrefs(prefs: NotificationPrefEntity)

    @Query("SELECT * FROM notification_prefs WHERE username = :username LIMIT 1")
    suspend fun getNotificationPrefs(username: String): NotificationPrefEntity?

    @Query("SELECT * FROM notification_prefs WHERE username = :username LIMIT 1")
    fun getNotificationPrefsFlow(username: String): Flow<NotificationPrefEntity?>
}
