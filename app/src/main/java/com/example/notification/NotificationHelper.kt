package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.NotificationPrefEntity

object NotificationHelper {
    const val CHANNEL_MESSAGES = "directchat_messages_channel"
    const val CHANNEL_FRIEND_REQUESTS = "directchat_requests_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Messages Channel
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "الرسائل الجديدة / New Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية عند وصول رسالة جديدة / Immediate alerts for incoming messages"
                enableLights(true)
                enableVibration(true)
            }

            // Friend Requests Channel
            val reqChannel = NotificationChannel(
                CHANNEL_FRIEND_REQUESTS,
                "طلبات المراسلة / Friend Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات طلبات التواصل والتعارف / Notifications for friend and contact requests"
                enableLights(true)
            }

            notificationManager.createNotificationChannel(msgChannel)
            notificationManager.createNotificationChannel(reqChannel)
        }
    }

    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageContent: String,
        messageType: String,
        prefs: NotificationPrefEntity?
    ) {
        if (prefs != null && !prefs.enabled) return

        // Permission check for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Discreet Mode Logic
        val isDiscreet = prefs?.discreetMode == true
        val showPreview = prefs?.showPreview != false

        val title = if (isDiscreet) {
            "DirectChat"
        } else {
            "رسالة جديدة من $senderName"
        }

        val body = when {
            isDiscreet -> "لديك رسالة جديدة / You have a new message"
            !showPreview -> "رسالة خاصة جديدة / Private message received"
            messageType == "IMAGE" -> "📷 أرسل صورة / Sent a photo"
            messageType == "AUDIO" -> "🎙️ تسجيل صوتي / Voice message"
            messageType == "FILE" -> "📎 ملف مرفق / Attached file"
            else -> messageContent
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (prefs?.sound != false) {
            builder.setSound(defaultSoundUri)
        }

        if (prefs?.vibrate != false) {
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt() + 1000

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted or system restricted
        }
    }

    fun showFriendRequestNotification(
        context: Context,
        senderName: String,
        prefs: NotificationPrefEntity?
    ) {
        if (prefs != null && (!prefs.enabled || !prefs.friendRequestAlerts)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isDiscreet = prefs?.discreetMode == true
        val title = if (isDiscreet) "DirectChat" else "طلب مراسلة جديد"
        val body = if (isDiscreet) {
            "طلب تواصل وارد / Incoming connection request"
        } else {
            "أرسل لك $senderName طلب تواصل / $senderName sent you a friend request"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_FRIEND_REQUESTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        if (prefs?.sound != false) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt() + 2000

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
