package com.example.mychatroom.data

sealed class Results<out T> {
    data class Success<out T>(val data: T) : Results<T>()
    data class Error(val exception: Exception) : Results<Nothing>()
}