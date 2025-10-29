package com.example.vytal.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.R

class CommunityAdapter(private val posts: List<CommunityPost>) :
    RecyclerView.Adapter<CommunityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvUpvotes: TextView = view.findViewById(R.id.tvUpvotes)
        val btnReadMore: Button = view.findViewById(R.id.btnReadMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_post, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        holder.tvTitle.text = post.title
        holder.tvAuthor.text = "By u/${post.author}"
        holder.tvUpvotes.text = "⬆ ${post.upvotes} upvotes"

        holder.btnReadMore.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.postUrl))
            holder.itemView.context.startActivity(intent)
        }
    }
}
