package com.example.vytal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.R
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.vytal.CommunityGroupsFragment
import org.json.JSONObject


class CommunityFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val apiUrl = "https://jsonplaceholder.typicode.com/posts"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewCommunity)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Setup Join Communities button
        val btnJoin = view.findViewById<Button>(R.id.btnJoinCommunities)
        btnJoin.setOnClickListener {
            try {
                val activity = requireActivity()
                // Determine which container to use based on activity type
                val containerId = when (activity::class.java.simpleName) {
                    "MainActivity" -> R.id.fragment_container
                    "HomeActivity" -> R.id.home_container
                    else -> {
                        // Try to find which container exists
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
        
        fetchCommunityPosts()
        return view
    }

    private fun fetchCommunityPosts() {
        val url = "https://www.reddit.com/r/fitness/top.json?limit=10&t=week"

        val queue = Volley.newRequestQueue(requireContext())
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val posts = mutableListOf<CommunityPost>()
                val children = response.getJSONObject("data").getJSONArray("children")

                for (i in 0 until children.length()) {
                    val data = children.getJSONObject(i).getJSONObject("data")
                    val title = data.getString("title")
                    val author = data.optString("author", "Reddit User")
                    val postUrl = "https://www.reddit.com" + data.optString("permalink")
                    val upvotes = data.optInt("ups", 0)

                    posts.add(CommunityPost(title, author, postUrl, upvotes))
                }

                recyclerView.adapter = CommunityAdapter(posts)
            },
            { error ->
                error.printStackTrace()
            }
        )

        queue.add(request)
    }

}
