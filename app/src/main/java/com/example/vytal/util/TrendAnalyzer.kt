package com.example.vytal.util

import com.example.vytal.model.*

/**
 * Analyzes health trend data and generates AI-style summaries
 */
object TrendAnalyzer {
    
    /**
     * Analyze trend data and generate a human-readable summary
     */
    fun analyze(
        diseaseType: DiseaseType,
        dailyData: List<DailyHealthData>,
        metrics: List<MetricConfig>
    ): Pair<String, AnalysisType> {
        if (dailyData.isEmpty()) {
            return Pair("No data available for this period.", AnalysisType.STABLE)
        }
        
        return when (diseaseType) {
            DiseaseType.DIABETES -> analyzeDiabetes(dailyData, metrics)
            DiseaseType.HYPERTENSION -> analyzeHypertension(dailyData, metrics)
            DiseaseType.THYROID -> analyzeThyroid(dailyData, metrics)
            DiseaseType.ASTHMA -> analyzeAsthma(dailyData, metrics)
            DiseaseType.ARTHRITIS -> analyzeArthritis(dailyData, metrics)
            DiseaseType.STRESS_FATIGUE -> analyzeStressFatigue(dailyData, metrics)
            DiseaseType.OBESITY -> analyzeObesity(dailyData, metrics)
        }
    }
    
    private fun analyzeDiabetes(
        dailyData: List<DailyHealthData>,
        metrics: List<MetricConfig>
    ): Pair<String, AnalysisType> {
        val fastingValues = dailyData.mapNotNull { it.values["fasting_glucose"] }
        val ppValues = dailyData.mapNotNull { it.values["pp_glucose"] }
        
        if (fastingValues.isEmpty() && ppValues.isEmpty()) {
            return Pair("Add glucose readings to see trend analysis.", AnalysisType.STABLE)
        }
        
        val fastingTrend = calculateTrend(fastingValues)
        val ppTrend = calculateTrend(ppValues)
        
        val fastingAvg = fastingValues.average()
        val ppAvg = ppValues.average()
        
        return when {
            fastingTrend == TrendDirection.DECREASING && fastingAvg < 110 -> {
                Pair("Your fasting glucose shows a positive downward trend this week. Keep up the good work!", AnalysisType.IMPROVEMENT)
            }
            fastingTrend == TrendDirection.INCREASING && fastingAvg > 126 -> {
                Pair("Fasting glucose levels are rising. Consider reviewing your diet and consulting your doctor.", AnalysisType.ALERT)
            }
            ppTrend == TrendDirection.INCREASING && ppAvg > 180 -> {
                Pair("Post-meal glucose is trending higher. Monitor carb intake and meal timing.", AnalysisType.ALERT)
            }
            fastingValues.any { it > 126 } || ppValues.any { it > 200 } -> {
                Pair("Some readings are above target range. Track patterns to discuss with your healthcare provider.", AnalysisType.ALERT)
            }
            else -> {
                Pair("Glucose levels remain stable within acceptable range. Continue monitoring.", AnalysisType.STABLE)
            }
        }
    }
    
    private fun analyzeHypertension(
        dailyData: List<DailyHealthData>,
        metrics: List<MetricConfig>
    ): Pair<String, AnalysisType> {
        val systolicValues = dailyData.mapNotNull { it.values["systolic"] }
        val diastolicValues = dailyData.mapNotNull { it.values["diastolic"] }
        
        if (systolicValues.isEmpty()) {
            return Pair("Add blood pressure readings to see trend analysis.", AnalysisType.STABLE)
        }
        
        val systolicTrend = calculateTrend(systolicValues)
        val systolicAvg = systolicValues.average()
        val diastolicAvg = diastolicValues.average()
        
        val highDays = systolicValues.count { it > 140 }
        
        return when {
            systolicTrend == TrendDirection.DECREASING && systolicAvg < 130 -> {
                Pair("Blood pressure shows improvement this week. Your lifestyle changes are working!", AnalysisType.IMPROVEMENT)
            }
            highDays >= 3 -> {
                Pair("BP levels were elevated on $highDays days—monitor sodium intake and stress levels.", AnalysisType.ALERT)
            }
            systolicTrend == TrendDirection.INCREASING -> {
                Pair("Blood pressure is trending upward. Consider tracking sodium and activity levels.", AnalysisType.ALERT)
            }
            systolicAvg > 135 || diastolicAvg > 85 -> {
                Pair("Average BP is slightly elevated. Regular monitoring recommended.", AnalysisType.ALERT)
            }
            else -> {
                Pair("Blood pressure remains in a healthy range. Keep up the good habits!", AnalysisType.STABLE)
            }
        }
    }
    
