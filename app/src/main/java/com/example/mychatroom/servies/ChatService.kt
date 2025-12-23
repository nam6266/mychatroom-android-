package com.example.mychatroom.servies

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.activity.result.launch
import androidx.compose.ui.semantics.text
import com.example.mychatroom.Injection
import com.example.mychatroom.data.Message
import com.example.mychatroom.repository.ChatRepository
import com.example.mychatroom.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatService : Service() {

    // no need to bind so no care
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var chatRepository: ChatRepository
    private lateinit var notificationHandler: NotificationHandler

    override fun onCreate() {
        super.onCreate()
        chatRepository = ChatRepository(Injection.instance())
        notificationHandler = NotificationHandler(this)

        val message = Message("nam", "110", "hello", System.currentTimeMillis() )

        notificationHandler.createNotificationChannel(
            channelId = NotificationHandler.CHANNEL_ID,
            channelName = "New Messages",
            channelDescription = "Shows notifications for new messages."
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ChatService", "Service starting...")

        val roomId = intent?.getStringExtra("ROOM_ID")
        if (roomId == null) {
            Log.e("ChatService", "Room ID is null, stopping service.")
            stopSelf() // Stop the service if it's started without a room ID
            return START_NOT_STICKY
        }

        // Launch a coroutine to listen for messages
        scope.launch {
            Log.d("ChatService", "Starting to listen for messages in room: $roomId")
            val message = Message("nam", "110", "hello", System.currentTimeMillis() )

            notificationHandler.showSimpleNotification(
                title = "New Message in Room", // Or pass room name in intent
                text = message.text
            )
        }

        // START_STICKY tells the system to restart the service if it gets killed.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines when the service is destroyed
        job.cancel()
        Log.d("ChatService", "Service destroyed, coroutines cancelled.")
    }
}