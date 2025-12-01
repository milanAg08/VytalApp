package com.example.vytal

data class Post(
    val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val userName: String = "Anonymous",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(), // List of user IDs who liked
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false
)

