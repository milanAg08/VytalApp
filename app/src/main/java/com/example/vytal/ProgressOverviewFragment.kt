package com.example.vytal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.vytal.data.AppDatabase
import com.example.vytal.databinding.FragmentProgressOverviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgressOverviewFragment : Fragment() {

    private var _binding: FragmentProgressOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).healthRecordDao()
            val latest = withContext(Dispatchers.IO) { dao.getLatestRecord() }

            latest?.let {
                binding.textSystolic.text = "${it.systolic} mmHg"
                binding.textDiastolic.text = "${it.diastolic} mmHg"
                binding.textSugar.text = "${it.sugarLevel} mg/dL"
                binding.textWeight.text = "${it.weight} kg"
            } ?: run {
                binding.textSystolic.text = "-"
                binding.textDiastolic.text = "-"
                binding.textSugar.text = "-"
                binding.textWeight.text = "-"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
