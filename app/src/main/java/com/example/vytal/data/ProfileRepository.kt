package com.example.vytal.data

class ProfileRepository(private val profileDao: ProfileDao) {

    // Insert or update a profile
    suspend fun saveProfile(profile: Profile) {
        profileDao.insertProfile(profile)
    }

    // Fetch saved profile
    suspend fun getProfile(): Profile? {
        return profileDao.getProfile()
    }
}
