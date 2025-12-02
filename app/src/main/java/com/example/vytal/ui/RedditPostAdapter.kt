package com.example.vytal.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.R

data class RedditPost(
    val title: String,
    val author: String,
    val postUrl: String,
    val upvotes: Int
)

class RedditPostAdapter(private val posts: List<RedditPost>) :
    RecyclerView.Adapter<RedditPostAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val tvLikesCount: TextView = view.findViewById(R.id.tvLikesCount)
        val tvCommentsCount: TextView = view.findViewById(R.id.tvCommentsCount)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val btnComment: ImageButton = view.findViewById(R.id.btnComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_post, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        
        holder.tvUserName.text = "u/${post.author}"
        holder.tvContent.text = post.title
        holder.tvLikesCount.text = post.upvotes.toString()
        holder.tvCommentsCount.text = "0"
        holder.tvTimestamp.text = "Reddit"
        
        // Hide like and comment buttons for Reddit posts, show read more instead
        holder.btnLike.visibility = View.GONE
        holder.btnComment.visibility = View.GONE
        holder.tvLikesCount.text = "⬆ ${post.upvotes}"
        
        // Make entire card clickable to open Reddit post
        holder.itemView.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.postUrl))
                holder.itemView.context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(holder.itemView.context, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

