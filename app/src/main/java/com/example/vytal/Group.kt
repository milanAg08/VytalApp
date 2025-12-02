package com.example.vytal

import java.io.Serializable

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val memberCount: Int = 0,
    val imageUrl: String = "",
    val category: String = "" // e.g., "diabetes", "fitness", "heart_health"
) : Serializable
