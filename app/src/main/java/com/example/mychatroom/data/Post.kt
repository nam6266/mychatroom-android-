package com.example.mychatroom.data

data class Post(
    val id: String = "",
    val posterFistName: String = "",
    val postMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val liked: Boolean = false,
    val likeNumber: Int = 0,
    val dislikeNumber: Int = 0,
    val postImgUrl: String? = ""
)