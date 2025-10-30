package com.example.vytal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val systolic: Int,         // Upper BP number
    val diastolic: Int,        // Lower BP number
    val sugarLevel: Float,     // Blood sugar
    val weight: Float,         // Weight in kg
    val timestamp: Long = System.currentTimeMillis() // Time recorded
)
