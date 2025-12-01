package com.example.vytal

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class GroupDetailFragment : Fragment() {

    private lateinit var postsRecycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: View
    private lateinit var fabCreatePost: ExtendedFloatingActionButton
    private lateinit var tvGroupName: TextView
    private lateinit var tvGroupDescription: TextView
    private lateinit var chipMemberCount: Chip
    
    private lateinit var adapter: PostsAdapter
    private val postsList = ArrayList<Post>()
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private var groupId: String = ""
    private var groupName: String = ""
    private var groupDescription: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_detail, container, false)
        
        groupId = arguments?.getString("groupId") ?: ""
        
        initViews(view)
        setupRecyclerView()
        loadGroupInfo()
        loadPosts()
        
        fabCreatePost.setOnClickListener {
            showCreatePostDialog()
        }
        
        return view
    }

    private fun initViews(view: View) {
        postsRecycler = view.findViewById(R.id.postsRecycler)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.layoutEmptyState)
        fabCreatePost = view.findViewById(R.id.fabCreatePost)
        tvGroupName = view.findViewById(R.id.tvGroupName)
        tvGroupDescription = view.findViewById(R.id.tvGroupDescription)
        chipMemberCount = view.findViewById(R.id.chipMemberCount)
    }

    private fun setupRecyclerView() {
        postsRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = PostsAdapter(postsList, groupId)
        postsRecycler.adapter = adapter
    }

    private fun loadGroupInfo() {
        if (groupId.isEmpty()) return
        
        db.collection("community_groups")
            .document(groupId)
            .get()
            .addOnSuccessListener { doc ->
                groupName = doc.getString("name") ?: "Group"
                groupDescription = doc.getString("description") ?: ""
                
                tvGroupName.text = groupName
                tvGroupDescription.text = groupDescription
                
                // Load member count
                doc.reference.collection("members")
                    .get()
                    .addOnSuccessListener { members ->
                        chipMemberCount.text = "${members.size()} members"
                    }
            }
    }

    private fun loadPosts() {
        if (groupId.isEmpty()) {
            showEmptyState()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        
        // Real-time listener for posts
        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                progressBar.visibility = View.GONE
                
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading posts: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                
                postsList.clear()
                
                snapshots?.forEach { doc ->
                    val id = doc.id
                    val userId = doc.getString("userId") ?: ""
                    val userName = doc.getString("userName") ?: "Anonymous"
                    val title = doc.getString("title") ?: ""
                    val content = doc.getString("content") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val likes = doc.get("likes") as? List<String> ?: emptyList()
                    val likeCount = doc.getLong("likeCount")?.toInt() ?: likes.size
                    val commentCount = doc.getLong("commentCount")?.toInt() ?: 0
                    val isLiked = likes.contains(currentUserId)
                    
                    postsList.add(Post(id, groupId, userId, userName, title, content, timestamp, likes, likeCount, commentCount, isLiked))
                }
                
                if (postsList.isEmpty()) {
                    showEmptyState()
                } else {
                    emptyStateLayout.visibility = View.GONE
                    postsRecycler.visibility = View.VISIBLE
                }
                
                adapter.notifyDataSetChanged()
            }
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        postsRecycler.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }

    private fun showCreatePostDialog() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "Please login to create posts", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_create_post)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        val etPostTitle = dialog.findViewById<TextInputEditText>(R.id.etPostTitle)
        val etPostContent = dialog.findViewById<TextInputEditText>(R.id.etPostContent)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
        val btnPost = dialog.findViewById<MaterialButton>(R.id.btnPost)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnPost.setOnClickListener {
            val title = etPostTitle?.text?.toString()?.trim() ?: ""
            val content = etPostContent?.text?.toString()?.trim() ?: ""
            
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter post content", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            createPost(title, content)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun createPost(title: String, content: String) {
        val userName = auth.currentUser?.email?.split("@")?.get(0) ?: "Anonymous"
        
        val postData = hashMapOf(
            "userId" to currentUserId,
            "userName" to userName,
            "title" to title,
            "content" to content,
            "timestamp" to System.currentTimeMillis(),
            "likes" to emptyList<String>(),
            "likeCount" to 0,
            "commentCount" to 0
        )
        
        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .add(postData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Post created successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error creating post: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
