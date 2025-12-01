package com.example.vytal

data class Comment(
    val id: String = "",
    val text: String = "",
    val userId: String = "",
    val userName: String = "Anonymous",
    val timestamp: Long = System.currentTimeMillis()
)