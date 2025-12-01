package com.example.vytal.util

import com.example.vytal.model.DiseaseType

/**
 * Status levels for health metrics
 */
enum class HealthStatus {
    NORMAL,      // Green - Within healthy range
    BORDERLINE,  // Yellow/Orange - Needs attention
    HIGH,        // Red - Above normal, concerning
    LOW          // Blue - Below normal
}

/**
 * Result of analyzing a single metric
 */
data class MetricAnalysis(
    val metricName: String,
    val displayName: String,
    val value: Float,
    val unit: String,
    val status: HealthStatus,
    val statusText: String,
    val interpretation: String
)

/**
 * Complete analysis result for a disease report
 */
data class ReportAnalysis(
    val diseaseType: DiseaseType,
    val metricAnalyses: List<MetricAnalysis>,
    val overallStatus: HealthStatus,
    val summary: String,
    val tips: List<String>
)

/**
 * Analyzes health reports and provides immediate feedback
 */
object ReportAnalyzer {

    fun analyzeReport(diseaseType: DiseaseType, metrics: Map<String, Any>): ReportAnalysis {
        return when (diseaseType) {
            DiseaseType.DIABETES -> analyzeDiabetes(metrics)
            DiseaseType.HYPERTENSION -> analyzeHypertension(metrics)
            DiseaseType.THYROID -> analyzeThyroid(metrics)
            DiseaseType.ASTHMA -> analyzeAsthma(metrics)
            DiseaseType.ARTHRITIS -> analyzeArthritis(metrics)
            DiseaseType.STRESS_FATIGUE -> analyzeStressFatigue(metrics)
            DiseaseType.OBESITY -> analyzeObesity(metrics)
        }
    }

    private fun analyzeDiabetes(metrics: Map<String, Any>): ReportAnalysis {
        val analyses = mutableListOf<MetricAnalysis>()
        val tips = mutableListOf<String>()

        // Fasting Glucose Analysis
        metrics["fasting_glucose"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 70 -> Triple(HealthStatus.LOW, "Low", "Below normal range. You may experience hypoglycemia symptoms.")
                value <= 100 -> Triple(HealthStatus.NORMAL, "Normal", "Your fasting glucose is within the healthy range.")
                value <= 125 -> Triple(HealthStatus.BORDERLINE, "Borderline", "Pre-diabetic range. Lifestyle changes recommended.")
                else -> Triple(HealthStatus.HIGH, "High", "Diabetic range. Please consult your doctor.")
            }
            analyses.add(MetricAnalysis("fasting_glucose", "Fasting Glucose", value, "mg/dL", status, statusText, interpretation))
            
            when (status) {
                HealthStatus.LOW -> tips.add("Consider having a small snack before bed to prevent morning lows.")
                HealthStatus.BORDERLINE -> tips.add("Reduce refined carbs and increase fiber intake.")
                HealthStatus.HIGH -> tips.add("Schedule a follow-up with your healthcare provider.")
                else -> {}
            }
        }

        // Post Prandial Glucose Analysis
        metrics["pp_glucose"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 70 -> Triple(HealthStatus.LOW, "Low", "Unusually low after meals. Monitor for symptoms.")
                value <= 140 -> Triple(HealthStatus.NORMAL, "Normal", "Post-meal glucose is well controlled.")
                value <= 199 -> Triple(HealthStatus.BORDERLINE, "Borderline", "Elevated post-meal glucose. Watch portion sizes.")
                else -> Triple(HealthStatus.HIGH, "High", "Significantly elevated. Review meal composition.")
            }
            analyses.add(MetricAnalysis("pp_glucose", "Post Prandial", value, "mg/dL", status, statusText, interpretation))
            
