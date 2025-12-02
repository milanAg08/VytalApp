package com.example.vytal.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.R
import com.example.vytal.model.CommunityPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

sealed class PostItem {
    data class FirestorePost(val post: CommunityPost) : PostItem()
    data class RedditPostItem(val post: RedditPost) : PostItem()
}

class UnifiedPostAdapter(
    private val items: MutableList<PostItem>,
    private val context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RecyclerView.Adapter<UnifiedPostAdapter.ViewHolder>() {

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

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PostItem.FirestorePost -> {
                bindFirestorePost(holder, item.post, position)
            }
            is PostItem.RedditPostItem -> {
                bindRedditPost(holder, item.post)
            }
        }
    }
    
    private fun bindFirestorePost(holder: ViewHolder, post: CommunityPost, position: Int) {
        val currentUserId = auth.currentUser?.uid ?: ""
        
        holder.tvUserName.text = post.userName.ifEmpty { "Anonymous" }
        holder.tvContent.text = post.content
        holder.tvLikesCount.text = post.likes.size.toString()
        holder.tvCommentsCount.text = post.commentsCount.toString()
        
        val timeAgo = formatTimeAgo(post.timestamp)
        holder.tvTimestamp.text = timeAgo
        
        val isLiked = post.likes.contains(currentUserId)
        holder.btnLike.setImageResource(
            if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
        )
        
        holder.btnLike.visibility = View.VISIBLE
        holder.btnComment.visibility = View.VISIBLE
        
        holder.btnLike.setOnClickListener {
            toggleLike(post, currentUserId, position)
        }
        
        holder.btnComment.setOnClickListener {
            if (post.id.isNotEmpty() && post.groupId.isNotEmpty()) {
                val dialog = CommentsDialog.newInstance(post.id, post.groupId)
                val fragmentManager = (context as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
                fragmentManager?.let {
                    dialog.show(it, "CommentsDialog")
                } ?: run {
                    Toast.makeText(context, "Unable to open comments", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun bindRedditPost(holder: ViewHolder, post: RedditPost) {
        holder.tvUserName.text = "🔴 u/${post.author}"
        holder.tvContent.text = post.title
        holder.tvLikesCount.text = "⬆ ${post.upvotes}"
        holder.tvCommentsCount.text = "0"
        holder.tvTimestamp.text = "From Reddit"
        holder.tvTimestamp.setTextColor(0xFFFF6B6B.toInt()) // Red color for Reddit indicator
        
        holder.btnLike.visibility = View.GONE
        holder.btnComment.visibility = View.GONE
        
        // Make the entire card clickable
        holder.itemView.setOnClickListener {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(post.postUrl))
                holder.itemView.context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(holder.itemView.context, "Unable to open link: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Add a subtle visual indicator that this is a Reddit post
        holder.itemView.alpha = 0.95f
    }
    
    private fun toggleLike(post: CommunityPost, userId: String, position: Int) {
        if (post.id.isEmpty() || post.groupId.isEmpty()) {
            Toast.makeText(context, "Error: Post ID missing", Toast.LENGTH_SHORT).show()
            return
        }
        
        val postRef = db.collection("community_groups")
            .document(post.groupId)
            .collection("posts")
            .document(post.id)
        
        val isLiked = post.likes.contains(userId)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikes = snapshot.get("likes") as? List<*> ?: emptyList<Any>()
            val likesList = currentLikes.mapNotNull { it as? String }.toMutableList()
            
            if (isLiked) {
                likesList.remove(userId)
            } else {
                if (!likesList.contains(userId)) {
                    likesList.add(userId)
                }
            }
            
            transaction.update(postRef, "likes", likesList)
            likesList
        }.addOnSuccessListener { likesList ->
            val updatedPost = post.copy(likes = likesList)
            items[position] = PostItem.FirestorePost(updatedPost)
            notifyItemChanged(position)
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show()
        }
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
}

