package com.example.vytal.model

/**
 * Represents a single health metric data point
 */
data class HealthDataPoint(
    val timestamp: Long,          // Unix timestamp
    val value: Float,
    val metricName: String,
    val unit: String = ""
)

/**
 * Represents aggregated daily data (average of multiple readings on same day)
 */
data class DailyHealthData(
    val date: String,             // Format: "MM/dd"
    val dayOfWeek: String,        // "Mon", "Tue", etc.
    val timestamp: Long,
    val values: Map<String, Float>,  // metricName -> averaged value
    val readingsCount: Int
)

/**
 * Disease types supported by the trend visualization
 */
enum class DiseaseType(val displayName: String, val firebaseKey: String) {
    DIABETES("Diabetes", "diabetes"),
    HYPERTENSION("Hypertension", "hypertension"),
    THYROID("Thyroid", "thyroid"),
    ASTHMA("Asthma", "asthma"),
    ARTHRITIS("Arthritis", "arthritis"),
    STRESS_FATIGUE("Stress/Fatigue", "stress_fatigue"),
    OBESITY("Obesity", "obesity")
}

/**
 * Metric configuration for each disease
 */
data class MetricConfig(
    val name: String,
    val displayName: String,
    val unit: String,
    val color: Int,
    val normalMin: Float? = null,
    val normalMax: Float? = null,
    val warningMin: Float? = null,
    val warningMax: Float? = null,
    val isSecondaryAxis: Boolean = false
)

/**
 * Full health trend data for a disease including all metrics and analysis
 */
data class HealthTrendData(
    val diseaseType: DiseaseType,
    val weekStartDate: Long,
    val weekEndDate: Long,
    val dailyData: List<DailyHealthData>,
    val metrics: List<MetricConfig>,
    val analysisText: String = "",
    val analysisType: AnalysisType = AnalysisType.STABLE
)

/**
 * Analysis result type for color coding
 */
enum class AnalysisType {
    IMPROVEMENT,  // Green - positive trend
    ALERT,        // Red - concerning trend
    STABLE        // Blue - no significant change
}

/**
 * Raw Firebase health report entry
 */
data class HealthReport(
    val id: String = "",
    val userId: String = "",
    val diseaseType: String = "",
    val timestamp: Long = 0L,
    val metrics: Map<String, Any> = emptyMap()
) {
    // Firebase requires a no-arg constructor
    constructor() : this("", "", "", 0L, emptyMap())
}

/**
 * Disease-specific metric definitions
 */
object DiseaseMetrics {
    
    fun getMetricsForDisease(diseaseType: DiseaseType, context: android.content.Context): List<MetricConfig> {
        return when (diseaseType) {
            DiseaseType.DIABETES -> listOf(
                MetricConfig(
                    name = "fasting_glucose",
                    displayName = "Fasting Glucose",
                    unit = "mg/dL",
                    color = context.getColor(com.example.vytal.R.color.pastel_blue_dark),
                    normalMin = 70f,
                    normalMax = 100f,
                    warningMax = 126f
                ),
                MetricConfig(
                    name = "pp_glucose",
                    displayName = "Post Prandial",
                    unit = "mg/dL",
                    color = context.getColor(com.example.vytal.R.color.pastel_purple_dark),
                    normalMin = 70f,
                    normalMax = 140f,
                    warningMax = 200f
                ),
                MetricConfig(
                    name = "hba1c",
                    displayName = "HbA1c",
                    unit = "%",
                    color = context.getColor(com.example.vytal.R.color.pastel_green_dark),
                    normalMax = 5.7f,
                    warningMax = 6.5f
                )
            )
            
            DiseaseType.HYPERTENSION -> listOf(
                MetricConfig(
                    name = "systolic",
                    displayName = "Systolic",
                    unit = "mmHg",
                    color = context.getColor(com.example.vytal.R.color.pastel_red_dark),
                    normalMax = 120f,
                    warningMax = 140f
                ),
                MetricConfig(
                    name = "diastolic",
                    displayName = "Diastolic",
                    unit = "mmHg",
                    color = context.getColor(com.example.vytal.R.color.pastel_blue_dark),
                    normalMax = 80f,
                    warningMax = 90f
                ),
                MetricConfig(
                    name = "pulse",
                    displayName = "Pulse",
                    unit = "bpm",
                    color = context.getColor(com.example.vytal.R.color.pastel_pink_dark),
                    normalMin = 60f,
                    normalMax = 100f
                )
            )
            
            DiseaseType.THYROID -> listOf(
                MetricConfig(
                    name = "tsh",
                    displayName = "TSH",
                    unit = "mIU/L",
                    color = context.getColor(com.example.vytal.R.color.pastel_purple_dark),
                    normalMin = 0.4f,
                    normalMax = 4.0f
                ),
                MetricConfig(
                    name = "t3",
                    displayName = "T3",
                    unit = "ng/dL",
                    color = context.getColor(com.example.vytal.R.color.pastel_teal_dark),
                    normalMin = 80f,
                    normalMax = 200f
                ),
                MetricConfig(
                    name = "t4",
                    displayName = "T4",
                    unit = "µg/dL",
                    color = context.getColor(com.example.vytal.R.color.pastel_orange_dark),
                    normalMin = 5.0f,
                    normalMax = 12.0f
                )
            )
            
            DiseaseType.ASTHMA -> listOf(
                MetricConfig(
                    name = "peak_flow",
                    displayName = "Peak Flow",
                    unit = "% predicted",
                    color = context.getColor(com.example.vytal.R.color.pastel_blue_dark),
                    normalMin = 80f,  // Green zone
                    warningMin = 50f,  // Yellow zone below 80%, Red zone below 50%
                    warningMax = 80f
                )
            )
            
            DiseaseType.ARTHRITIS -> listOf(
                MetricConfig(
                    name = "pain_score",
                    displayName = "Pain Score",
                    unit = "/10",
                    color = context.getColor(com.example.vytal.R.color.pastel_red_dark),
                    normalMax = 3f,
                    warningMax = 7f
                ),
                MetricConfig(
                    name = "stiffness_duration",
                    displayName = "Stiffness",
                    unit = "min",
                    color = context.getColor(com.example.vytal.R.color.pastel_orange_dark),
                    normalMax = 30f,
                    warningMax = 60f
                )
            )
            
            DiseaseType.STRESS_FATIGUE -> listOf(
                MetricConfig(
                    name = "sleep_hours",
                    displayName = "Sleep",
                    unit = "hrs",
                    color = context.getColor(com.example.vytal.R.color.pastel_blue_dark),
                    normalMin = 7f,
                    normalMax = 9f
                ),
                MetricConfig(
                    name = "stress_score",
                    displayName = "Stress",
                    unit = "/10",
                    color = context.getColor(com.example.vytal.R.color.pastel_red_dark),
                    normalMax = 4f,
                    warningMax = 7f,
                    isSecondaryAxis = true
                )
            )
            
            DiseaseType.OBESITY -> listOf(
                MetricConfig(
                    name = "weight",
                    displayName = "Weight",
                    unit = "kg",
                    color = context.getColor(com.example.vytal.R.color.pastel_purple_dark)
                ),
                MetricConfig(
                    name = "waist_height_ratio",
                    displayName = "WHtR",
                    unit = "",
                    color = context.getColor(com.example.vytal.R.color.pastel_teal_dark),
                    normalMax = 0.5f,
                    warningMax = 0.6f,
                    isSecondaryAxis = true
                )
            )
        }
    }
}



