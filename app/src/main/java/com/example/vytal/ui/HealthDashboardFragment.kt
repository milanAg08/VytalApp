package com.example.vytal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.vytal.R
import com.example.vytal.data.AppDatabase
import kotlinx.coroutines.launch

class HealthDashboardFragment : Fragment() {

    private lateinit var bpText: TextView
    private lateinit var sugarText: TextView
    private lateinit var weightText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_health_dashboard, container, false)

        // Bind views
        bpText = view.findViewById(R.id.textBP)
        sugarText = view.findViewById(R.id.textSugar)
        weightText = view.findViewById(R.id.textWeight)

        loadLatestHealthData()

        return view
    }

    private fun loadLatestHealthData() {
        val context = requireContext()
        val db = AppDatabase.getDatabase(context)
        val healthDao = db.healthRecordDao()

        lifecycleScope.launch {
            val latest = healthDao.getLatestRecord()
            if (latest != null) {
                bpText.text = "${latest.systolic}/${latest.diastolic}"
                sugarText.text = "${latest.sugarLevel} mg/dL"
                weightText.text = "${latest.weight} kg"
            } else {
                bpText.text = "--/--"
                sugarText.text = "-- mg/dL"
                weightText.text = "-- kg"
            }
        }
    }
}
