package com.example.mychatroom

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object Injection {
    private val instance: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    fun instance(): FirebaseFirestore {
        return instance
    }

    private val firebaseStorageInstance: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    fun firebaseStorageInstance(): FirebaseStorage {
        return firebaseStorageInstance
    }
}