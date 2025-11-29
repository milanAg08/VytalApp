package com.example.vytal.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.vytal.R
import com.example.vytal.TrendVisualizationActivity
import com.example.vytal.databinding.FragmentMyspaceBinding
import com.example.vytal.model.DiseaseType
import com.example.vytal.ui.analyzers.*
import com.google.android.material.chip.Chip

class MySpaceFragment : Fragment() {

    private var _binding: FragmentMyspaceBinding? = null
    private val binding get() = _binding!!
    private var analyzer: ChronicRiskAnalyzer? = null
    
    // Current detected condition for navigation
    private var currentDetectedCondition: String? = null

    // Smart symptom suggestions
    private val symptomSuggestions = listOf(
        "Fatigue", "Thirst", "Chest pain", "Dizziness", "Weight gain",
        "Hair loss", "Wheezing", "Joint pain", "Stress", "Headache",
        "Sleep issues", "Blurry vision"
    )

    // Icon mapping for conditions
    private val conditionIcons = mapOf(
        "Diabetes Risk Pattern" to R.drawable.ic_health_diabetes,
        "Hypertension/Heart Risk Pattern" to R.drawable.ic_health_heart,
        "Thyroid Imbalance Pattern" to R.drawable.ic_health_thyroid,
        "Asthma/Respiratory Pattern" to R.drawable.ic_health_lungs,
        "Arthritis/Joint Pain Pattern" to R.drawable.ic_health_joint,
        "Obesity/Metabolic Risk Pattern" to R.drawable.ic_health_scale,
        "Stress/Fatigue Pattern" to R.drawable.ic_health_stress
    )
    
    // Color mapping for conditions
    private val conditionColors = mapOf(
        "Diabetes Risk Pattern" to R.color.pastel_blue_dark,
        "Hypertension/Heart Risk Pattern" to R.color.pastel_pink_dark,
        "Thyroid Imbalance Pattern" to R.color.pastel_purple_dark,
        "Asthma/Respiratory Pattern" to R.color.pastel_teal_dark,
        "Arthritis/Joint Pain Pattern" to R.color.pastel_orange_dark,
        "Obesity/Metabolic Risk Pattern" to R.color.pastel_green_dark,
        "Stress/Fatigue Pattern" to R.color.pastel_purple_dark
    )
    
    // Map conditions to disease types for navigation
    private val conditionToDiseaseType = mapOf(
        "Diabetes Risk Pattern" to DiseaseType.DIABETES,
        "Hypertension/Heart Risk Pattern" to DiseaseType.HYPERTENSION,
        "Thyroid Imbalance Pattern" to DiseaseType.THYROID,
        "Asthma/Respiratory Pattern" to DiseaseType.ASTHMA,
        "Arthritis/Joint Pain Pattern" to DiseaseType.ARTHRITIS,
        "Obesity/Metabolic Risk Pattern" to DiseaseType.OBESITY,
        "Stress/Fatigue Pattern" to DiseaseType.STRESS_FATIGUE
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyspaceBinding.inflate(inflater, container, false)
        
        // Initialize analyzer with extra safety
        try {
            analyzer = ChronicRiskAnalyzer(requireContext())
            android.util.Log.d("MySpaceFragment", "Analyzer initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("MySpaceFragment", "Failed to initialize analyzer: ${e.message}", e)
            analyzer = null
        } catch (t: Throwable) {
            android.util.Log.e("MySpaceFragment", "Critical error initializing analyzer: ${t.message}", t)
            analyzer = null
        }
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            setupSymptomChips()
            setupAnalyzeButton()
            setupGoToAnalyzerButton()
            setupOpenDiseaseAnalyzersButton()
        } catch (e: Exception) {
            android.util.Log.e("MySpaceFragment", "Error in onViewCreated: ${e.message}", e)
        }
    }
    
    private fun setupOpenDiseaseAnalyzersButton() {
        binding.btnOpenDiseaseAnalyzers.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, DiseaseGridFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    
    private fun setupGoToAnalyzerButton() {
        binding.btnGoToAnalyzer.setOnClickListener {
            val condition = currentDetectedCondition ?: return@setOnClickListener
            val diseaseType = conditionToDiseaseType[condition] ?: return@setOnClickListener
            
            // Navigate to the disease analyzer fragment
            val fragment = when (diseaseType) {
                DiseaseType.DIABETES -> DiabetesAnalyzerFragment()
                DiseaseType.HYPERTENSION -> HypertensionAnalyzerFragment()
                DiseaseType.THYROID -> ThyroidAnalyzerFragment()
                DiseaseType.ASTHMA -> AsthmaAnalyzerFragment()
                DiseaseType.ARTHRITIS -> ArthritisAnalyzerFragment()
                DiseaseType.STRESS_FATIGUE -> StressFatigueAnalyzerFragment()
                DiseaseType.OBESITY -> ObesityAnalyzerFragment()
            }
            
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
    }

    private fun setupSymptomChips() {
        val chipGroup = binding.symptomChipGroup
        
        symptomSuggestions.forEach { symptom ->
            val chip = Chip(requireContext()).apply {
                text = symptom
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.medical_chip_bg)
                )
                setTextColor(ContextCompat.getColor(requireContext(), R.color.medical_text_primary))
                chipStrokeWidth = 1f
                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.trend_border)
                )
                textSize = 13f
                chipCornerRadius = 20f
            }
            
