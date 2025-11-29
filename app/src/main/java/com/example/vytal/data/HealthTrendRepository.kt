package com.example.vytal.data

import com.example.vytal.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for fetching health trend data from Firebase
 */
class HealthTrendRepository {
    
    // Use explicit URL to ensure correct database
    private val database: FirebaseDatabase by lazy {
        val db = FirebaseDatabase.getInstance("https://vytal-7e88c-default-rtdb.firebaseio.com")
        android.util.Log.d("HealthTrendRepo", "Using explicit Firebase Database URL: ${db.reference}")
        
        // Ensure we're online
        db.goOnline()
        android.util.Log.d("HealthTrendRepo", "Database set to online mode")
        
        db
    }
    private val auth = FirebaseAuth.getInstance()
    
    private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    
    /**
     * Fetch health reports for a specific disease type within a date range
     */
    fun getHealthReports(
        diseaseType: DiseaseType,
        startTime: Long,
        endTime: Long
    ): Flow<List<HealthReport>> = callbackFlow {
        val userId = auth.currentUser?.uid
        android.util.Log.d("HealthTrendRepo", "getHealthReports called - userId: $userId, disease: ${diseaseType.firebaseKey}")
        
        if (userId == null) {
            android.util.Log.w("HealthTrendRepo", "User not logged in, returning empty list")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val path = "health_reports/$userId/${diseaseType.firebaseKey}"
        android.util.Log.d("HealthTrendRepo", "Querying Firebase path: $path")
        android.util.Log.d("HealthTrendRepo", "Database URL: ${database.reference.toString()}")
        
        val ref = database.getReference("health_reports")
            .child(userId)
            .child(diseaseType.firebaseKey)
        
        android.util.Log.d("HealthTrendRepo", "Full ref path: ${ref.toString()}")
        
        // Use simple single value event instead of continuous listener
        // This is more reliable for one-time reads
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("HealthTrendRepo", "onDataChange called - exists: ${snapshot.exists()}, childrenCount: ${snapshot.childrenCount}")
                
                val reports = mutableListOf<HealthReport>()
                for (child in snapshot.children) {
                    try {
                        val id = child.key ?: continue
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue
                        
                        // Filter by time range manually since ordered query might not work
                        if (timestamp < startTime || timestamp > endTime) {
                            android.util.Log.d("HealthTrendRepo", "Skipping report $id - timestamp $timestamp out of range")
                            continue
                        }
                        
                        val metricsSnapshot = child.child("metrics")
                        
                        val metrics = mutableMapOf<String, Any>()
                        for (metricChild in metricsSnapshot.children) {
                            val key = metricChild.key ?: continue
                            val value = metricChild.getValue(Any::class.java) ?: continue
                            metrics[key] = value
                        }
                        
                        reports.add(HealthReport(
                            id = id,
                            userId = userId,
                            diseaseType = diseaseType.firebaseKey,
                            timestamp = timestamp,
                            metrics = metrics
                        ))
                        android.util.Log.d("HealthTrendRepo", "Parsed report: id=$id, timestamp=$timestamp, metrics=${metrics.keys}")
                    } catch (e: Exception) {
                        android.util.Log.e("HealthTrendRepo", "Error parsing report: ${e.message}")
                    }
                }
                android.util.Log.d("HealthTrendRepo", "Sending ${reports.size} reports")
                trySend(reports)
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("HealthTrendRepo", "Query cancelled: ${error.message}, code: ${error.code}")
                trySend(emptyList())
                close(error.toException())
            }
        }
        
        android.util.Log.d("HealthTrendRepo", "Adding SingleValueEventListener (no query filter)")
        
