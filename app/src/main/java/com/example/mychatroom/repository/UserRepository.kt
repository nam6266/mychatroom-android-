package com.example.mychatroom.repository

import android.util.Log
import com.example.mychatroom.data.Results
import com.example.mychatroom.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class UserRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private suspend fun saveUserToFirestore(user: User) {
        firestore.collection("users").document(user.email).set(user).await()
    }

    suspend fun login(email: String, password: String): Results<Pair<Boolean, User>> {
        return try {
            if (email.isEmpty() || password.isEmpty()) {
                return Results.Error(Exception("password or email is incorrect format"))
            }
            auth.signInWithEmailAndPassword(email, password).await()
            val userId = auth.currentUser?.uid ?: return Results.Error(Exception("User not found"))

            val snapshot = firestore.collection("users").document(userId).get().await()
            val user = snapshot.toObject(User::class.java)
            user?.let {
                Results.Success(Pair(true, it))
            } ?: Results.Error(Exception("User data not found"))

        } catch (e: Exception) {
            Results.Error(e)
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Results<Pair<Boolean, User>> =
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
            if (uid != null) {
                val user = User(firstName = firstName, lastName = lastName, email = email)
                firestore.collection("users").document(uid).set(user).await()
                Results.Success(Pair(true, user))
            } else {
                Results.Error(Exception("User ID not found"))
            }
        } catch (e: Exception) {
            Log.d("namLog", "signUp fail : $e")
            Results.Error(e)
        }

//    fun getCurrentUser(roomId: String): Flow<Int> = callbackFlow {
//        val subscription =
//            firestore.collection("rooms")
//                .document(roomId)
//                .collection("messages")
//                .orderBy("timeStamp")
//                .addSnapshotListener { querySnapshot, exception ->
//                    if (exception != null) {
//                        trySend(emptyList<Message>())
//                        return@addSnapshotListener
//                    }
//
//                    querySnapshot?.let {
//                        val message =
//                            it.documents.mapNotNull { doc -> doc.toObject(Message::class.java) }
//                        trySend(message).isSuccess
//                    }
//                }
//        awaitClose {
//            subscription.remove()
//        }
//    }

    suspend fun getCurrentUser(): Results<User> = try {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val userDocument = firestore.collection("users").document(uid).get().await()
            val user = userDocument.toObject(User::class.java)
            if (user != null) {
                Results.Success(user)
            } else {
                Results.Error(Exception("User data not found"))
            }
        } else {
            Results.Error(Exception("User not authenticated"))
        }
    } catch (e: Exception) {
        Results.Error(e)
    }
}