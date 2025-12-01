package com.example.vytal

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val memberCount: Int = 0,
    val isJoined: Boolean = false
)
