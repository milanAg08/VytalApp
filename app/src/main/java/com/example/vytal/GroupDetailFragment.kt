package com.example.vytal

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.model.CommunityPost
import com.example.vytal.ui.GroupPostAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class GroupDetailFragment : Fragment() {

    private lateinit var postsRecycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvGroupName: TextView
    private lateinit var tvGroupDescription: TextView
    private lateinit var tvMemberCount: TextView
    private lateinit var fabCreatePost: FloatingActionButton
    private lateinit var adapter: GroupPostAdapter
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postsList = mutableListOf<CommunityPost>()
    private var groupId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_detail, container, false)

        groupId = arguments?.getString("groupId") ?: ""
        
        if (groupId.isEmpty()) {
            Toast.makeText(requireContext(), "Group ID missing", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return view
        }

        postsRecycler = view.findViewById(R.id.postsRecycler)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvGroupName = view.findViewById(R.id.tvGroupName)
        tvGroupDescription = view.findViewById(R.id.tvGroupDescription)
        tvMemberCount = view.findViewById(R.id.tvMemberCount)
        fabCreatePost = view.findViewById(R.id.fabCreatePost)

        postsRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = GroupPostAdapter(postsList, requireContext(), db, auth, groupId)
        postsRecycler.adapter = adapter

        // Load group info
        loadGroupInfo()

        // Load posts
        loadPosts()

        // Create post FAB
        fabCreatePost.setOnClickListener {
            showCreatePostDialog()
        }

        return view
    }

    private fun loadGroupInfo() {
        db.collection("community_groups")
            .document(groupId)
            .get()
            .addOnSuccessListener { doc ->
                val groupName = doc.getString("name") ?: "Community"
                val groupDesc = doc.getString("description") ?: ""
                val members = doc.get("members") as? List<*> ?: emptyList<Any>()
                val memberCount = members.size
                
                tvGroupName.text = groupName
                tvGroupDescription.text = groupDesc
                tvMemberCount.text = "$memberCount members"
            }
    }

    private fun loadPosts() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE

                if (error != null) {
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Error loading posts: ${error.message}"
                    return@addSnapshotListener
                }

                postsList.clear()
                snapshot?.documents?.forEach { doc ->
                    val post = doc.toObject(CommunityPost::class.java)
                    post?.let {
                        postsList.add(it.copy(id = doc.id, groupId = groupId))
                    }
                }

                adapter.notifyDataSetChanged()

                if (postsList.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "No posts yet. Be the first to share!"
                } else {
                    tvEmptyState.visibility = View.GONE
                }
            }
    }

    private fun showCreatePostDialog() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to create a post", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = View.inflate(requireContext(), R.layout.dialog_create_post, null)

        val etContent = dialogView.findViewById<EditText>(R.id.etPostContent)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create Post")
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val content = etContent.text.toString().trim()
                if (content.isNotEmpty()) {
                    createPost(content)
                } else {
                    Toast.makeText(requireContext(), "Please enter some content", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun createPost(content: String) {
        val currentUser = auth.currentUser ?: return
        val userName = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "Anonymous"

        val post = hashMapOf(
            "groupId" to groupId,
            "userId" to currentUser.uid,
            "userName" to userName,
            "content" to content,
            "imageUrl" to "",
            "timestamp" to System.currentTimeMillis(),
            "likes" to emptyList<String>(),
            "commentsCount" to 0
        )

        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .add(post)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Post created!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to create post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
