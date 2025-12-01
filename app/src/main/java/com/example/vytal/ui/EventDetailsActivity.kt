package com.example.vytal.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vytal.R
import com.example.vytal.model.Event
import com.example.vytal.model.EventJoiner
import com.example.vytal.model.UserReward
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var tvEventTitle: TextView
    private lateinit var tvEventDescription: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvReward: TextView
    private lateinit var tvStreak: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnMarkProgress: Button
    private lateinit var tvLeaderboard: TextView
    private lateinit var tvPoints: TextView

    private lateinit var event: Event
    private var joiner: EventJoiner? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        event = intent.getSerializableExtra("event") as Event
        joiner = intent.getSerializableExtra("joiner") as? EventJoiner

        sharedPreferences = getSharedPreferences("event_progress_${event.id}", MODE_PRIVATE)

        initViews()
        setupViews()
        fetchLeaderboard()
    }

    private fun initViews() {
        tvEventTitle = findViewById(R.id.tvEventTitleDetails)
        tvEventDescription = findViewById(R.id.tvEventDescriptionDetails)
        tvDuration = findViewById(R.id.tvDurationDetails)
        tvReward = findViewById(R.id.tvRewardDetails)
        tvStreak = findViewById(R.id.tvStreakDetails)
        progressBar = findViewById(R.id.progressBarDetails)
        btnMarkProgress = findViewById(R.id.btnMarkProgress)
        tvLeaderboard = findViewById(R.id.tvLeaderboard)
        tvPoints = findViewById(R.id.tvPoints)
    }

    private fun setupViews() {
        tvEventTitle.text = event.title
        tvEventDescription.text = event.description
        tvDuration.text = "Duration: ${event.duration} days"
        tvReward.text = "Reward: ${event.reward}"

        if (joiner != null) {
            updateProgressUI()
            btnMarkProgress.setOnClickListener { markDailyProgress() }
        } else {
            tvStreak.text = "Not joined yet"
            progressBar.progress = 0
            tvPoints.text = "Join to start earning points!"
            btnMarkProgress.isEnabled = true
            btnMarkProgress.text = "Join Challenge"
            btnMarkProgress.setOnClickListener { joinChallenge() }
        }
    }

    private fun updateProgressUI() {
        val currentJoiner = joiner ?: return
        val progress = if (event.duration > 0) {
            (currentJoiner.streak * 100 / event.duration).coerceAtMost(100)
        } else {
            0
        }
        progressBar.progress = progress
        tvStreak.text = "Current Streak: ${currentJoiner.streak} days"
        tvPoints.text = "Points: ${currentJoiner.totalPoints}"

        // Check if already marked today
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val isTodayMarked = currentJoiner.completedDays.contains(today)
        
        if (isTodayMarked) {
            btnMarkProgress.text = "✓ Progress marked for today"
            btnMarkProgress.isEnabled = false
        } else {
            btnMarkProgress.text = "Mark Today's Progress"
            btnMarkProgress.isEnabled = true
        }
    }

    private fun joinChallenge() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to join challenges", Toast.LENGTH_SHORT).show()
            return
        }

        val newJoiner = EventJoiner(
            uid = currentUser.uid,
            joinDate = System.currentTimeMillis(),
            streak = 0,
            completedDays = emptyList(),
            totalPoints = 0,
            isCompleted = false
        )

        db.collection("events")
            .document(event.id)
            .collection("joiners")
            .document(currentUser.uid)
            .set(newJoiner)
            .addOnSuccessListener {
                joiner = newJoiner
                setupViews()
                Toast.makeText(this, "Successfully joined challenge!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("EventDetailsActivity", "Error joining challenge", e)
                Toast.makeText(this, "Failed to join challenge. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markDailyProgress() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to track progress", Toast.LENGTH_SHORT).show()
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentJoiner = joiner ?: return

        // Check if already marked today
        if (currentJoiner.completedDays.contains(today)) {
            Toast.makeText(this, "You've already marked progress for today!", Toast.LENGTH_SHORT).show()
            return
        }

        // Update local SharedPreferences first
        val editor = sharedPreferences.edit()
        val completedDaysSet = currentJoiner.completedDays.toMutableSet()
        completedDaysSet.add(today)
        editor.putStringSet("completed_days", completedDaysSet)
        editor.putInt("streak", currentJoiner.streak + 1)
        editor.apply()

        // Calculate new streak (consecutive days)
        val newCompletedDays = completedDaysSet.toList().sorted()
        val newStreak = calculateStreak(newCompletedDays)
        val pointsEarned = 10 // Points per day
        val newTotalPoints = currentJoiner.totalPoints + pointsEarned

        // Check if challenge is completed
        val isCompleted = newStreak >= event.duration

        val updatedJoiner = currentJoiner.copy(
            streak = newStreak,
            completedDays = newCompletedDays,
            totalPoints = newTotalPoints,
            isCompleted = isCompleted
        )

        // Update Firestore
        db.collection("events")
            .document(event.id)
            .collection("joiners")
            .document(currentUser.uid)
            .set(updatedJoiner)
            .addOnSuccessListener {
                joiner = updatedJoiner
                updateProgressUI()
                
                // Update user rewards
                updateUserRewards(pointsEarned)
                
                // Check if challenge completed
                if (isCompleted) {
                    Toast.makeText(this, "🎉 Challenge completed! You earned ${event.reward}!", Toast.LENGTH_LONG).show()
                    // Award completion badge
                    awardBadge("challenge_completed_${event.id}")
                } else {
                    Toast.makeText(this, "Great job! Keep it up! +$pointsEarned points", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EventDetailsActivity", "Error updating progress", e)
                Toast.makeText(this, "Failed to update progress. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateStreak(completedDays: List<String>): Int {
        if (completedDays.isEmpty()) return 0
        
        val sortedDays = completedDays.sorted().reversed()
        var streak = 0
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        for (i in sortedDays.indices) {
            val dayStr = sortedDays[i]
            val dayDate = dateFormat.parse(dayStr) ?: continue
            calendar.time = dayDate
            
            if (i == 0) {
                // First day - check if it's today or yesterday
                val today = Calendar.getInstance()
                val diff = (today.timeInMillis - calendar.timeInMillis) / (1000 * 60 * 60 * 24)
                if (diff <= 1) {
                    streak = 1
                } else {
                    break
                }
            } else {
                // Check if consecutive
                val prevDayStr = sortedDays[i - 1]
                val prevDayDate = dateFormat.parse(prevDayStr) ?: continue
                val prevCalendar = Calendar.getInstance().apply { time = prevDayDate }
                prevCalendar.add(Calendar.DAY_OF_YEAR, -1)
                
                if (dateFormat.format(prevCalendar.time) == dayStr) {
                    streak++
                } else {
                    break
                }
            }
        }
        
        return streak
    }

    private fun updateUserRewards(points: Int) {
        val currentUser = auth.currentUser ?: return
        
        db.collection("users")
            .document(currentUser.uid)
            .collection("rewards")
            .document("user_rewards")
            .get()
            .addOnSuccessListener { document ->
                val currentReward = if (document.exists()) {
                    document.toObject(UserReward::class.java) ?: UserReward()
                } else {
                    UserReward()
                }
                
                val updatedReward = currentReward.copy(
                    points = currentReward.points + points,
                    lastUpdated = System.currentTimeMillis()
                )
                
                db.collection("users")
                    .document(currentUser.uid)
                    .collection("rewards")
                    .document("user_rewards")
                    .set(updatedReward)
            }
    }

    private fun awardBadge(badgeName: String) {
        val currentUser = auth.currentUser ?: return
        
        db.collection("users")
            .document(currentUser.uid)
            .collection("rewards")
            .document("user_rewards")
            .get()
            .addOnSuccessListener { document ->
                val currentReward = if (document.exists()) {
                    document.toObject(UserReward::class.java) ?: UserReward()
                } else {
                    UserReward()
                }
                
                val badges = currentReward.badges.toMutableList()
                if (!badges.contains(badgeName)) {
                    badges.add(badgeName)
                }
                
                val updatedReward = currentReward.copy(
                    badges = badges,
                    lastUpdated = System.currentTimeMillis()
                )
                
                db.collection("users")
                    .document(currentUser.uid)
                    .collection("rewards")
                    .document("user_rewards")
                    .set(updatedReward)
            }
    }

    private fun fetchLeaderboard() {
        db.collection("events")
            .document(event.id)
            .collection("joiners")
            .orderBy("totalPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val leaderboardText = StringBuilder("🏆 Top Participants:\n\n")
                var rank = 1
                
                for (document in documents) {
                    val joiner = document.toObject(EventJoiner::class.java)
                    // Get username from users collection (simplified - you might want to cache this)
                    leaderboardText.append("$rank. User ${joiner.uid.take(8)}... - ${joiner.totalPoints} pts (${joiner.streak} days)\n")
                    rank++
                }
                
                if (documents.isEmpty()) {
                    tvLeaderboard.text = "No participants yet. Be the first!"
                } else {
                    tvLeaderboard.text = leaderboardText.toString()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EventDetailsActivity", "Error fetching leaderboard", e)
                tvLeaderboard.text = "Unable to load leaderboard"
            }
    }
}

