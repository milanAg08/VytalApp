package com.example.vytal.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).healthRecordDao()
    val allRecords = dao.getAllRecords()

    fun insertRecord(record: HealthRecord) {
        viewModelScope.launch {
            dao.insertRecord(record)
        }
    }

    suspend fun getLatestRecord(): HealthRecord? {
        return dao.getLatestRecord()
    }
}
