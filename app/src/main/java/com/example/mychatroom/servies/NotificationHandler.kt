package com.example.mychatroom.servies

import android.app.NotificationManager
import android.content.Context
import com.example.mychatroom.data.Message
import androidx.core.app.NotificationCompat
import com.example.mychatroom.R
import java.util.concurrent.atomic.AtomicInteger

class NotificationHandler(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "new_message_channel"
        private val notificationIdCounter = AtomicInteger(0)
    }

    fun createNotificationChannel(channelId: String, channelName: String, channelDescription: String) {
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = channelDescription

            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNewMessageNotification(message: Message, roomName : String) {
        val notificationId = notificationIdCounter.incrementAndGet()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.bubble_chat)
            .setContentTitle("New Message in $roomName")
            .setContentText("${message.senderFirstName}: ${message.text}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    fun showSimpleNotification(title: String, text: String) {
        val notificationId = notificationIdCounter.incrementAndGet()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.bubble_chat) // **IMPORTANT**: You must create this icon!
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Notification is dismissed when the user taps on it
            .build()

        // Use a unique ID for each notification if you want to show multiple,
        // or a static ID to update the existing one.
        notificationManager.notify(notificationId, notification)
    }
}
