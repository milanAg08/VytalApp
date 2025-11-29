package com.example.vytal.model

import java.io.Serializable

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val duration: Int = 0, // in days
    val reward: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val imageUrl: String = "",
    val category: String = "", // e.g., "yoga", "dietary", "fitness"
    val isCustom: Boolean = false, // true if created by user
    val creatorId: String = "", // UID of user who created custom event
    val targetValue: Int = 0, // For tracking (e.g., steps, glasses of water)
    val unit: String = "" // e.g., "steps", "glasses", "minutes"
) : Serializable

data class EventJoiner(
    val uid: String = "",
    val joinDate: Long = System.currentTimeMillis(),
    val streak: Int = 0,
    val completedDays: List<String> = emptyList(), // List of dates in format "yyyy-MM-dd"
    val totalPoints: Int = 0,
    val isCompleted: Boolean = false
) : Serializable

data class UserReward(
    val points: Int = 0,
    val badges: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable

data class DailyLog(
    val date: String = "", // Format: "yyyy-MM-dd"
    val eventId: String = "",
    val userId: String = "",
    val completed: Boolean = false,
    val value: Int = 0, // Actual value logged (e.g., actual steps taken)
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

