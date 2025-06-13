package com.example.mychatroom.data

data class User(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val role: Role = Role.MEMBER
)