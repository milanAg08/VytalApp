package com.example.vytal.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.vytal.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EventNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun doWork(): Result {
        val currentUser = auth.currentUser ?: return Result.success()

        // Fetch all events user has joined
        db.collectionGroup("joiners")
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                
                for (document in documents) {
                    val joiner = document.toObject(com.example.vytal.model.EventJoiner::class.java)
                    
                    // Check if user hasn't marked progress today
                    if (!joiner.completedDays.contains(today) && !joiner.isCompleted) {
                        // Get event details
                        val pathParts = document.reference.path.split("/")
                        if (pathParts.size >= 2) {
                            val eventId = pathParts[1]
                            db.collection("events")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener { eventDoc ->
                                    if (eventDoc.exists()) {
                                        val event = eventDoc.toObject(com.example.vytal.model.Event::class.java)
                                        event?.let {
                                            sendNotification(it.title, it.description)
                                        }
                                    }
                                }
                        }
                    }
                }
            }

        return Result.success()
    }

    private fun sendNotification(title: String, description: String) {
        val notificationId = System.currentTimeMillis().toInt()
        val notification = NotificationCompat.Builder(applicationContext, "event_channel")
            .setSmallIcon(R.drawable.ic_event)
            .setContentTitle("Don't forget: $title")
            .setContentText("Mark your daily progress to maintain your streak!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }
}


