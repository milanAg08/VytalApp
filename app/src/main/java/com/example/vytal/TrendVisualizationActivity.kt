package com.example.vytal

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.vytal.data.HealthTrendRepository
import com.example.vytal.databinding.ActivityTrendVisualizationBinding
import com.example.vytal.model.*
import com.example.vytal.util.TrendAnalyzer
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrendVisualizationActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var binding: ActivityTrendVisualizationBinding
    private lateinit var repository: HealthTrendRepository
    
    private var diseaseType: DiseaseType = DiseaseType.DIABETES
    private var currentWeekOffset: Int = 0
    private var currentDailyData: List<DailyHealthData> = emptyList()
    private var currentMetrics: List<MetricConfig> = emptyList()
    
    companion object {
        const val EXTRA_DISEASE_TYPE = "disease_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrendVisualizationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        repository = HealthTrendRepository()
        
        // Get disease type from intent
        val diseaseTypeName = intent.getStringExtra(EXTRA_DISEASE_TYPE) ?: DiseaseType.DIABETES.name
        diseaseType = DiseaseType.valueOf(diseaseTypeName)
        
        setupUI()
        setupNavigation()
        loadData()
    }
    
    private fun setupUI() {
        binding.tvDiseaseTitle.text = "${diseaseType.displayName} Trends"
        
        // Setup chart title based on disease
        binding.tvChartTitle.text = when (diseaseType) {
            DiseaseType.DIABETES -> "Blood Glucose Levels"
            DiseaseType.HYPERTENSION -> "Blood Pressure & Pulse"
            DiseaseType.THYROID -> "Thyroid Hormones"
            DiseaseType.ASTHMA -> "Peak Flow Reading"
            DiseaseType.ARTHRITIS -> "Pain & Stiffness"
            DiseaseType.STRESS_FATIGUE -> "Sleep & Stress"
            DiseaseType.OBESITY -> "Weight Management"
        }
        
        // Get metrics for this disease
        currentMetrics = DiseaseMetrics.getMetricsForDisease(diseaseType, this)
        
        // Setup legend
        setupLegend()
        
        // Configure chart appearance
        configureChart(binding.lineChart)
        configureChart(binding.combinedChart)
        configureChart(binding.areaChart)
        
        // Show appropriate chart based on disease
        when (diseaseType) {
            DiseaseType.ASTHMA -> {
                binding.lineChart.visibility = View.GONE
                binding.combinedChart.visibility = View.VISIBLE
            }
            DiseaseType.ARTHRITIS -> {
                binding.lineChart.visibility = View.GONE
                binding.areaChart.visibility = View.VISIBLE
            }
            else -> {
                binding.lineChart.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupLegend() {
        binding.legendContainer.removeAllViews()
        
        for (metric in currentMetrics) {
            val legendItem = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(context, R.drawable.bg_legend_item)
                setPadding(24, 16, 24, 16)
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = 12
                layoutParams = params
            }
            
            // Colored dot
            val dot = View(this).apply {
                val size = 12.dpToPx()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = 8
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(metric.color)
                }
            }
            
            // Label
            val label = TextView(this).apply {
                text = metric.displayName
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.trend_text_secondary))
            }
            
            legendItem.addView(dot)
            legendItem.addView(label)
            binding.legendContainer.addView(legendItem)
        }
    }
    
    private fun setupNavigation() {
        binding.btnBack.setOnClickListener { 
            finish() 
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        
        binding.btnPrevWeek.setOnClickListener {
            currentWeekOffset--
            animateNavigation(true)
            loadData()
        }
        
        binding.btnNextWeek.setOnClickListener {
            if (currentWeekOffset < 0) {
                currentWeekOffset++
                animateNavigation(false)
                loadData()
            }
        }
        
        // Disable next button if on current week
        updateNavigationButtons()
    }
    
    private fun animateNavigation(isPrev: Boolean) {
        val chart = when {
            binding.combinedChart.visibility == View.VISIBLE -> binding.combinedChart
            binding.areaChart.visibility == View.VISIBLE -> binding.areaChart
            else -> binding.lineChart
        }
        
        chart.animate()
            .alpha(0.3f)
            .translationX(if (isPrev) 50f else -50f)
            .setDuration(150)
            .withEndAction {
                chart.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }
    
    private fun updateNavigationButtons() {
        binding.btnNextWeek.isEnabled = currentWeekOffset < 0
        binding.btnNextWeek.alpha = if (currentWeekOffset < 0) 1f else 0.3f
    }
    
    private var loadingJob: kotlinx.coroutines.Job? = null
    
    private fun loadData() {
        val (startTime, endTime) = repository.getWeekBoundaries(currentWeekOffset)
        binding.tvWeekRange.text = repository.formatWeekRange(startTime, endTime)
        updateNavigationButtons()
        
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoData.visibility = View.GONE
        
        // Hide all charts while loading
        binding.lineChart.visibility = View.GONE
        binding.combinedChart.visibility = View.GONE
        binding.areaChart.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
        
        android.util.Log.d("TrendViz", "Loading data for ${diseaseType.name}, week offset: $currentWeekOffset")
        
        // Cancel any existing loading job
        loadingJob?.cancel()
        
        // Add a timeout - if no response in 5 seconds, show empty state
        loadingJob = lifecycleScope.launch {
            var dataReceived = false
            
            // Start a timeout coroutine
            val timeoutJob = launch {
                kotlinx.coroutines.delay(5000) // 5 second timeout
                if (!dataReceived) {
                    android.util.Log.w("TrendViz", "Timeout waiting for Firebase response")
                    binding.progressBar.visibility = View.GONE
                    showNoData()
                }
            }
            
            try {
                repository.getHealthReports(diseaseType, startTime, endTime)
                    .catch { e ->
                        android.util.Log.e("TrendViz", "Error loading data: ${e.message}", e)
                        dataReceived = true
                        timeoutJob.cancel()
                        binding.progressBar.visibility = View.GONE
                        showNoData()
                    }
                    .collectLatest { reports ->
                        dataReceived = true
                        timeoutJob.cancel()
                        android.util.Log.d("TrendViz", "Received ${reports.size} reports")
                        binding.progressBar.visibility = View.GONE
                        
                        if (reports.isEmpty()) {
                            android.util.Log.d("TrendViz", "No reports found, showing empty state")
                            showNoData()
                        } else {
                            currentDailyData = repository.aggregateByDay(reports)
                            android.util.Log.d("TrendViz", "Aggregated to ${currentDailyData.size} days")
                            showChart()
                            updateStats(reports.size, currentDailyData.size)
                            updateAnalysis()
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("TrendViz", "Exception in loadData: ${e.message}", e)
                dataReceived = true
                timeoutJob.cancel()
                binding.progressBar.visibility = View.GONE
                showNoData()
            }
        }
    }
    
    private fun showNoData() {
        android.util.Log.d("TrendViz", "showNoData called")
        
        // Show empty state container
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.lineChart.visibility = View.GONE
        binding.combinedChart.visibility = View.GONE
        binding.areaChart.visibility = View.GONE
        
        binding.tvReadingsCount.text = "0"
        binding.tvDaysTracked.text = "0"
        binding.tvStreak.text = "0"
        
        // Check if user is logged in
        val isLoggedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
        
        if (isLoggedIn) {
            binding.tvAnalysis.text = "No data available for this period. Go back and save a health report first, then return to view trends."
            binding.btnAddFirstReport.text = "Go Back to Add Report"
            
            binding.btnAddFirstReport.setOnClickListener {
                finish()
            }
        } else {
            binding.tvAnalysis.text = "Please log in to view and track your health data."
            binding.btnAddFirstReport.text = "Go Back"
            
            binding.btnAddFirstReport.setOnClickListener {
                finish()
            }
        }
        
        setAnalysisStyle(AnalysisType.STABLE)
    }
    
    private fun showChart() {
        android.util.Log.d("TrendViz", "showChart called for ${diseaseType.name}")
        
        // Hide empty state
        binding.emptyStateContainer.visibility = View.GONE
        
        // Hide all charts first
        binding.lineChart.visibility = View.GONE
        binding.combinedChart.visibility = View.GONE
        binding.areaChart.visibility = View.GONE
        
        when (diseaseType) {
            DiseaseType.ASTHMA -> {
                android.util.Log.d("TrendViz", "Setting up asthma chart")
                binding.combinedChart.visibility = View.VISIBLE
                setupAsthmaChart()
            }
            DiseaseType.ARTHRITIS -> {
                android.util.Log.d("TrendViz", "Setting up arthritis chart")
                binding.areaChart.visibility = View.VISIBLE
                setupArthritisChart()
            }
            else -> {
                android.util.Log.d("TrendViz", "Setting up line chart with ${currentDailyData.size} data points")
                binding.lineChart.visibility = View.VISIBLE
                setupLineChart()
            }
        }
    }
    
    private fun configureChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            legend.isEnabled = false
            setOnChartValueSelectedListener(this@TrendVisualizationActivity)
            
            // X-Axis styling
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(context, R.color.trend_text_secondary)
                textSize = 11f
                granularity = 1f
            }
            
            // Y-Axis styling
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(context, R.color.trend_border)
                textColor = ContextCompat.getColor(context, R.color.trend_text_secondary)
                textSize = 10f
                setDrawAxisLine(false)
            }
            
            axisRight.isEnabled = false
            
            // Extra offsets for rounded look
            setExtraOffsets(16f, 16f, 16f, 16f)
        }
    }
    
    private fun configureChart(chart: CombinedChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            legend.isEnabled = false
            setOnChartValueSelectedListener(this@TrendVisualizationActivity)
            drawOrder = arrayOf(
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
            )
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(context, R.color.trend_text_secondary)
                textSize = 11f
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(context, R.color.trend_border)
                textColor = ContextCompat.getColor(context, R.color.trend_text_secondary)
                textSize = 10f
                setDrawAxisLine(false)
                axisMinimum = 0f
                axisMaximum = 100f
            }
            
            axisRight.isEnabled = false
            setExtraOffsets(16f, 16f, 16f, 16f)
        }
    }
    
    private fun setupLineChart() {
        val chart = binding.lineChart
        val xLabels = currentDailyData.map { it.dayOfWeek }
        
        android.util.Log.d("TrendViz", "setupLineChart - xLabels: $xLabels")
        
        // Clear any existing data and limit lines
        chart.clear()
        chart.axisLeft.removeAllLimitLines()
        
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        chart.xAxis.labelCount = xLabels.size
        
        val dataSets = mutableListOf<LineDataSet>()
        
        for ((index, metric) in currentMetrics.withIndex()) {
            val entries = currentDailyData.mapIndexedNotNull { i, data ->
                data.values[metric.name]?.let { value ->
                    android.util.Log.d("TrendViz", "Entry: day=$i, metric=${metric.name}, value=$value")
                    Entry(i.toFloat(), value)
                }
            }
            
            android.util.Log.d("TrendViz", "Metric ${metric.name}: ${entries.size} entries")
            
            if (entries.isNotEmpty()) {
                val dataSet = createLineDataSet(entries, metric.displayName, metric.color)
                
                // Add dual axis support for stress/fatigue and obesity
                if (metric.isSecondaryAxis) {
                    chart.axisRight.isEnabled = true
                    chart.axisRight.apply {
                        setDrawGridLines(false)
                        textColor = metric.color
                        textSize = 10f
                        setDrawAxisLine(false)
                    }
                    dataSet.axisDependency = YAxis.AxisDependency.RIGHT
                }
                
                dataSets.add(dataSet)
            }
        }
        
        // Add normal range limit lines for first metric
        currentMetrics.firstOrNull()?.let { metric ->
            metric.normalMax?.let { max ->
                val limitLine = LimitLine(max, "Normal").apply {
                    lineColor = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_green)
                    lineWidth = 1f
                    enableDashedLine(10f, 10f, 0f)
                    textColor = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_green)
                    textSize = 9f
                }
                chart.axisLeft.addLimitLine(limitLine)
            }
            metric.warningMax?.let { max ->
                val limitLine = LimitLine(max, "Warning").apply {
                    lineColor = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_red)
                    lineWidth = 1f
                    enableDashedLine(10f, 10f, 0f)
                    textColor = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_red)
                    textSize = 9f
                }
                chart.axisLeft.addLimitLine(limitLine)
            }
        }
        
        android.util.Log.d("TrendViz", "Total datasets: ${dataSets.size}")
        
        if (dataSets.isNotEmpty()) {
            chart.data = LineData(dataSets.toList())
            chart.animateX(800, Easing.EaseInOutCubic)
            android.util.Log.d("TrendViz", "Chart data set with animation")
        } else {
            android.util.Log.d("TrendViz", "No datasets to display!")
            // Show a message if we have daily data but no matching metrics
            if (currentDailyData.isNotEmpty()) {
                android.util.Log.d("TrendViz", "Daily data exists but metrics don't match: ${currentDailyData.first().values.keys}")
            }
        }
        
        chart.invalidate()
    }
    
    private fun setupAsthmaChart() {
        val chart = binding.combinedChart
        val xLabels = currentDailyData.map { it.dayOfWeek }
        
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        chart.xAxis.labelCount = xLabels.size
        
        // Create zone background bars
        val greenZoneEntries = mutableListOf<BarEntry>()
        val yellowZoneEntries = mutableListOf<BarEntry>()
        val redZoneEntries = mutableListOf<BarEntry>()
        
        currentDailyData.forEachIndexed { i, _ ->
            // Full bar backgrounds
            greenZoneEntries.add(BarEntry(i.toFloat(), 100f)) // Top zone (80-100)
            yellowZoneEntries.add(BarEntry(i.toFloat(), 80f))  // Middle zone (50-80)
            redZoneEntries.add(BarEntry(i.toFloat(), 50f))     // Bottom zone (0-50)
        }
        
        // Peak flow line data
        val lineEntries = currentDailyData.mapIndexedNotNull { i, data ->
            data.values["peak_flow"]?.let { Entry(i.toFloat(), it) }
        }
        
        val lineDataSet = createLineDataSet(
            lineEntries, 
            "Peak Flow", 
            ContextCompat.getColor(this, R.color.pastel_blue_dark)
        ).apply {
            lineWidth = 3f
            setDrawCircles(true)
            circleRadius = 6f
            setCircleColor(ContextCompat.getColor(this@TrendVisualizationActivity, R.color.pastel_blue_dark))
            setDrawCircleHole(true)
            circleHoleRadius = 3f
        }
        
        // Create bar data sets for zones (stacked)
        val barDataSet = BarDataSet(greenZoneEntries, "Zones").apply {
            color = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_green_light)
            setDrawValues(false)
        }
        
        val combinedData = CombinedData()
        combinedData.setData(BarData(barDataSet).apply { barWidth = 0.9f })
        combinedData.setData(LineData(lineDataSet))
        
        // Add zone limit lines
        chart.axisLeft.apply {
            removeAllLimitLines()
            addLimitLine(LimitLine(80f, "Green Zone").apply {
                lineColor = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_green)
                lineWidth = 1f
                enableDashedLine(5f, 5f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                textColor = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_green)
            })
            addLimitLine(LimitLine(50f, "Caution").apply {
                lineColor = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_yellow)
                lineWidth = 1f
                enableDashedLine(5f, 5f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                textColor = ContextCompat.getColor(this@TrendVisualizationActivity, R.color.zone_yellow)
            })
        }
        
        chart.data = combinedData
        chart.animateX(800, Easing.EaseInOutCubic)
        chart.invalidate()
    }
    
    private fun setupArthritisChart() {
        val chart = binding.areaChart
        val xLabels = currentDailyData.map { it.dayOfWeek }
        
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        chart.xAxis.labelCount = xLabels.size
        
        val dataSets = mutableListOf<LineDataSet>()
        
        for (metric in currentMetrics) {
            val entries = currentDailyData.mapIndexedNotNull { i, data ->
                data.values[metric.name]?.let { Entry(i.toFloat(), it) }
            }
            
            if (entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, metric.displayName).apply {
                    color = metric.color
                    lineWidth = 2.5f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawCircles(true)
                    circleRadius = 5f
                    setCircleColor(metric.color)
                    setDrawCircleHole(true)
                    circleHoleRadius = 2.5f
                    
                    // Enable fill for area chart effect
                    setDrawFilled(true)
                    fillColor = metric.color
                    fillAlpha = 40
                    
                    setDrawValues(false)
                    highlightLineWidth = 1.5f
                    highLightColor = metric.color
                }
                dataSets.add(dataSet)
            }
        }
        
        if (dataSets.isNotEmpty()) {
            chart.data = LineData(dataSets.toList())
            chart.animateX(800, Easing.EaseInOutCubic)
        }
        
        chart.invalidate()
    }
    
    private fun createLineDataSet(entries: List<Entry>, label: String, color: Int): LineDataSet {
        return LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 2.5f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawCircles(true)
            circleRadius = 5f
            setCircleColor(color)
            setDrawCircleHole(true)
            circleHoleRadius = 2.5f
            circleHoleColor = Color.WHITE
            setDrawValues(false)
            highlightLineWidth = 1.5f
            highLightColor = color
            
            // Subtle gradient fill
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 25
        }
    }
    
    private fun updateStats(totalReadings: Int, daysTracked: Int) {
        binding.tvReadingsCount.text = totalReadings.toString()
        binding.tvDaysTracked.text = daysTracked.toString()
        
        // Calculate streak (consecutive days from today backward)
        val streak = calculateStreak()
        binding.tvStreak.text = streak.toString()
    }
    
    private fun calculateStreak(): Int {
        if (currentDailyData.isEmpty()) return 0
        
        var streak = 0
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val sortedData = currentDailyData.sortedByDescending { it.timestamp }
        
        for (data in sortedData) {
            val dayDiff = ((calendar.timeInMillis - data.timestamp) / (1000 * 60 * 60 * 24)).toInt()
            if (dayDiff == streak) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun updateAnalysis() {
        val (analysisText, analysisType) = TrendAnalyzer.analyze(
            diseaseType, 
            currentDailyData, 
            currentMetrics
        )
        
        binding.tvAnalysis.text = analysisText
        setAnalysisStyle(analysisType)
    }
    
    private fun setAnalysisStyle(type: AnalysisType) {
        val (bgDrawable, textColor) = when (type) {
            AnalysisType.IMPROVEMENT -> Pair(
                R.drawable.bg_analysis_improvement,
                R.color.analysis_improvement
            )
            AnalysisType.ALERT -> Pair(
                R.drawable.bg_analysis_alert,
                R.color.analysis_alert
            )
            AnalysisType.STABLE -> Pair(
                R.drawable.bg_analysis_stable,
                R.color.analysis_stable
            )
        }
        
        binding.analysisCard.setCardBackgroundColor(Color.TRANSPARENT)
        binding.analysisCard.background = ContextCompat.getDrawable(this, bgDrawable)
        
        // Update indicator color
        val indicatorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(this@TrendVisualizationActivity, textColor))
        }
        binding.analysisIndicator.background = indicatorDrawable
    }
    
    // Chart value selection listener
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if (e == null || currentDailyData.isEmpty()) return
        
        val index = e.x.toInt()
        if (index >= currentDailyData.size) return
        
        val dayData = currentDailyData[index]
        
        // Format tooltip
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val date = Date(dayData.timestamp)
        
        binding.tvTooltipDate.text = dateFormat.format(date)
        
        // Build value text from all metrics
        val valueText = buildString {
            dayData.values.forEach { (key, value) ->
                val metric = currentMetrics.find { it.name == key }
                if (metric != null) {
                    if (isNotEmpty()) append("\n")
                    append("${metric.displayName}: ${formatValue(value)} ${metric.unit}")
                }
            }
        }
        binding.tvTooltipValue.text = valueText
        
        binding.tooltipCard.visibility = View.VISIBLE
        binding.tooltipCard.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }
    
    override fun onNothingSelected() {
        binding.tooltipCard.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.tooltipCard.visibility = View.GONE
            }
            .start()
    }
    
    private fun formatValue(value: Float): String {
        return if (value % 1 == 0f) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

