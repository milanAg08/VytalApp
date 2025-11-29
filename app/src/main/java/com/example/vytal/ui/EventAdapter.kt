package com.example.vytal.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.R
import com.example.vytal.model.Event
import com.example.vytal.model.EventJoiner

class EventAdapter(
    private val events: List<Event>,
    private val joiners: Map<String, EventJoiner>,
    private val currentUserId: String?,
    private val onJoinClick: (Event) -> Unit,
    private val onViewDetailsClick: (Event, EventJoiner?) -> Unit
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    private var lastPosition = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewCategoryStrip: View = view.findViewById(R.id.viewCategoryStrip)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvEventTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvEventDescription: TextView = view.findViewById(R.id.tvEventDescription)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvReward: TextView = view.findViewById(R.id.tvReward)
        val btnJoinChallenge: Button = view.findViewById(R.id.btnJoinChallenge)
        val btnViewDetails: Button = view.findViewById(R.id.btnViewDetails)
        val layoutProgress: View = view.findViewById(R.id.layoutProgress)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val tvStreak: TextView = view.findViewById(R.id.tvStreak)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = events.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        val joiner = joiners[event.id]

        // Set category with emoji
        val categoryEmoji = getCategoryEmoji(event.category)
        holder.tvCategory.text = "$categoryEmoji ${event.category}"
        
        // Set category color
        val categoryColor = getCategoryColor(event.category)
        holder.viewCategoryStrip.setBackgroundColor(categoryColor)
        try {
            holder.tvCategory.background.setTint(categoryColor)
        } catch (e: Exception) {
            // Fallback
        }

        holder.tvEventTitle.text = event.title
        holder.tvEventDescription.text = event.description
        holder.tvDuration.text = "${event.duration} days"
        holder.tvReward.text = event.reward

        val isJoined = joiner != null && joiner.uid == currentUserId

        if (isJoined) {
            // User has joined - show progress
            holder.btnJoinChallenge.visibility = View.GONE
            holder.btnViewDetails.visibility = View.VISIBLE
            holder.layoutProgress.visibility = View.VISIBLE

            val progress = if (event.duration > 0) {
                (joiner!!.streak * 100 / event.duration).coerceAtMost(100)
            } else {
                0
            }
            holder.progressBar.progress = progress
            holder.tvStreak.text = "🔥 Day ${joiner!!.streak}/${event.duration}"

            holder.btnViewDetails.setOnClickListener {
                onViewDetailsClick(event, joiner)
            }
            
            holder.itemView.setOnClickListener {
                onViewDetailsClick(event, joiner)
            }
        } else {
            // User hasn't joined - show join button
            holder.btnJoinChallenge.visibility = View.VISIBLE
            holder.btnViewDetails.visibility = View.GONE
            holder.layoutProgress.visibility = View.GONE

            holder.btnJoinChallenge.setOnClickListener {
                onJoinClick(event)
            }
            
            holder.itemView.setOnClickListener {
                onViewDetailsClick(event, null)
            }
        }

        // Add slide animation
        setAnimation(holder.itemView, position)
    }

    private fun getCategoryEmoji(category: String): String {
        return when (category.lowercase()) {
            "yoga" -> "🧘"
            "fitness" -> "💪"
            "diet" -> "🥗"
            "wellness" -> "🧠"
            "health" -> "💚"
            "custom" -> "⭐"
            else -> "🎯"
        }
    }

    private fun getCategoryColor(category: String): Int {
        return when (category.lowercase()) {
            "yoga" -> 0xFF8B5CF6.toInt()      // Purple
            "fitness" -> 0xFFEF4444.toInt()   // Red
            "diet" -> 0xFF10B981.toInt()      // Green
            "wellness" -> 0xFF3B82F6.toInt()  // Blue
            "health" -> 0xFFF97316.toInt()    // Orange
            "custom" -> 0xFFF59E0B.toInt()    // Gold
            else -> 0xFF10B981.toInt()        // Default green
        }
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(viewToAnimate.context, android.R.anim.fade_in)
            animation.duration = 400
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.itemView.clearAnimation()
    }
}
