package com.example.mychatroom.repository

import android.net.Uri
import android.util.Log
import com.example.mychatroom.data.Post
import com.example.mychatroom.data.Results
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class HomeRepository(
    private val firestore: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage
) {

    suspend fun creatPost(
        message: String,
        posterFistName: String,
        postImgUrl: String?
    ): Results<Unit> = try {
        val docRef = firestore.collection("posts").document()
        val post = Post(
            id = docRef.id,
            postMessage = message,
            posterFistName = posterFistName,
            postImgUrl = postImgUrl
        )
        docRef.set(post).await()
        Results.Success(Unit)
    } catch (e: Exception) {
        Results.Error(e)
    }

    suspend fun getPosts(): Results<List<Post>> = try {
        val querySnapshot =
            firestore.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).get()
                .await()
        val posts = querySnapshot.documents.map { document ->
            document.toObject(Post::class.java)!!.copy(id = document.id)
        }
        Results.Success(posts)
    } catch (e: Exception) {
        Results.Error(e)
    }

    suspend fun uploadImageToFirebase(uri: Uri): Results<String> = try {
        val storageRef = firebaseStorage.reference
        val fileName = "images/${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(fileName)

        imageRef.putFile(uri).await()
        val downloadLink = imageRef.downloadUrl.await().toString()
        Log.d("namLog", "downloadLink : $downloadLink")
        Results.Success(downloadLink)
    } catch (e: Exception) {
        Results.Error(e)
    }
}