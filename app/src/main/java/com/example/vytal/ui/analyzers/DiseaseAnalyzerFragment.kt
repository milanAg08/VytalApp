package com.example.vytal.ui.analyzers

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.vytal.R
import com.example.vytal.TrendVisualizationActivity
import com.example.vytal.data.HealthTrendRepository
import com.example.vytal.model.DiseaseType
import com.example.vytal.util.HealthStatus
import com.example.vytal.util.MetricAnalysis
import com.example.vytal.util.ReportAnalysis
import com.example.vytal.util.ReportAnalyzer

/**
 * Base fragment for disease-specific report analyzers.
 * Each disease analyzer extends this and adds specific input fields.
 * Includes the "View Trends" button to navigate to trend visualization.
 * Shows immediate analysis with color-coded badges and personalized tips.
 */
abstract class DiseaseAnalyzerFragment : Fragment() {
    
    protected abstract val diseaseType: DiseaseType
    protected abstract val layoutResId: Int
    
    protected lateinit var repository: HealthTrendRepository
    
    // Analysis views
    private var analysisResultCard: CardView? = null
    private var statusIndicator: View? = null
    private var tvOverallStatus: TextView? = null
    private var tvSummary: TextView? = null
    private var metricsContainer: LinearLayout? = null
    private var tipsContainer: LinearLayout? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutResId, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = HealthTrendRepository()
        
        // Setup analysis views
        setupAnalysisViews(view)
        
        // Setup View Trends button
        view.findViewById<Button>(R.id.btnViewTrends)?.setOnClickListener {
            openTrendVisualization()
        }
        
        // Setup Save Report button
        view.findViewById<Button>(R.id.btnSaveReport)?.setOnClickListener {
            saveReportAndAnalyze()
        }
        
        // Setup back button if exists
        view.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        setupDiseaseSpecificUI(view)
    }
    
    private fun setupAnalysisViews(view: View) {
        analysisResultCard = view.findViewById(R.id.analysisResultCard)
        statusIndicator = view.findViewById(R.id.statusIndicator)
        tvOverallStatus = view.findViewById(R.id.tvOverallStatus)
        tvSummary = view.findViewById(R.id.tvSummary)
        metricsContainer = view.findViewById(R.id.metricsContainer)
        tipsContainer = view.findViewById(R.id.tipsContainer)
    }
    
    protected abstract fun setupDiseaseSpecificUI(view: View)
    protected abstract fun collectMetrics(): Map<String, Any>?
    
    private fun openTrendVisualization() {
        val intent = Intent(requireContext(), TrendVisualizationActivity::class.java).apply {
            putExtra(TrendVisualizationActivity.EXTRA_DISEASE_TYPE, diseaseType.name)
        }
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    private fun saveReportAndAnalyze() {
        val metrics = collectMetrics()
        if (metrics == null) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Analyze the report first
        val analysis = ReportAnalyzer.analyzeReport(diseaseType, metrics)
        showAnalysis(analysis)
        
        // Save to Firebase
        repository.saveHealthReport(
            diseaseType = diseaseType,
            metrics = metrics,
            onSuccess = {
                Toast.makeText(context, "Report saved successfully!", Toast.LENGTH_SHORT).show()
            },
            onError = { e ->
                Toast.makeText(context, "Error saving report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun showAnalysis(analysis: ReportAnalysis) {
        analysisResultCard?.let { card ->
            // Show the card with animation
            if (card.visibility != View.VISIBLE) {
                card.visibility = View.VISIBLE
                card.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right))
            }
            
            // Scroll to analysis card
            (card.parent as? View)?.let { parent ->
                (parent.parent as? ScrollView)?.smoothScrollTo(0, card.top)
            }
        }
        
        // Set overall status badge and colors
        val (statusColor, statusBgColor, statusText) = when (analysis.overallStatus) {
            HealthStatus.NORMAL -> Triple(
                R.color.confidence_high,
                R.color.analysis_improvement_bg,
                "Normal"
            )
            HealthStatus.BORDERLINE -> Triple(
                R.color.confidence_medium,
                R.color.zone_yellow_light,
                "Borderline"
            )
            HealthStatus.HIGH -> Triple(
                R.color.confidence_low,
                R.color.analysis_alert_bg,
                "High"
            )
            HealthStatus.LOW -> Triple(
                R.color.pastel_blue_dark,
                R.color.analysis_stable_bg,
                "Low"
            )
        }
        
        // Update status indicator
        statusIndicator?.setBackgroundColor(ContextCompat.getColor(requireContext(), statusColor))
        
        // Update status badge
        tvOverallStatus?.apply {
            text = statusText
            val badgeBg = background as? GradientDrawable ?: GradientDrawable()
            badgeBg.setColor(ContextCompat.getColor(requireContext(), statusColor))
            badgeBg.cornerRadius = 12 * resources.displayMetrics.density
            background = badgeBg
        }
        
        // Update summary
        tvSummary?.text = analysis.summary
        
        // Build metric items
        metricsContainer?.removeAllViews()
        analysis.metricAnalyses.forEach { metricAnalysis ->
            val metricView = createMetricAnalysisView(metricAnalysis)
            metricsContainer?.addView(metricView)
        }
        
        // Build tips
        tipsContainer?.removeAllViews()
        analysis.tips.forEach { tip ->
            val tipView = createTipView(tip)
            tipsContainer?.addView(tipView)
        }
    }
    
    private fun createMetricAnalysisView(analysis: MetricAnalysis): View {
        val density = resources.displayMetrics.density
        
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * density).toInt()
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_metric_item)
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
        }
        
        // Header row with name, value, and badge
        val headerRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val nameText = TextView(requireContext()).apply {
            text = analysis.displayName
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.trend_text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val valueText = TextView(requireContext()).apply {
            text = "${formatValue(analysis.value)} ${analysis.unit}"
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.trend_text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        // Status badge
        val badgeColor = when (analysis.status) {
            HealthStatus.NORMAL -> R.color.confidence_high
            HealthStatus.BORDERLINE -> R.color.confidence_medium
            HealthStatus.HIGH -> R.color.confidence_low
            HealthStatus.LOW -> R.color.pastel_blue_dark
        }
        
        val badge = TextView(requireContext()).apply {
            text = analysis.statusText
            textSize = 11f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            val badgeBg = GradientDrawable().apply {
                setColor(ContextCompat.getColor(requireContext(), badgeColor))
                cornerRadius = 8 * density
            }
            background = badgeBg
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * density).toInt()
            }
        }
        
        headerRow.addView(nameText)
        headerRow.addView(valueText)
        headerRow.addView(badge)
        
        // Interpretation text
        val interpretationText = TextView(requireContext()).apply {
            text = analysis.interpretation
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.trend_text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
            }
        }
        
        container.addView(headerRow)
        container.addView(interpretationText)
        
        return container
    }
    
    private fun createTipView(tip: String): View {
        val density = resources.displayMetrics.density
        
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            
            val bullet = TextView(requireContext()).apply {
                text = "•"
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.buttonGreenDark))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * density).toInt()
                }
            }
            
            val tipText = TextView(requireContext()).apply {
                text = tip
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.trend_text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            
            addView(bullet)
            addView(tipText)
        }
    }
    
    private fun formatValue(value: Float): String {
        return if (value % 1 == 0f) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}