    private fun analyzeThyroid(
        dailyData: List<DailyHealthData>,
        metrics: List<MetricConfig>
    ): Pair<String, AnalysisType> {
        val tshValues = dailyData.mapNotNull { it.values["tsh"] }
        
        if (tshValues.isEmpty()) {
            return Pair("Add thyroid test results to see trend analysis.", AnalysisType.STABLE)
        }
        
        val tshAvg = tshValues.average()
        val tshTrend = calculateTrend(tshValues)
        val fluctuation = calculateFluctuation(tshValues)
        
        return when {
            tshAvg < 0.4 -> {
                Pair("TSH is below normal range, suggesting possible hyperthyroidism. Consult your doctor.", AnalysisType.ALERT)
            }
            tshAvg > 4.0 -> {
                Pair("TSH is above normal range, suggesting possible hypothyroidism. Follow up with your doctor.", AnalysisType.ALERT)
            }
            fluctuation > 0.5 -> {
                Pair("TSH shows some fluctuations. Continue monitoring for consistent patterns.", AnalysisType.STABLE)
            }
            tshTrend == TrendDirection.STABLE && tshAvg in 0.4..4.0 -> {
                Pair("TSH appears stable with mild fluctuations. Thyroid function looks healthy.", AnalysisType.IMPROVEMENT)
            }
            else -> {
                Pair("Thyroid markers are within normal range. Continue regular monitoring.", AnalysisType.STABLE)
            }
        }
    }
    
    private fun analyzeAsthma(
        dailyData: List<DailyHealthData>,
        metrics: List<MetricConfig>
    ): Pair<String, AnalysisType> {
        val peakFlowValues = dailyData.mapNotNull { it.values["peak_flow"] }
        
        if (peakFlowValues.isEmpty()) {
            return Pair("Add peak flow readings to see trend analysis.", AnalysisType.STABLE)
        }
        
        val avgPeakFlow = peakFlowValues.average()
        val greenDays = peakFlowValues.count { it >= 80 }
        val yellowDays = peakFlowValues.count { it in 50.0..79.9 }
        val redDays = peakFlowValues.count { it < 50 }
        val trend = calculateTrend(peakFlowValues)
        
        return when {
            redDays > 0 -> {
                Pair("Peak flow was in red zone on $redDays day(s)—ensure rescue inhaler is accessible.", AnalysisType.ALERT)
            }
            greenDays == peakFlowValues.size -> {
                Pair("Peak flow remained in the green zone all week. Excellent respiratory health!", AnalysisType.IMPROVEMENT)
            }
            yellowDays >= 3 -> {
                Pair("Peak flow was in caution zone on $yellowDays days. Review triggers and medication.", AnalysisType.ALERT)
            }
            trend == TrendDirection.INCREASING -> {
                Pair("Peak flow is improving. Your lung function is trending positively.", AnalysisType.IMPROVEMENT)
            }
            else -> {
                Pair("Peak flow mostly in green zone with minor variations. Continue current management.", AnalysisType.STABLE)
            }
        }
    }
    
    private fun analyzeArthritis(
        dailyData: List<DailyHealthData>,
        metrics: List<MetricConfig>
    ): Pair<String, AnalysisType> {
        val painScores = dailyData.mapNotNull { it.values["pain_score"] }
        val stiffnessValues = dailyData.mapNotNull { it.values["stiffness_duration"] }
        
        if (painScores.isEmpty()) {
            return Pair("Add pain and stiffness logs to see trend analysis.", AnalysisType.STABLE)
        }
        
        val avgPain = painScores.average()
        val painTrend = calculateTrend(painScores)
        val highPainDays = painScores.count { it >= 7 }
        
        return when {
            painTrend == TrendDirection.DECREASING && avgPain < 4 -> {
                Pair("Pain levels show improvement this week. Your treatment plan is helping!", AnalysisType.IMPROVEMENT)
            }
            highPainDays >= 3 -> {
                Pair("Pain was severe on $highPainDays days. Consider discussing flare management with your doctor.", AnalysisType.ALERT)
            }
            painTrend == TrendDirection.INCREASING -> {
                Pair("Pain scores are trending upward. Track activities that may trigger flares.", AnalysisType.ALERT)
            }
            stiffnessValues.average() > 45 -> {
                Pair("Morning stiffness is prolonged. Gentle stretching may help.", AnalysisType.ALERT)
            }
            else -> {
                Pair("Joint symptoms are stable. Continue your current management approach.", AnalysisType.STABLE)
            }
        }
    }
    
