package com.example.vytal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostsAdapter(
    private val posts: ArrayList<Post>,
    private val groupId: String
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().uid

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userProfilePic: ImageView = view.findViewById(R.id.userProfilePic)
        val userName: TextView = view.findViewById(R.id.userName)
        val postText: TextView = view.findViewById(R.id.postText)
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val likeBtn: ImageView = view.findViewById(R.id.likeBtn)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val commentBtn: ImageView = view.findViewById(R.id.commentBtn)
        val postTime: TextView = view.findViewById(R.id.postTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // ⭐ CHECK userId
        if (post.userId.isNullOrEmpty()) {
            holder.userName.text = "Unknown User"
            holder.userProfilePic.setImageResource(R.drawable.ic_profile_placeholder)
        } else {

            // ⭐ Load user details from Firestore
            db.collection("users")
                .document(post.userId!!)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("name") ?: "Unknown"
                        val photoUrl = doc.getString("photoUrl") ?: ""

                        holder.userName.text = name

                        if (photoUrl.isNotEmpty()) {
                            Glide.with(holder.itemView.context)
                                .load(photoUrl)
                                .circleCrop()
                                .into(holder.userProfilePic)
                        } else {
                            holder.userProfilePic.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                }
        }

        // POST TEXT
        holder.postText.text = post.text

        // POST IMAGE
        if (!post.imageUrl.isNullOrEmpty()) {
            holder.postImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .into(holder.postImage)
        } else {
            holder.postImage.visibility = View.GONE
        }

        // TIME AGO
        holder.postTime.text = getTimeAgo(post.timestamp)

        // LIKES
        holder.likeCount.text = "${post.likes.size} likes"

        if (post.likes.contains(uid)) {
            holder.likeBtn.setImageResource(R.drawable.ic_like_filled)
        } else {
            holder.likeBtn.setImageResource(R.drawable.ic_like_outline)
        }

        holder.likeBtn.setOnClickListener {
            toggleLike(post)
        }

        holder.commentBtn.setOnClickListener {
            val activity = holder.itemView.context as AppCompatActivity
            CommentsBottomSheet.newInstance(groupId, post.postId)
                .show(activity.supportFragmentManager, "comments")
        }
    }

    override fun getItemCount(): Int = posts.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    private fun toggleLike(post: Post) {
        val postRef = db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .document(post.postId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("likes") as ArrayList<String>

            if (uid != null) {
                if (likes.contains(uid)) likes.remove(uid)
                else likes.add(uid)
            }

            transaction.update(postRef, "likes", likes)
        }
    }
}