            if (status == HealthStatus.HIGH || status == HealthStatus.BORDERLINE) {
                tips.add("Take a 15-minute walk after meals to help lower blood sugar.")
            }
        }

        // HbA1c Analysis
        metrics["hba1c"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 5.7f -> Triple(HealthStatus.NORMAL, "Normal", "Excellent long-term glucose control.")
                value <= 6.4f -> Triple(HealthStatus.BORDERLINE, "Borderline", "Pre-diabetic range. Focus on prevention.")
                else -> Triple(HealthStatus.HIGH, "High", "Diabetic range. Comprehensive management needed.")
            }
            analyses.add(MetricAnalysis("hba1c", "HbA1c", value, "%", status, statusText, interpretation))
        }

        // Add general tips
        tips.add("Stay hydrated - drink at least 8 glasses of water daily.")
        tips.add("Monitor your glucose at consistent times each day.")

        val overallStatus = analyses.maxOfOrNull { it.status } ?: HealthStatus.NORMAL
        val summary = generateDiabetesSummary(analyses, overallStatus)

        return ReportAnalysis(DiseaseType.DIABETES, analyses, overallStatus, summary, tips.take(4))
    }

    private fun generateDiabetesSummary(analyses: List<MetricAnalysis>, overall: HealthStatus): String {
        return when (overall) {
            HealthStatus.NORMAL -> "Great job! Your glucose levels are within healthy ranges. Keep up your current routine."
            HealthStatus.BORDERLINE -> "Some values need attention. Focus on diet and exercise to prevent progression."
            HealthStatus.HIGH -> "Your readings indicate elevated glucose. Please consult your healthcare provider for guidance."
            HealthStatus.LOW -> "Your glucose is running low. Discuss with your doctor about adjusting your treatment."
        }
    }

    private fun analyzeHypertension(metrics: Map<String, Any>): ReportAnalysis {
        val analyses = mutableListOf<MetricAnalysis>()
        val tips = mutableListOf<String>()

        val systolic = metrics["systolic"]?.toString()?.toFloatOrNull()
        val diastolic = metrics["diastolic"]?.toString()?.toFloatOrNull()

        // Systolic Analysis
        systolic?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 90 -> Triple(HealthStatus.LOW, "Low", "Blood pressure is low. Monitor for dizziness.")
                value < 120 -> Triple(HealthStatus.NORMAL, "Normal", "Optimal systolic pressure.")
                value < 130 -> Triple(HealthStatus.BORDERLINE, "Elevated", "Slightly elevated. Lifestyle changes help.")
                value < 140 -> Triple(HealthStatus.BORDERLINE, "Stage 1", "High blood pressure stage 1.")
                else -> Triple(HealthStatus.HIGH, "Stage 2", "High blood pressure stage 2. Seek medical advice.")
            }
            analyses.add(MetricAnalysis("systolic", "Systolic", value, "mmHg", status, statusText, interpretation))
        }

        // Diastolic Analysis
        diastolic?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 60 -> Triple(HealthStatus.LOW, "Low", "Diastolic pressure is low.")
                value < 80 -> Triple(HealthStatus.NORMAL, "Normal", "Optimal diastolic pressure.")
                value < 90 -> Triple(HealthStatus.BORDERLINE, "Elevated", "Mildly elevated diastolic.")
                else -> Triple(HealthStatus.HIGH, "High", "High diastolic pressure.")
            }
            analyses.add(MetricAnalysis("diastolic", "Diastolic", value, "mmHg", status, statusText, interpretation))
        }

        // Pulse Analysis
        metrics["pulse"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 60 -> Triple(HealthStatus.LOW, "Low", "Bradycardia - slower than normal heart rate.")
                value <= 100 -> Triple(HealthStatus.NORMAL, "Normal", "Heart rate is within normal range.")
                else -> Triple(HealthStatus.HIGH, "High", "Tachycardia - faster than normal heart rate.")
            }
            analyses.add(MetricAnalysis("pulse", "Pulse", value, "bpm", status, statusText, interpretation))
        }

        // Generate tips based on status
        val overallStatus = analyses.maxOfOrNull { it.status } ?: HealthStatus.NORMAL
        
        when (overallStatus) {
            HealthStatus.HIGH, HealthStatus.BORDERLINE -> {
                tips.add("Reduce sodium intake to less than 2,300mg per day.")
                tips.add("Practice stress-reduction techniques like deep breathing.")
                tips.add("Aim for 30 minutes of moderate exercise most days.")
            }
            else -> {
                tips.add("Continue your heart-healthy lifestyle choices.")
            }
        }
        tips.add("Measure BP at the same time daily for accurate tracking.")
        tips.add("Avoid caffeine and alcohol before measuring.")

        val summary = when (overallStatus) {
            HealthStatus.NORMAL -> "Excellent! Your blood pressure is in the optimal range."
            HealthStatus.BORDERLINE -> "Your BP is slightly elevated. Lifestyle modifications can help."
            HealthStatus.HIGH -> "Your blood pressure is high. Please consult your healthcare provider."
            HealthStatus.LOW -> "Your blood pressure is low. Stay hydrated and rise slowly from sitting."
        }

        return ReportAnalysis(DiseaseType.HYPERTENSION, analyses, overallStatus, summary, tips.take(4))
    }

    private fun analyzeThyroid(metrics: Map<String, Any>): ReportAnalysis {
        val analyses = mutableListOf<MetricAnalysis>()
        val tips = mutableListOf<String>()

        // TSH Analysis
        metrics["tsh"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 0.4f -> Triple(HealthStatus.LOW, "Low", "May indicate hyperthyroidism (overactive thyroid).")
                value <= 4.0f -> Triple(HealthStatus.NORMAL, "Normal", "TSH is within the healthy range.")
                value <= 10f -> Triple(HealthStatus.BORDERLINE, "Borderline", "Mildly elevated. May indicate subclinical hypothyroidism.")
                else -> Triple(HealthStatus.HIGH, "High", "Indicates hypothyroidism (underactive thyroid).")
            }
            analyses.add(MetricAnalysis("tsh", "TSH", value, "mIU/L", status, statusText, interpretation))
            
            if (status == HealthStatus.LOW) {
                tips.add("Avoid excessive iodine and stimulants like caffeine.")
            } else if (status == HealthStatus.HIGH || status == HealthStatus.BORDERLINE) {
                tips.add("Ensure adequate iodine and selenium in your diet.")
            }
        }

        // T3 Analysis
        metrics["t3"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 80 -> Triple(HealthStatus.LOW, "Low", "T3 is below normal range.")
                value <= 200 -> Triple(HealthStatus.NORMAL, "Normal", "T3 is within healthy range.")
                else -> Triple(HealthStatus.HIGH, "High", "T3 is elevated.")
            }
            analyses.add(MetricAnalysis("t3", "T3", value, "ng/dL", status, statusText, interpretation))
        }

        // T4 Analysis
        metrics["t4"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 5.0f -> Triple(HealthStatus.LOW, "Low", "T4 is below normal range.")
                value <= 12.0f -> Triple(HealthStatus.NORMAL, "Normal", "T4 is within healthy range.")
                else -> Triple(HealthStatus.HIGH, "High", "T4 is elevated.")
            }
            analyses.add(MetricAnalysis("t4", "T4", value, "µg/dL", status, statusText, interpretation))
        }

        tips.add("Take thyroid medication on an empty stomach if prescribed.")
        tips.add("Get regular thyroid function tests as recommended.")
        tips.add("Manage stress as it can affect thyroid function.")

        val overallStatus = analyses.maxOfOrNull { it.status } ?: HealthStatus.NORMAL
        val summary = when (overallStatus) {
            HealthStatus.NORMAL -> "Your thyroid function appears stable. Continue regular monitoring."
            HealthStatus.BORDERLINE -> "Some thyroid markers need attention. Discuss with your doctor."
            HealthStatus.HIGH -> "Thyroid levels are abnormal. Medical evaluation recommended."
            HealthStatus.LOW -> "Some thyroid levels are low. Please consult your healthcare provider."
        }

        return ReportAnalysis(DiseaseType.THYROID, analyses, overallStatus, summary, tips.take(4))
    }

    private fun analyzeAsthma(metrics: Map<String, Any>): ReportAnalysis {
        val analyses = mutableListOf<MetricAnalysis>()
        val tips = mutableListOf<String>()

        metrics["peak_flow"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value >= 80 -> Triple(HealthStatus.NORMAL, "Green Zone", "Good control! Continue current management.")
                value >= 50 -> Triple(HealthStatus.BORDERLINE, "Yellow Zone", "Caution needed. May need to adjust medication.")
                else -> Triple(HealthStatus.HIGH, "Red Zone", "Medical alert! Use rescue inhaler and seek help if needed.")
            }
            analyses.add(MetricAnalysis("peak_flow", "Peak Flow", value, "% predicted", status, statusText, interpretation))

            when (status) {
                HealthStatus.NORMAL -> {
                    tips.add("Continue your current asthma action plan.")
                    tips.add("Keep rescue inhaler accessible but you're doing great!")
                }
                HealthStatus.BORDERLINE -> {
                    tips.add("Increase controller medication as per your action plan.")
                    tips.add("Avoid known triggers and monitor symptoms closely.")
                    tips.add("Use rescue inhaler as needed before symptoms worsen.")
                }
                HealthStatus.HIGH -> {
                    tips.add("Use rescue inhaler immediately.")
                    tips.add("Seek emergency care if no improvement in 15-20 minutes.")
                    tips.add("Contact your doctor today to review your treatment.")
                }
                else -> {}
            }
        }

        tips.add("Track your triggers to identify patterns.")

        val overallStatus = analyses.firstOrNull()?.status ?: HealthStatus.NORMAL
        val summary = when (overallStatus) {
            HealthStatus.NORMAL -> "Excellent! Your peak flow is in the green zone. Asthma is well controlled."
            HealthStatus.BORDERLINE -> "Your peak flow is in the yellow zone. Be cautious and follow your action plan."
            HealthStatus.HIGH -> "Your peak flow is in the red zone. This requires immediate attention."
            else -> "Continue monitoring your peak flow regularly."
        }

        return ReportAnalysis(DiseaseType.ASTHMA, analyses, overallStatus, summary, tips.take(4))
    }

    private fun analyzeArthritis(metrics: Map<String, Any>): ReportAnalysis {
        val analyses = mutableListOf<MetricAnalysis>()
        val tips = mutableListOf<String>()

        // Pain Score Analysis
        metrics["pain_score"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value <= 3 -> Triple(HealthStatus.NORMAL, "Mild", "Pain is manageable. Good control.")
                value <= 6 -> Triple(HealthStatus.BORDERLINE, "Moderate", "Pain is affecting daily activities.")
                else -> Triple(HealthStatus.HIGH, "Severe", "Significant pain. May need treatment adjustment.")
            }
            analyses.add(MetricAnalysis("pain_score", "Pain Score", value, "/10", status, statusText, interpretation))

            when (status) {
                HealthStatus.BORDERLINE -> tips.add("Consider gentle exercises like swimming or yoga.")
                HealthStatus.HIGH -> tips.add("Apply heat or cold therapy for pain relief.")
                else -> {}
            }
        }

        // Stiffness Duration Analysis
        metrics["stiffness_duration"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value <= 30 -> Triple(HealthStatus.NORMAL, "Normal", "Morning stiffness is within expected range.")
                value <= 60 -> Triple(HealthStatus.BORDERLINE, "Moderate", "Prolonged stiffness may indicate inflammation.")
                else -> Triple(HealthStatus.HIGH, "Prolonged", "Extended stiffness suggests active inflammation.")
            }
            analyses.add(MetricAnalysis("stiffness_duration", "Morning Stiffness", value, "min", status, statusText, interpretation))

            if (status != HealthStatus.NORMAL) {
                tips.add("Do gentle stretches before getting out of bed.")
                tips.add("Take a warm shower to help ease morning stiffness.")
            }
        }

        tips.add("Maintain a healthy weight to reduce joint stress.")
        tips.add("Stay active with low-impact exercises.")
        tips.add("Consider anti-inflammatory foods in your diet.")

        val overallStatus = analyses.maxOfOrNull { it.status } ?: HealthStatus.NORMAL
        val summary = when (overallStatus) {
            HealthStatus.NORMAL -> "Your symptoms are well managed. Keep up your current routine."
            HealthStatus.BORDERLINE -> "Symptoms are moderate. Consider discussing with your doctor."
            HealthStatus.HIGH -> "Symptoms are severe. Please consult your rheumatologist."
            else -> "Continue monitoring your symptoms."
        }

        return ReportAnalysis(DiseaseType.ARTHRITIS, analyses, overallStatus, summary, tips.take(4))
    }

    private fun analyzeStressFatigue(metrics: Map<String, Any>): ReportAnalysis {
        val analyses = mutableListOf<MetricAnalysis>()
        val tips = mutableListOf<String>()

        // Sleep Hours Analysis
        metrics["sleep_hours"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 5 -> Triple(HealthStatus.HIGH, "Poor", "Severely insufficient sleep. This impacts health.")
                value < 7 -> Triple(HealthStatus.BORDERLINE, "Insufficient", "Below recommended 7-9 hours.")
                value <= 9 -> Triple(HealthStatus.NORMAL, "Good", "Optimal sleep duration achieved.")
                else -> Triple(HealthStatus.BORDERLINE, "Excessive", "Oversleeping may indicate other issues.")
            }
            analyses.add(MetricAnalysis("sleep_hours", "Sleep Duration", value, "hours", status, statusText, interpretation))

            when (status) {
                HealthStatus.HIGH -> {
                    tips.add("Set a consistent bedtime and wake time.")
                    tips.add("Avoid screens 1 hour before bed.")
                }
                HealthStatus.BORDERLINE -> {
                    tips.add("Try to add 30 minutes to your sleep schedule.")
                }
                else -> {}
            }
        }

        // Stress Score Analysis
        metrics["stress_score"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value <= 3 -> Triple(HealthStatus.NORMAL, "Low", "Stress levels are well managed.")
                value <= 6 -> Triple(HealthStatus.BORDERLINE, "Moderate", "Moderate stress. Monitor and practice self-care.")
                else -> Triple(HealthStatus.HIGH, "High", "High stress levels. Consider stress management strategies.")
            }
            analyses.add(MetricAnalysis("stress_score", "Stress Level", value, "/10", status, statusText, interpretation))

            when (status) {
                HealthStatus.BORDERLINE, HealthStatus.HIGH -> {
                    tips.add("Practice deep breathing exercises daily.")
                    tips.add("Consider meditation or mindfulness apps.")
                    tips.add("Take short breaks throughout the day.")
                }
                else -> {}
            }
        }

        tips.add("Regular physical activity helps reduce stress.")
        tips.add("Connect with friends and family for emotional support.")

        val overallStatus = analyses.maxOfOrNull { it.status } ?: HealthStatus.NORMAL
        val summary = when (overallStatus) {
            HealthStatus.NORMAL -> "Great! Your sleep and stress levels are healthy. Keep it up!"
            HealthStatus.BORDERLINE -> "Some areas need attention. Small changes can make a big difference."
            HealthStatus.HIGH -> "Your wellness indicators suggest high stress or poor sleep. Prioritize self-care."
            else -> "Continue tracking your wellness metrics."
        }

        return ReportAnalysis(DiseaseType.STRESS_FATIGUE, analyses, overallStatus, summary, tips.take(4))
    }

    private fun analyzeObesity(metrics: Map<String, Any>): ReportAnalysis {
        val analyses = mutableListOf<MetricAnalysis>()
        val tips = mutableListOf<String>()

        // Weight - Note: Without height, we can only track the value
        metrics["weight"]?.toString()?.toFloatOrNull()?.let { value ->
            // For weight alone, we just track it
            analyses.add(MetricAnalysis("weight", "Weight", value, "kg", HealthStatus.NORMAL, "Recorded", "Weight logged for tracking."))
        }

        // Waist-to-Height Ratio Analysis
        metrics["waist_height_ratio"]?.toString()?.toFloatOrNull()?.let { value ->
            val (status, statusText, interpretation) = when {
                value < 0.4f -> Triple(HealthStatus.LOW, "Low", "May indicate underweight. Ensure adequate nutrition.")
                value < 0.5f -> Triple(HealthStatus.NORMAL, "Healthy", "Excellent! Waist-to-height ratio is optimal.")
                value < 0.6f -> Triple(HealthStatus.BORDERLINE, "Elevated", "Increased health risk. Focus on waist reduction.")
                else -> Triple(HealthStatus.HIGH, "High", "High health risk. Comprehensive lifestyle changes needed.")
            }
            analyses.add(MetricAnalysis("waist_height_ratio", "Waist-Height Ratio", value, "", status, statusText, interpretation))

            when (status) {
                HealthStatus.BORDERLINE -> {
                    tips.add("Focus on reducing waist circumference through exercise.")
                    tips.add("Reduce refined carbohydrates and sugary drinks.")
                }
                HealthStatus.HIGH -> {
                    tips.add("Consider consulting a nutritionist for a personalized plan.")
                    tips.add("Aim for 150 minutes of moderate activity per week.")
                }
                else -> {}
            }
        }

        tips.add("Track your weight at the same time daily for accuracy.")
        tips.add("Focus on sustainable lifestyle changes, not crash diets.")
        tips.add("Stay hydrated - sometimes thirst is mistaken for hunger.")

        val overallStatus = analyses.filter { it.metricName != "weight" }.maxOfOrNull { it.status } ?: HealthStatus.NORMAL
        val summary = when (overallStatus) {
            HealthStatus.NORMAL -> "Your metrics are in a healthy range. Maintain your current habits."
            HealthStatus.BORDERLINE -> "Some indicators suggest room for improvement. Small changes help."
            HealthStatus.HIGH -> "Health risk indicators are elevated. Consider professional guidance."
            HealthStatus.LOW -> "Some values are below optimal. Ensure you're eating enough."
        }

        return ReportAnalysis(DiseaseType.OBESITY, analyses, overallStatus, summary, tips.take(4))
    }
}




