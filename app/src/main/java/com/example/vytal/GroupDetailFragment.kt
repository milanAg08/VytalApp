package com.example.vytal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

private lateinit var postsRecycler: RecyclerView
private lateinit var postsAdapter: PostsAdapter
private val postsList = ArrayList<Post>()

class GroupDetailFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var commentsRecycler: RecyclerView
    private lateinit var adapter: CommentsAdapter
    private val commentsList = ArrayList<Comment>()

    private val db = FirebaseFirestore.getInstance()
    private var groupId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_group_detail, container, false)

        groupId = arguments?.getString("groupId") ?: ""

        webView = view.findViewById(R.id.groupWebView)
        commentsRecycler = view.findViewById(R.id.commentsRecycler)

        commentsRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CommentsAdapter(this.commentsList)
        commentsRecycler.adapter = adapter

        val commentInput = view.findViewById<EditText>(R.id.commentInput)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitComment)

        // Load health article
        loadWebArticle(groupId)

        // Load existing comments
        loadComments()

        // ----------- POSTS RECYCLER SETUP -------------
        postsRecycler = view.findViewById(R.id.postsRecycler)
        postsRecycler.layoutManager = LinearLayoutManager(requireContext())
        postsAdapter = PostsAdapter(postsList, groupId)
        postsRecycler.adapter = postsAdapter

        // Load posts
        loadPosts()

        // Create post
        view.findViewById<View>(R.id.fabCreatePost).setOnClickListener {
            openCreatePostDialog()
        }

        btnSubmit.setOnClickListener {
            val text = commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                saveComment(text)
                commentInput.text.clear()
            }
        }

        return view
    }

    private fun loadWebArticle(groupId: String) {
        val url = when (groupId) {
            "diabetes_support" -> "https://www.healthline.com/diabetes"
            "fitness_yoga" -> "https://www.healthline.com/health/fitness-exercise"
            "heart_health_hub" -> "https://www.healthline.com/health/heart-disease"
            "mental_wellness" -> "https://www.healthline.com/health/mental-health"
            else -> "https://www.healthline.com/"
        }

        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
    }

    private fun saveComment(text: String) {
        val commentData = hashMapOf(
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("community_groups")
            .document(groupId)
            .collection("comments")
            .add(commentData)
    }

    private fun loadComments() {
        db.collection("community_groups")
            .document(groupId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, _ ->

                commentsList.clear()
                for (doc in snapshots!!) {
                    val text = doc.getString("text") ?: ""
                    commentsList.add(Comment(text))
                }

                adapter.notifyDataSetChanged()
            }
    }

    // ----------------- FIXED: SEPARATE FUNCTION -----------------
    private fun loadPosts() {
        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->

                postsList.clear()
                for (doc in snapshot!!) {
                    val post = doc.toObject(Post::class.java)
                    post.postId = doc.id
                    postsList.add(post)
                }
                postsAdapter.notifyDataSetChanged()
            }
    }

    private fun openCreatePostDialog() {
        CreatePostDialog.newInstance(groupId)
            .show(parentFragmentManager, "createPost")
    }
}