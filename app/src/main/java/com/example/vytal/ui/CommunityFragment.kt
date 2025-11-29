package com.example.vytal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        
        // Add scroll listener to hide/show bottom navigation
        recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                try {
                    val activity = activity as? com.example.vytal.MainActivity
                    
                    if (!recyclerView.canScrollVertically(-1)) {
                        // At the top - always show bottom nav
                        activity?.showBottomNavigation()
                    } else if (dy > 0 && recyclerView.canScrollVertically(1)) {
                        // Scrolling down
                        activity?.hideBottomNavigation()
                    } else if (dy < 0) {
                        // Scrolling up
                        activity?.showBottomNavigation()
                    }
                } catch (e: Exception) {
                    // Ignore scroll listener errors
                }
            }
        })
        
        // Ensure bottom nav is visible when fragment is first shown
        try {
            (activity as? com.example.vytal.MainActivity)?.showBottomNavigation()
        } catch (e: Exception) {
            // Ignore if activity is not available yet
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
