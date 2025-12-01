package com.example.vytal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [GroupDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
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

        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(true)
            allowFileAccess = false
            allowContentAccess = false
        }

        // Set WebViewClient to handle errors
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    // Show error message
                    webView.loadDataWithBaseURL(
                        null,
                        """
                        <html>
                        <body style="font-family: Arial; padding: 20px; text-align: center;">
                            <h2>Unable to Load Content</h2>
                            <p>Please check your internet connection and try again.</p>
                            <p style="color: #666; font-size: 12px;">Error: ${error?.description ?: "Network error"}</p>
                        </body>
                        </html>
                        """.trimIndent(),
                        "text/html",
                        "UTF-8",
                        null
                    )
                    Toast.makeText(
                        requireContext(),
                        "Unable to load webpage. Please check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Hide loading indicator if needed
            }
        }

        try {
            webView.loadUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Error loading content: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
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
}