    private fun analyzeStressFatigue(
        dailyData: List<DailyHealthData>,
        metrics: List<MetricConfig>
    ): Pair<String, AnalysisType> {
        val sleepHours = dailyData.mapNotNull { it.values["sleep_hours"] }
        val stressScores = dailyData.mapNotNull { it.values["stress_score"] }
        
        if (sleepHours.isEmpty() && stressScores.isEmpty()) {
            return Pair("Add sleep and stress data to see trend analysis.", AnalysisType.STABLE)
        }
        
        val avgSleep = sleepHours.average()
        val avgStress = stressScores.average()
        val sleepTrend = calculateTrend(sleepHours)
        val stressTrend = calculateTrend(stressScores)
        
        val poorSleepDays = sleepHours.count { it < 6 }
        val highStressDays = stressScores.count { it >= 7 }
        
        return when {
            avgSleep >= 7 && avgStress < 4 -> {
                Pair("Good sleep quality and low stress levels. You're doing great!", AnalysisType.IMPROVEMENT)
            }
            poorSleepDays >= 4 -> {
                Pair("Sleep has been insufficient on $poorSleepDays days. Prioritize rest and sleep hygiene.", AnalysisType.ALERT)
            }
            highStressDays >= 3 -> {
                Pair("High stress reported on $highStressDays days. Consider relaxation techniques.", AnalysisType.ALERT)
            }
            sleepTrend == TrendDirection.INCREASING && avgSleep < 7 -> {
                Pair("Sleep is improving. Keep building consistent sleep habits.", AnalysisType.IMPROVEMENT)
            }
            stressTrend == TrendDirection.DECREASING -> {
                Pair("Stress levels are decreasing. Your coping strategies are working.", AnalysisType.IMPROVEMENT)
            }
            else -> {
                Pair("Sleep and stress levels are moderate. Focus on consistency.", AnalysisType.STABLE)
            }
        }
    }
    
    private fun analyzeObesity(
        dailyData: List<DailyHealthData>,
        metrics: List<MetricConfig>
    ): Pair<String, AnalysisType> {
        val weightValues = dailyData.mapNotNull { it.values["weight"] }
        val whrValues = dailyData.mapNotNull { it.values["waist_height_ratio"] }
        
        if (weightValues.isEmpty()) {
            return Pair("Add weight measurements to see trend analysis.", AnalysisType.STABLE)
        }
        
        val weightTrend = calculateTrend(weightValues)
        val weightChange = if (weightValues.size >= 2) {
            weightValues.last() - weightValues.first()
        } else 0f
        
        val avgWhr = if (whrValues.isNotEmpty()) whrValues.average() else 0.0
        
        return when {
            weightTrend == TrendDirection.DECREASING && weightChange < -0.5 -> {
                Pair("Weight shows a healthy downward trend. Your efforts are paying off!", AnalysisType.IMPROVEMENT)
            }
            weightTrend == TrendDirection.INCREASING && weightChange > 1 -> {
                Pair("Weight has increased this week. Review diet and activity levels.", AnalysisType.ALERT)
            }
            avgWhr > 0.6 -> {
                Pair("Waist-to-height ratio is elevated. Focus on core exercises and nutrition.", AnalysisType.ALERT)
            }
            avgWhr in 0.5..0.6 && weightTrend != TrendDirection.INCREASING -> {
                Pair("Maintaining stable weight. Continue balanced approach to nutrition.", AnalysisType.STABLE)
            }
            else -> {
                Pair("Weight metrics are stable. Keep tracking for long-term insights.", AnalysisType.STABLE)
            }
        }
    }
    
    // Helper functions
    
    private enum class TrendDirection {
        INCREASING, DECREASING, STABLE
    }
    
    private fun calculateTrend(values: List<Float>): TrendDirection {
        if (values.size < 3) return TrendDirection.STABLE
        
        // Simple linear regression
        val n = values.size
        val xMean = (n - 1) / 2.0
        val yMean = values.average()
        
        var numerator = 0.0
        var denominator = 0.0
        
        values.forEachIndexed { index, value ->
            numerator += (index - xMean) * (value - yMean)
            denominator += (index - xMean) * (index - xMean)
        }
        
        if (denominator == 0.0) return TrendDirection.STABLE
        
        val slope = numerator / denominator
        val threshold = yMean * 0.02 // 2% threshold for significance
        
        return when {
            slope > threshold -> TrendDirection.INCREASING
            slope < -threshold -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun calculateFluctuation(values: List<Float>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        if (mean == 0.0) return 0.0
        
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance) / mean // Coefficient of variation
    }
}

