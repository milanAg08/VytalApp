package com.example.vytal

data class Post(
    var postId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val userId: String = "",
    val timestamp: Long = 0,
    val likes: ArrayList<String> = arrayListOf(),

    val userName: String = "",          // must have default ""
    val userProfilePic: String = ""     // must have default ""
)

