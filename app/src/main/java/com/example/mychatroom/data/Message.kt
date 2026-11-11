package com.example.mychatroom.data

import com.google.firebase.firestore.Exclude

data class Message(
    val senderFirstName: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    // Computed on the device, never stored in Firestore.
    @get:Exclude val isSentByCurrentUser: Boolean = false
)
