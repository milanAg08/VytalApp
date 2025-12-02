package com.example.vytal.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.vytal.CommunityGroupsFragment
import com.example.vytal.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.json.JSONObject

class CommunityFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: UnifiedPostAdapter
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postsList = mutableListOf<PostItem>()
    private var redditPostsLoaded = false
    private var firestorePostsLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community, container, false)
        
        recyclerView = view.findViewById(R.id.recyclerViewCommunity)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UnifiedPostAdapter(postsList, requireContext(), db, auth)
        recyclerView.adapter = adapter
        
        // Setup Join Communities button
        val btnJoin = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnJoinCommunities)
        btnJoin.setOnClickListener {
            try {
                val activity = requireActivity()
                val containerId = when (activity::class.java.simpleName) {
                    "MainActivity" -> R.id.fragment_container
                    "HomeActivity" -> R.id.home_container
                    else -> {
                        if (activity.findViewById<View>(R.id.fragment_container) != null) {
                            R.id.fragment_container
                        } else {
                            R.id.home_container
                        }
                    }
                }
                
                parentFragmentManager.beginTransaction()
                    .replace(containerId, CommunityGroupsFragment())
                    .addToBackStack(null)
                    .commit()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error navigating to groups: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Load Reddit posts first, then community posts
        progressBar.visibility = View.VISIBLE
        loadRedditPosts()
        loadCommunityPosts()
        
        return view
    }

    private fun loadCommunityPosts() {
        val currentUserId = auth.currentUser?.uid ?: ""
        
        if (currentUserId.isNotEmpty()) {
            db.collection("community_groups")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener { groupsSnapshot ->
                    val groupIds = groupsSnapshot.documents.map { it.id }
                    
                    if (groupIds.isNotEmpty()) {
                        loadPostsFromGroups(groupIds)
                    } else {
                        firestorePostsLoaded = true
                        checkAndUpdateUI()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CommunityFragment", "Error loading groups: ${e.message}")
                    firestorePostsLoaded = true
                    checkAndUpdateUI()
                }
        } else {
            firestorePostsLoaded = true
            checkAndUpdateUI()
        }
    }
    
    private fun loadPostsFromGroups(groupIds: List<String>) {
        if (groupIds.isNotEmpty()) {
            db.collection("community_groups")
                .document(groupIds[0])
                .collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        val post = doc.toObject(com.example.vytal.model.CommunityPost::class.java)
                        post?.let {
                            val firestorePost = PostItem.FirestorePost(it.copy(id = doc.id, groupId = groupIds[0]))
                            // Add to beginning of list
                            if (!postsList.any { item -> 
                                item is PostItem.FirestorePost && (item as PostItem.FirestorePost).post.id == doc.id 
                            }) {
                                postsList.add(0, firestorePost)
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                    firestorePostsLoaded = true
                    checkAndUpdateUI()
                }
                .addOnFailureListener { e ->
                    Log.e("CommunityFragment", "Error loading posts: ${e.message}")
                    firestorePostsLoaded = true
                    checkAndUpdateUI()
                }
        } else {
            firestorePostsLoaded = true
            checkAndUpdateUI()
        }
    }
    
    private fun loadRedditPosts() {
        val url = "https://www.reddit.com/r/fitness/top.json?limit=10&t=week"
        val queue = Volley.newRequestQueue(requireContext())
        
        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val redditPosts = mutableListOf<PostItem>()
                    val children = response.getJSONObject("data").getJSONArray("children")

                    for (i in 0 until children.length()) {
                        val data = children.getJSONObject(i).getJSONObject("data")
                        val title = data.getString("title")
                        val author = data.optString("author", "Reddit User")
                        val postUrl = "https://www.reddit.com" + data.optString("permalink", "")
                        val upvotes = data.optInt("ups", 0)

                        val redditPost = RedditPost(title, author, postUrl, upvotes)
                        redditPosts.add(PostItem.RedditPostItem(redditPost))
                    }
                    
                    // Add Reddit posts to the list (at the end)
                    if (redditPosts.isNotEmpty()) {
                        postsList.addAll(redditPosts)
                        adapter.notifyDataSetChanged()
                        Log.d("CommunityFragment", "Added ${redditPosts.size} Reddit posts to list. Total posts: ${postsList.size}")
                    } else {
                        Log.w("CommunityFragment", "No Reddit posts to add")
                    }
                    redditPostsLoaded = true
                    checkAndUpdateUI()
                } catch (e: Exception) {
                    Log.e("CommunityFragment", "Error parsing Reddit response: ${e.message}", e)
                    redditPostsLoaded = true
                    checkAndUpdateUI()
                }
            },
            { error ->
                Log.e("CommunityFragment", "Error loading Reddit posts: ${error.message}", error)
                // Try alternative subreddit or show error
                loadAlternativeRedditPosts()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "VytalApp/1.0"
                return headers
            }
        }

        queue.add(request)
    }
    
    private fun loadAlternativeRedditPosts() {
        // Try a different subreddit as fallback
        val url = "https://www.reddit.com/r/health/top.json?limit=10&t=week"
        val queue = Volley.newRequestQueue(requireContext())
        
        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val redditPosts = mutableListOf<PostItem>()
                    val children = response.getJSONObject("data").getJSONArray("children")

                    for (i in 0 until children.length()) {
                        val data = children.getJSONObject(i).getJSONObject("data")
                        val title = data.getString("title")
                        val author = data.optString("author", "Reddit User")
                        val postUrl = "https://www.reddit.com" + data.optString("permalink", "")
                        val upvotes = data.optInt("ups", 0)

                        val redditPost = RedditPost(title, author, postUrl, upvotes)
                        redditPosts.add(PostItem.RedditPostItem(redditPost))
                    }
                    
                    postsList.addAll(redditPosts)
                    adapter.notifyDataSetChanged()
                    redditPostsLoaded = true
                    Log.d("CommunityFragment", "Loaded ${redditPosts.size} Reddit posts from r/health")
                    checkAndUpdateUI()
                } catch (e: Exception) {
                    Log.e("CommunityFragment", "Error parsing alternative Reddit response: ${e.message}", e)
                    redditPostsLoaded = true
                    checkAndUpdateUI()
                }
            },
            { error ->
                Log.e("CommunityFragment", "Error loading alternative Reddit posts: ${error.message}", error)
                redditPostsLoaded = true
                checkAndUpdateUI()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "VytalApp/1.0"
                return headers
            }
        }

        queue.add(request)
    }
    
    private fun checkAndUpdateUI() {
        if (redditPostsLoaded && firestorePostsLoaded) {
            progressBar.visibility = View.GONE
            if (postsList.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = "No posts available. Join a community to get started!"
            } else {
                tvEmptyState.visibility = View.GONE
            }
        }
    }
}
