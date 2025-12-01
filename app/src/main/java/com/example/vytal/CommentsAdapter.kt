package com.example.vytal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CommentsAdapter(private val comments: ArrayList<Comment>) :
    RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault())

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.commentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        val displayText = "${comment.userName}: ${comment.text}"
        holder.text.text = displayText
    }

    override fun getItemCount(): Int = comments.size
}
