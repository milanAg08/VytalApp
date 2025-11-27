package com.example.vytal

data class Post(
    var postId: String = "",
    var userId: String = "",
    var userName: String = "",          // ⭐ NEW
    var userProfilePic: String = "",    // ⭐ NEW
    var text: String = "",
    var imageUrl: String? = null,
    var timestamp: Long = 0,
    var likes: ArrayList<String> = ArrayList()
)
