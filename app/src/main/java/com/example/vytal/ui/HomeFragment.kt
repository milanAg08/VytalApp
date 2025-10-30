package com.example.vytal.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.R
import com.example.vytal.model.Article
import com.example.vytal.model.ArticlesAdapter
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.ContextCompat
import com.example.vytal.LoginActivity


// Fragment representing the home screen
class HomeFragment : Fragment() {

    // Called to create the fragment's view hierarchy
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment_home layout into a view object
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Reference to the SearchView defined in fragment_home.xml
        val searchView = view.findViewById<SearchView>(R.id.search_view)

        // Set up RecyclerView for featured articles
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_articles)
        recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        // Build a sample list of articles (in production this comes from API or DB)
        val articles = listOf(
            Article("Healthy Eating", "Tips for balanced diet", "https://..."),
            Article("Heart Health", "Recognize symptoms and prevention", "https://..."),
            Article("Yoga for Beginners", "Simple poses to start", "https://...")
        )

        // Adapter expects a List<Article> and sets it on RecyclerView
        val adapter = ArticlesAdapter(requireContext(), articles)
        recycler.adapter = adapter

        val commBtn= view.findViewById<Button>(R.id.btn_community)
        val eventBtn= view.findViewById<Button>(R.id.btn_events)

        commBtn.setOnClickListener {
            Log.d("HomeFragment", "Community button clicked")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CommunityFragment())
                .addToBackStack(null)
                .commit()
        }

        eventBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EventFragment())
                .addToBackStack(null)
                .commit()
        }

        // Example SearchView listener to filter content (simple placeholder)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // handle submit (e.g., open search results page)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Could call adapter.filter(...) if adapter supports it
                return false
            }
        })

        val btnBmiCalculator = view.findViewById<Button>(R.id.btnBmiCalculator)
        btnBmiCalculator.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BmiFragment())
                .addToBackStack(null)
                .commit()
        }

        val btnSymptomChecker = view.findViewById<Button>(R.id.btnSymptomChecker)
        btnSymptomChecker.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SymptomCheckerFragment())
                .addToBackStack(null)
                .commit()
        }

        val auth = FirebaseAuth.getInstance()

        val logoutBtn = view.findViewById<Button>(R.id.btnLogout)
        logoutBtn.setOnClickListener {
            auth.signOut()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()

        }

        // Return the inflated view
        return view
    }
}
