package com.example.vytal.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProfileRepository

    init {
        val dao = AppDatabase.getDatabase(application).profileDao()
        repository = ProfileRepository(dao)
    }

    // Save or update profile
    fun saveProfile(profile: Profile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    // Get saved profile
    suspend fun getProfile(): Profile? {
        return repository.getProfile()
    }
}
