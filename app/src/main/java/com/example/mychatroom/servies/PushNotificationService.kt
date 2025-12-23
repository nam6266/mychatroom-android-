package com.example.mychatroom.servies

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        //update server
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // handle message reciver
    }
}