/**
 * Diabetes Report Analyzer
 */
class DiabetesAnalyzerFragment : DiseaseAnalyzerFragment() {
    
    override val diseaseType = DiseaseType.DIABETES
    override val layoutResId = R.layout.fragment_diabetes_analyzer
    
    private var etFastingGlucose: EditText? = null
    private var etPPGlucose: EditText? = null
    private var etHbA1c: EditText? = null
    
    override fun setupDiseaseSpecificUI(view: View) {
        etFastingGlucose = view.findViewById(R.id.etFastingGlucose)
        etPPGlucose = view.findViewById(R.id.etPPGlucose)
        etHbA1c = view.findViewById(R.id.etHbA1c)
    }
    
    override fun collectMetrics(): Map<String, Any>? {
        val fasting = etFastingGlucose?.text?.toString()?.toFloatOrNull()
        val pp = etPPGlucose?.text?.toString()?.toFloatOrNull()
        val hba1c = etHbA1c?.text?.toString()?.toFloatOrNull()
        
        if (fasting == null && pp == null) return null
        
        return buildMap {
            fasting?.let { put("fasting_glucose", it) }
            pp?.let { put("pp_glucose", it) }
            hba1c?.let { put("hba1c", it) }
        }
    }
}

/**
 * Hypertension Report Analyzer
 */
class HypertensionAnalyzerFragment : DiseaseAnalyzerFragment() {
    
    override val diseaseType = DiseaseType.HYPERTENSION
    override val layoutResId = R.layout.fragment_hypertension_analyzer
    
    private var etSystolic: EditText? = null
    private var etDiastolic: EditText? = null
    private var etPulse: EditText? = null
    
    override fun setupDiseaseSpecificUI(view: View) {
        etSystolic = view.findViewById(R.id.etSystolic)
        etDiastolic = view.findViewById(R.id.etDiastolic)
        etPulse = view.findViewById(R.id.etPulse)
    }
    
    override fun collectMetrics(): Map<String, Any>? {
        val systolic = etSystolic?.text?.toString()?.toFloatOrNull()
        val diastolic = etDiastolic?.text?.toString()?.toFloatOrNull()
        val pulse = etPulse?.text?.toString()?.toFloatOrNull()
        
        if (systolic == null || diastolic == null) return null
        
        return buildMap {
            put("systolic", systolic)
            put("diastolic", diastolic)
            pulse?.let { put("pulse", it) }
        }
    }
}

