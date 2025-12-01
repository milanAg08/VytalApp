package com.example.vytal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(
    private val posts: ArrayList<Post>,
    private val groupId: String
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvPostTitle: TextView = itemView.findViewById(R.id.tvPostTitle)
        val tvPostContent: TextView = itemView.findViewById(R.id.tvPostContent)
        val btnLike: MaterialButton = itemView.findViewById(R.id.btnLike)
        val btnComment: MaterialButton = itemView.findViewById(R.id.btnComment)
        val tvExpandComments: TextView = itemView.findViewById(R.id.tvExpandComments)
        val layoutComments: LinearLayout = itemView.findViewById(R.id.layoutComments)
        val commentsRecycler: RecyclerView = itemView.findViewById(R.id.commentsRecycler)
        val etCommentInput: EditText = itemView.findViewById(R.id.etCommentInput)
        val btnSubmitComment: MaterialButton = itemView.findViewById(R.id.btnSubmitComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        
        holder.tvUserName.text = post.userName
        holder.tvPostTitle.text = post.title
        holder.tvPostContent.text = post.content
        holder.tvTimestamp.text = formatTimestamp(post.timestamp)
        
        // Update like button
        updateLikeButton(holder, post)
        
        // Update comment count
        holder.btnComment.text = "${post.commentCount}"
        
        // Like button click
        holder.btnLike.setOnClickListener {
            toggleLike(post, holder)
        }
        
        // Expand/collapse comments
        var isCommentsExpanded = false
        holder.tvExpandComments.setOnClickListener {
            isCommentsExpanded = !isCommentsExpanded
            if (isCommentsExpanded) {
                holder.layoutComments.visibility = View.VISIBLE
                holder.tvExpandComments.text = "Hide comments"
                loadComments(post.id, holder)
            } else {
                holder.layoutComments.visibility = View.GONE
                holder.tvExpandComments.text = "View comments"
            }
        }
        
        // Submit comment
        holder.btnSubmitComment.setOnClickListener {
            val commentText = holder.etCommentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(post.id, commentText, holder)
                holder.etCommentInput.text.clear()
            }
        }
    }

    private fun updateLikeButton(holder: PostViewHolder, post: Post) {
        val isLiked = post.likes.contains(currentUserId)
        val likeCount = post.likes.size
        
        holder.btnLike.text = likeCount.toString()
        
        if (isLiked) {
            holder.btnLike.setIconResource(android.R.drawable.btn_star_big_on)
            holder.btnLike.iconTint = holder.itemView.context.getColorStateList(R.color.buttonGreenDark)
            holder.btnLike.setTextColor(holder.itemView.context.getColor(R.color.buttonGreenDark))
        } else {
            holder.btnLike.setIconResource(android.R.drawable.btn_star_big_off)
            holder.btnLike.iconTint = holder.itemView.context.getColorStateList(android.R.color.darker_gray)
            holder.btnLike.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }
    }

    private fun toggleLike(post: Post, holder: PostViewHolder) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(holder.itemView.context, "Please login to like posts", Toast.LENGTH_SHORT).show()
            return
        }
        
        val postRef = db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .document(post.id)
        
        val isLiked = post.likes.contains(currentUserId)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikes = snapshot.get("likes") as? List<String> ?: emptyList()
            val newLikes = if (isLiked) {
                currentLikes.filter { it != currentUserId }
            } else {
                currentLikes + currentUserId
            }
            transaction.update(postRef, "likes", newLikes)
            transaction.update(postRef, "likeCount", newLikes.size)
        }.addOnSuccessListener {
            // Update local post
            val updatedLikes = if (isLiked) {
                post.likes.filter { it != currentUserId }
            } else {
                post.likes + currentUserId
            }
            val updatedPost = post.copy(likes = updatedLikes, likeCount = updatedLikes.size)
            posts[holder.adapterPosition] = updatedPost
            updateLikeButton(holder, updatedPost)
        }.addOnFailureListener {
            Toast.makeText(holder.itemView.context, "Error updating like", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadComments(postId: String, holder: PostViewHolder) {
        val commentsRef = db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp")
        
        commentsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            
            val comments = ArrayList<Comment>()
            snapshots?.forEach { doc ->
                val text = doc.getString("text") ?: ""
                val userId = doc.getString("userId") ?: ""
                val userName = doc.getString("userName") ?: "Anonymous"
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                comments.add(Comment(doc.id, text, userId, userName, timestamp))
            }
            
            holder.commentsRecycler.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.commentsRecycler.adapter = CommentsAdapter(comments)
        }
    }

    private fun addComment(postId: String, text: String, holder: PostViewHolder) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(holder.itemView.context, "Please login to comment", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userName = auth.currentUser?.email?.split("@")?.get(0) ?: "Anonymous"
        
        val commentData = hashMapOf(
            "text" to text,
            "userId" to currentUserId,
            "userName" to userName,
            "timestamp" to System.currentTimeMillis()
        )
        
        val postRef = db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .document(postId)
        
        // Add comment
        postRef.collection("comments")
            .add(commentData)
            .addOnSuccessListener {
                // Update comment count
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val currentCount = snapshot.getLong("commentCount") ?: 0L
                    transaction.update(postRef, "commentCount", currentCount + 1)
                }
            }
            .addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Error adding comment", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> dateFormat.format(Date(timestamp))
        }
    }

    override fun getItemCount(): Int = posts.size
}

