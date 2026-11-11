package com.example.mychatroom.repository

import android.util.Log
import com.example.mychatroom.data.Message
import com.example.mychatroom.data.Results
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val firestore: FirebaseFirestore) {

    suspend fun sendMessage(roomId: String, message: Message): Results<Boolean> = try {
        firestore.collection("rooms").document(roomId)
            .collection("messages").add(message).await()
        Results.Success(true)
    } catch (
        e: Exception
    ) {
        Results.Error(e)
    }

    fun getChatMessage(roomId: String): Flow<List<Message>> = callbackFlow {
        val subscription = firestore.collection("rooms").document(roomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Listening to room $roomId failed", error)
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    // mapNotNull: one malformed document must not crash the chat
                    trySend(it.documents.mapNotNull { doc -> doc.toObject(Message::class.java) })
                }
            }

        awaitClose { subscription.remove() }
    }

}