        // Add a timeout check
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            android.util.Log.e("HealthTrendRepo", "getHealthReports: TIMEOUT - Firebase not responding after 8 seconds")
            android.util.Log.e("HealthTrendRepo", "This usually means:")
            android.util.Log.e("HealthTrendRepo", "1. Firebase Realtime Database is not enabled in Console")
            android.util.Log.e("HealthTrendRepo", "2. Database rules are blocking access")
            android.util.Log.e("HealthTrendRepo", "3. Network connectivity issues")
            trySend(emptyList())
            close()
        }
        timeoutHandler.postDelayed(timeoutRunnable, 8000) // 8 second timeout
        
        // Wrap listener to handle timeout
        val wrappedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                listener.onDataChange(snapshot)
            }
            
            override fun onCancelled(error: DatabaseError) {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                listener.onCancelled(error)
            }
        }
        
        // Use addListenerForSingleValueEvent for more reliable one-time read
        ref.addListenerForSingleValueEvent(wrappedListener)
        
        awaitClose {
            android.util.Log.d("HealthTrendRepo", "Flow closed")
            timeoutHandler.removeCallbacks(timeoutRunnable)
        }
    }
    
    /**
     * Process raw reports into daily aggregated data
     */
    fun aggregateByDay(reports: List<HealthReport>): List<DailyHealthData> {
        if (reports.isEmpty()) return emptyList()
        
        // Group by day
        val byDay = reports.groupBy { report ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = report.timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        
        return byDay.map { (dayTimestamp, dayReports) ->
            val date = Date(dayTimestamp)
            
            // Aggregate metrics - calculate average for each metric
            val aggregatedMetrics = mutableMapOf<String, MutableList<Float>>()
            
            for (report in dayReports) {
                for ((key, value) in report.metrics) {
                    val floatValue = when (value) {
                        is Number -> value.toFloat()
                        is String -> value.toFloatOrNull() ?: continue
                        else -> continue
                    }
                    aggregatedMetrics.getOrPut(key) { mutableListOf() }.add(floatValue)
                }
            }
            
            val averagedMetrics = aggregatedMetrics.mapValues { (_, values) ->
                values.average().toFloat()
            }
            
            DailyHealthData(
                date = dateFormat.format(date),
                dayOfWeek = dayFormat.format(date),
                timestamp = dayTimestamp,
                values = averagedMetrics,
                readingsCount = dayReports.size
            )
        }.sortedBy { it.timestamp }
    }
    
    /**
     * Get week boundaries for pagination
     */
    fun getWeekBoundaries(weekOffset: Int = 0): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        // Go to start of current week (Sunday or Monday based on locale)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Go back to start of week
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromStart = if (dayOfWeek == Calendar.SUNDAY) 0 else dayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromStart)
        
        // Apply week offset (negative for past weeks)
        calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
        
        val startOfWeek = calendar.timeInMillis
        
        // End of week
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfWeek = calendar.timeInMillis
        
        return Pair(startOfWeek, endOfWeek)
    }
    
    /**
     * Format week range for display
     */
    fun formatWeekRange(startTime: Long, endTime: Long): String {
        val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val start = Date(startTime)
        val end = Date(endTime)
        return "${monthDayFormat.format(start)} - ${monthDayFormat.format(end)}"
    }
    
    /**
     * Save a new health report to Firebase
     */
    fun saveHealthReport(
        diseaseType: DiseaseType,
        metrics: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            android.util.Log.e("HealthTrendRepo", "saveHealthReport: User not logged in")
            onError(Exception("User not logged in"))
            return
        }
        
        val ref = database.getReference("health_reports")
            .child(userId)
            .child(diseaseType.firebaseKey)
            .push()
        
        val timestamp = System.currentTimeMillis()
        val report = mapOf(
            "timestamp" to timestamp,
            "metrics" to metrics
        )
        
        android.util.Log.d("HealthTrendRepo", "saveHealthReport: Saving to ${ref.path}")
        android.util.Log.d("HealthTrendRepo", "saveHealthReport: Data = $report")
        android.util.Log.d("HealthTrendRepo", "saveHealthReport: Database URL = ${database.reference}")
        
        // Add timeout handler
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var timeoutRunnable: Runnable? = null
        
        timeoutRunnable = Runnable {
            android.util.Log.e("HealthTrendRepo", "saveHealthReport: TIMEOUT after 10 seconds")
            android.util.Log.e("HealthTrendRepo", "saveHealthReport: This usually means:")
            android.util.Log.e("HealthTrendRepo", "saveHealthReport: 1. Network connectivity issue")
            android.util.Log.e("HealthTrendRepo", "saveHealthReport: 2. Firebase SDK not properly initialized")
            android.util.Log.e("HealthTrendRepo", "saveHealthReport: 3. Database URL incorrect")
            onError(Exception("Save operation timed out. Check Firebase Realtime Database connection."))
        }
        timeoutHandler.postDelayed(timeoutRunnable!!, 10000) // 10 second timeout
        
        android.util.Log.d("HealthTrendRepo", "saveHealthReport: Calling setValue...")
        
        // Try to test connection first with a simple read
        database.reference.child(".info").child("connected").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java)
                android.util.Log.d("HealthTrendRepo", "saveHealthReport: Connection test - connected = $connected")
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("HealthTrendRepo", "saveHealthReport: Connection test failed - code: ${error.code}, message: ${error.message}")
            }
        })
        
        ref.setValue(report)
            .addOnSuccessListener { 
                timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
                android.util.Log.d("HealthTrendRepo", "saveHealthReport: SUCCESS - saved at ${ref.key}")
                onSuccess() 
            }
            .addOnFailureListener { e ->
                timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
                android.util.Log.e("HealthTrendRepo", "saveHealthReport: FAILED - ${e.message}", e)
                android.util.Log.e("HealthTrendRepo", "saveHealthReport: Error type = ${e.javaClass.simpleName}")
                android.util.Log.e("HealthTrendRepo", "saveHealthReport: Error stack trace:")
                e.printStackTrace()
                onError(e) 
            }
    }
    
    /**
     * Generate sample data for testing (remove in production)
     */
    fun generateSampleData(diseaseType: DiseaseType, daysBack: Int = 21) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            android.util.Log.e("HealthTrendRepository", "Cannot generate sample data: User not logged in")
            return
        }
        
        android.util.Log.d("HealthTrendRepository", "Generating sample data for ${diseaseType.firebaseKey} for user $userId")
        
        val random = Random()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
        
        var generatedCount = 0
        
        repeat(daysBack) { day ->
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            
            // Skip some days randomly (30% chance to skip)
            if (random.nextFloat() > 0.7f) return@repeat
            
            val metrics = when (diseaseType) {
                DiseaseType.DIABETES -> mapOf(
                    "fasting_glucose" to (80 + random.nextFloat() * 60),
                    "pp_glucose" to (120 + random.nextFloat() * 80),
                    "hba1c" to (5.0f + random.nextFloat() * 2f)
                )
                DiseaseType.HYPERTENSION -> mapOf(
                    "systolic" to (110 + random.nextFloat() * 40),
                    "diastolic" to (70 + random.nextFloat() * 25),
                    "pulse" to (65 + random.nextFloat() * 35)
                )
                DiseaseType.THYROID -> mapOf(
                    "tsh" to (0.5f + random.nextFloat() * 5f),
                    "t3" to (80 + random.nextFloat() * 120),
                    "t4" to (5 + random.nextFloat() * 8)
                )
                DiseaseType.ASTHMA -> mapOf(
                    "peak_flow" to (50 + random.nextFloat() * 50)
                )
                DiseaseType.ARTHRITIS -> mapOf(
                    "pain_score" to (random.nextFloat() * 10),
                    "stiffness_duration" to (random.nextFloat() * 90)
                )
                DiseaseType.STRESS_FATIGUE -> mapOf(
                    "sleep_hours" to (4 + random.nextFloat() * 6),
                    "stress_score" to (random.nextFloat() * 10)
                )
                DiseaseType.OBESITY -> mapOf(
                    "weight" to (60 + random.nextFloat() * 40),
                    "waist_height_ratio" to (0.4f + random.nextFloat() * 0.3f)
                )
            }
            
            val ref = database.getReference("health_reports")
                .child(userId)
                .child(diseaseType.firebaseKey)
                .push()
            
            val timestamp = calendar.timeInMillis + random.nextInt(86400000) // Random time within day
            
            ref.setValue(mapOf(
                "timestamp" to timestamp,
                "metrics" to metrics
            )).addOnSuccessListener {
                android.util.Log.d("HealthTrendRepository", "Sample data point saved")
            }.addOnFailureListener { e ->
                android.util.Log.e("HealthTrendRepository", "Failed to save sample data: ${e.message}")
            }
            
            generatedCount++
        }
        
        android.util.Log.d("HealthTrendRepository", "Generated $generatedCount sample data points for ${diseaseType.firebaseKey}")
    }
}

