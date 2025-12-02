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
import java.text.SimpleDateFormat
import java.util.*

class GroupPostAdapter(
    private val posts: MutableList<CommunityPost>,
    private val context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val groupId: String
) : RecyclerView.Adapter<GroupPostAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val tvLikesCount: TextView = view.findViewById(R.id.tvLikesCount)
        val tvLikeText: TextView = view.findViewById(R.id.tvLikeText)
        val tvCommentText: TextView = view.findViewById(R.id.tvCommentText)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val btnComment: ImageButton = view.findViewById(R.id.btnComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_post, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        val currentUserId = auth.currentUser?.uid ?: ""
        
        holder.tvUserName.text = post.userName.ifEmpty { "Anonymous" }
        holder.tvContent.text = post.content
        
        // Format timestamp
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        holder.tvTimestamp.text = dateFormat.format(Date(post.timestamp))
        
        // Update likes
        val likesCount = post.likes.size
        holder.tvLikesCount.text = if (likesCount == 1) "1 like" else "$likesCount likes"
        
        // Check if user has liked this post
        val isLiked = post.likes.contains(currentUserId)
        holder.btnLike.setImageResource(
            if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
        )
        
        // Like button click
        holder.btnLike.setOnClickListener {
            toggleLike(post, currentUserId, position)
        }
        
        // Comment button click
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
            posts[position] = updatedPost
            notifyItemChanged(position)
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show()
        }
    }
}