/**
 * Thyroid Report Analyzer
 */
class ThyroidAnalyzerFragment : DiseaseAnalyzerFragment() {
    
    override val diseaseType = DiseaseType.THYROID
    override val layoutResId = R.layout.fragment_thyroid_analyzer
    
    private var etTSH: EditText? = null
    private var etT3: EditText? = null
    private var etT4: EditText? = null
    
    override fun setupDiseaseSpecificUI(view: View) {
        etTSH = view.findViewById(R.id.etTSH)
        etT3 = view.findViewById(R.id.etT3)
        etT4 = view.findViewById(R.id.etT4)
    }
    
    override fun collectMetrics(): Map<String, Any>? {
        val tsh = etTSH?.text?.toString()?.toFloatOrNull()
        val t3 = etT3?.text?.toString()?.toFloatOrNull()
        val t4 = etT4?.text?.toString()?.toFloatOrNull()
        
        if (tsh == null) return null
        
        return buildMap {
            put("tsh", tsh)
            t3?.let { put("t3", it) }
            t4?.let { put("t4", it) }
        }
    }
}

/**
 * Asthma Report Analyzer
 */
class AsthmaAnalyzerFragment : DiseaseAnalyzerFragment() {
    
    override val diseaseType = DiseaseType.ASTHMA
    override val layoutResId = R.layout.fragment_asthma_analyzer
    
    private var etPeakFlow: EditText? = null
    
    override fun setupDiseaseSpecificUI(view: View) {
        etPeakFlow = view.findViewById(R.id.etPeakFlow)
    }
    
    override fun collectMetrics(): Map<String, Any>? {
        val peakFlow = etPeakFlow?.text?.toString()?.toFloatOrNull() ?: return null
        return mapOf("peak_flow" to peakFlow)
    }
}

/**
 * Arthritis Report Analyzer
 */
class ArthritisAnalyzerFragment : DiseaseAnalyzerFragment() {
    
    override val diseaseType = DiseaseType.ARTHRITIS
    override val layoutResId = R.layout.fragment_arthritis_analyzer
    
    private var etPainScore: EditText? = null
    private var etStiffnessDuration: EditText? = null
    
    override fun setupDiseaseSpecificUI(view: View) {
        etPainScore = view.findViewById(R.id.etPainScore)
        etStiffnessDuration = view.findViewById(R.id.etStiffnessDuration)
    }
    
    override fun collectMetrics(): Map<String, Any>? {
        val painScore = etPainScore?.text?.toString()?.toFloatOrNull() ?: return null
        val stiffness = etStiffnessDuration?.text?.toString()?.toFloatOrNull()
        
        return buildMap {
            put("pain_score", painScore)
            stiffness?.let { put("stiffness_duration", it) }
        }
    }
}

/**
 * Stress/Fatigue Report Analyzer
 */
class StressFatigueAnalyzerFragment : DiseaseAnalyzerFragment() {
    
    override val diseaseType = DiseaseType.STRESS_FATIGUE
    override val layoutResId = R.layout.fragment_stress_fatigue_analyzer
    
    private var etSleepHours: EditText? = null
    private var etStressScore: EditText? = null
    
    override fun setupDiseaseSpecificUI(view: View) {
        etSleepHours = view.findViewById(R.id.etSleepHours)
        etStressScore = view.findViewById(R.id.etStressScore)
    }
    
    override fun collectMetrics(): Map<String, Any>? {
        val sleepHours = etSleepHours?.text?.toString()?.toFloatOrNull()
        val stressScore = etStressScore?.text?.toString()?.toFloatOrNull()
        
        if (sleepHours == null && stressScore == null) return null
        
        return buildMap {
            sleepHours?.let { put("sleep_hours", it) }
            stressScore?.let { put("stress_score", it) }
        }
    }
}

/**
 * Obesity Report Analyzer
 */
class ObesityAnalyzerFragment : DiseaseAnalyzerFragment() {
    
    override val diseaseType = DiseaseType.OBESITY
    override val layoutResId = R.layout.fragment_obesity_analyzer
    
    private var etWeight: EditText? = null
    private var etWaistHeightRatio: EditText? = null
    
    override fun setupDiseaseSpecificUI(view: View) {
        etWeight = view.findViewById(R.id.etWeight)
        etWaistHeightRatio = view.findViewById(R.id.etWaistHeightRatio)
    }
    
    override fun collectMetrics(): Map<String, Any>? {
        val weight = etWeight?.text?.toString()?.toFloatOrNull() ?: return null
        val whr = etWaistHeightRatio?.text?.toString()?.toFloatOrNull()
        
        return buildMap {
            put("weight", weight)
            whr?.let { put("waist_height_ratio", it) }
        }
    }
}
