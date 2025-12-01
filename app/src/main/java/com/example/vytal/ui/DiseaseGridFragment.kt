package com.example.vytal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.vytal.R
import com.example.vytal.databinding.FragmentDiseaseGridBinding
import com.example.vytal.ui.analyzers.*

/**
 * Disease Analyzer Grid Screen
 * Modern grid of rounded tiles for 7 diseases
 */
class DiseaseGridFragment : Fragment() {

    private var _binding: FragmentDiseaseGridBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiseaseGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        setupTileClickListeners()
    }

    private fun setupNavigation() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupTileClickListeners() {
        binding.tileDiabetes.setOnClickListener {
            navigateToAnalyzer(DiabetesAnalyzerFragment())
        }

        binding.tileHypertension.setOnClickListener {
            navigateToAnalyzer(HypertensionAnalyzerFragment())
        }

        binding.tileThyroid.setOnClickListener {
            navigateToAnalyzer(ThyroidAnalyzerFragment())
        }

        binding.tileAsthma.setOnClickListener {
            navigateToAnalyzer(AsthmaAnalyzerFragment())
        }

        binding.tileArthritis.setOnClickListener {
            navigateToAnalyzer(ArthritisAnalyzerFragment())
        }

        binding.tileStressFatigue.setOnClickListener {
            navigateToAnalyzer(StressFatigueAnalyzerFragment())
        }

        binding.tileObesity.setOnClickListener {
            navigateToAnalyzer(ObesityAnalyzerFragment())
        }
    }

    private fun navigateToAnalyzer(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



