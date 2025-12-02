package com.example.vytal.model

import java.io.Serializable

data class CommunityPost(
    val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(), // List of user IDs who liked
    val commentsCount: Int = 0
) : Serializable

