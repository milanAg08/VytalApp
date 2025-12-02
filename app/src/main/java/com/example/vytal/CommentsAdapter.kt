package com.example.vytal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommentsAdapter(private val comments: ArrayList<Comment>) :
    RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.commentText)
        val userName: TextView = itemView.findViewById(R.id.commentUserName)
        val timestamp: TextView = itemView.findViewById(R.id.commentTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        holder.text.text = comment.text
        holder.userName.text = comment.userName.ifEmpty { "Anonymous" }
        
        // Format timestamp
        val timeAgo = formatTimeAgo(comment.timestamp)
        holder.timestamp.text = timeAgo
    }
    
    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "$days d ago"
            hours > 0 -> "$hours h ago"
            minutes > 0 -> "$minutes m ago"
            else -> "Just now"
        }
    }

    override fun getItemCount(): Int = comments.size
}