            chip.setOnCheckedChangeListener { chipView, isChecked ->
                val chipButton = chipView as? Chip ?: return@setOnCheckedChangeListener
                if (isChecked) {
                    chipButton.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.medical_chip_selected)
                    )
                    chipButton.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.pastel_blue_dark)
                    )
                    chipButton.chipStrokeWidth = 2f
                } else {
                    chipButton.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.medical_chip_bg)
                    )
                    chipButton.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.trend_border)
                    )
                    chipButton.chipStrokeWidth = 1f
                }
            }
            
            chip.setOnClickListener {
                val currentText = binding.inputSymptoms.text.toString()
                val newText = if (currentText.isBlank()) {
                    symptom
                } else {
                    "$currentText, $symptom"
                }
                binding.inputSymptoms.setText(newText)
                binding.inputSymptoms.setSelection(newText.length)
            }
            
            chipGroup.addView(chip)
        }
    }

    private fun setupAnalyzeButton() {
        binding.btnAnalyze.setOnClickListener {
            try {
                val text = binding.inputSymptoms.text.toString().trim()

                if (text.isBlank()) {
                    binding.resultCard.visibility = View.GONE
                    return@setOnClickListener
                }

                val currentAnalyzer = analyzer
                if (currentAnalyzer == null) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Analyzer not available. Please restart the app.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val results = currentAnalyzer.predict(text)
                val top = results.first()
                val confidence = top.second

                // Update UI with results
                updateResultCard(top.first, confidence, results)

                // Show result card with animation
                if (binding.resultCard.visibility == View.GONE) {
                    binding.resultCard.visibility = View.VISIBLE
                    val fadeIn = AlphaAnimation(0f, 1f)
                    fadeIn.duration = 300
                    binding.resultCard.startAnimation(fadeIn)
                }

            } catch (e: Exception) {
                android.util.Log.e("MySpaceFragment", "Error analyzing symptoms: ${e.message}", e)
                binding.resultCard.visibility = View.GONE
            }
        }
    }

    private fun updateResultCard(condition: String, confidence: Float, allResults: List<Pair<String, Float>>) {
        // Store current condition for navigation
        currentDetectedCondition = condition
        
        // Set condition name
        binding.conditionName.text = condition
        
        // Set icon
        val iconRes = conditionIcons[condition] ?: R.drawable.ic_health_heart
        binding.conditionIcon.setImageResource(iconRes)
        
        // Update confidence percentage
        val confidencePercent = (confidence * 100).toInt()
        binding.confidencePercentage.text = "$confidencePercent%"
        
        // Set confidence bar color based on level
        val confidenceColorRes = when {
            confidence >= 0.7f -> R.color.confidence_high
            confidence >= 0.4f -> R.color.confidence_medium
            else -> R.color.confidence_low
        }
        val confidenceColor = ContextCompat.getColor(requireContext(), confidenceColorRes)
        
        // Update confidence bar with proper drawable
        val barDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 3f * resources.displayMetrics.density
            setColor(confidenceColor)
        }
        binding.confidenceBarFill.background = barDrawable
        
        // Update confidence bar width
        binding.resultCard.post {
            val parent = binding.confidenceBarFill.parent as? FrameLayout
            val parentWidth = parent?.width ?: 200
            val layoutParams = binding.confidenceBarFill.layoutParams
            layoutParams.width = (parentWidth * confidence).toInt().coerceAtLeast(8)
            binding.confidenceBarFill.layoutParams = layoutParams
        }
        
        // Update confidence percentage color
        binding.confidencePercentage.setTextColor(confidenceColor)
        
        // Set subtitle
        binding.conditionSubtitle.text = when {
            confidence >= 0.7f -> "High Confidence"
            confidence >= 0.4f -> "Moderate Confidence"
            else -> "Low Confidence"
        }
        
        // Build predictions list with progress bars
        binding.predictionsContainer.removeAllViews()
        allResults.forEach { (label, prob) ->
            val itemView = createPredictionItem(label, prob)
            binding.predictionsContainer.addView(itemView)
        }
        
        // Update button text based on condition
        val diseaseType = conditionToDiseaseType[condition]
        if (diseaseType != null) {
            binding.btnGoToAnalyzer.text = "Open ${diseaseType.displayName} Analyzer →"
            binding.btnGoToAnalyzer.visibility = View.VISIBLE
        } else {
            binding.btnGoToAnalyzer.visibility = View.GONE
        }
        
        // Set info text
        binding.resultInfo.text = "⚠️ Note: This AI analysis is for informational purposes only. Please consult a healthcare professional for proper diagnosis and treatment."
    }
    
    private fun createPredictionItem(label: String, probability: Float): View {
        val density = resources.displayMetrics.density
        
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_prediction_item)
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
        }
        
        // Header row with label and percentage
        val headerRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val labelText = TextView(requireContext()).apply {
            text = label.replace(" Pattern", "")
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.trend_text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val percentText = TextView(requireContext()).apply {
            text = "${(probability * 100).toInt()}%"
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), conditionColors[label] ?: R.color.pastel_blue_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        headerRow.addView(labelText)
        headerRow.addView(percentText)
        
        // Progress bar
        val progressContainer = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (4 * density).toInt()
            ).apply {
                topMargin = (6 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 2 * density
                setColor(ContextCompat.getColor(requireContext(), R.color.trend_border))
            }
        }
        
        val progressFill = View(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                0,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 2 * density
                setColor(ContextCompat.getColor(requireContext(), conditionColors[label] ?: R.color.pastel_blue_dark))
            }
        }
        
        progressContainer.addView(progressFill)
        
        // Set progress width after layout
        progressContainer.post {
            val params = progressFill.layoutParams
            params.width = (progressContainer.width * probability).toInt()
            progressFill.layoutParams = params
        }
        
        container.addView(headerRow)
        container.addView(progressContainer)
        
        return container
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
