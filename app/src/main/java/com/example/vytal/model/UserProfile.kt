package com.example.vytal.model

import java.io.Serializable

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bloodType: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val address: String = "",
    val emergencyContact: String = "",
    val emergencyPhone: String = "",
    val medicalConditions: String = "",
    val allergies: String = "",
    val medications: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable

