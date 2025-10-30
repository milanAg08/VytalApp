package com.example.vytal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProfileDao {

    // Insert a new profile
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    // Update existing profile
    @Update
    suspend fun updateProfile(profile: Profile)

    // Retrieve the latest saved profile (only one in this app)
    @Query("SELECT * FROM profile_table LIMIT 1")
    suspend fun getProfile(): Profile?
}
