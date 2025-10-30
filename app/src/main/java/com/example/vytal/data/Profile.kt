package com.example.vytal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// ------------------------------
// This class defines the structure of our table in the database.
// Each property corresponds to a column.
// ------------------------------
@Entity(tableName = "profile_table")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Unique ID for each saved profile

    val name: String,
    val age: Int,
    val email: String,
    val imageUri: String? // optional, stores selected photo path
)
