package com.example.mychatroom.repository

import com.example.mychatroom.data.Results
import com.example.mychatroom.data.Room
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RoomRepository(private val firestore: FirebaseFirestore) {

    suspend fun createRoom(name: String): Results<Unit> = try {
        val docRef = firestore.collection("rooms").document() // generates a unique ID
        val room = Room(id = docRef.id, name = name)          // puts the ID in the object
        docRef.set(room).await()
        Results.Success(Unit)
    } catch (e: Exception) {
        Results.Error(e)
    }

    suspend fun getRoom(): Results<List<Room>> = try {
        val querySnapshot = firestore.collection("rooms").get().await()
        val rooms = querySnapshot.documents.map { document ->
            document.toObject(Room::class.java)!!.copy(id = document.id)
        }
        Results.Success(rooms)
    } catch (e: Exception) {
        Results.Error(e)
    }
}