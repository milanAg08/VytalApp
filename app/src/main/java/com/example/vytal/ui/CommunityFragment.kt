package com.example.vytal.ui

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
import com.example.vytal.CommunityGroupsFragment
import com.example.vytal.Group
import com.example.vytal.GroupsAdapter
import com.example.vytal.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.button.MaterialButton

class CommunityFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: View
    private lateinit var btnJoinNow: MaterialButton
    private lateinit var tvGroupsJoined: TextView
    private lateinit var tvTotalPosts: TextView
    private lateinit var tvMyActivity: TextView
    
    private lateinit var adapter: GroupsAdapter
    private val groupList = ArrayList<Group>()
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community, container, false)
        
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadUserGroups()
        
        return view
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewCommunity)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.layoutEmptyState)
        btnJoinNow = view.findViewById(R.id.btnJoinNow)
        tvGroupsJoined = view.findViewById(R.id.tvGroupsJoined)
        tvTotalPosts = view.findViewById(R.id.tvTotalPosts)
        tvMyActivity = view.findViewById(R.id.tvMyActivity)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = GroupsAdapter(groupList) { group ->
            // Handle group click - navigate to group detail
            navigateToGroupDetail(group)
        }
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        val btnJoinCommunities = view?.findViewById<MaterialButton>(R.id.btnJoinCommunities)
        btnJoinCommunities?.setOnClickListener {
            navigateToGroupsList()
        }
        
        btnJoinNow.setOnClickListener {
            navigateToGroupsList()
        }
    }

    private fun loadUserGroups() {
        if (currentUserId.isEmpty()) {
            showEmptyState()
            return
        }

        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE

        // Load all groups and check which ones user has joined
        db.collection("community_groups")
            .get()
            .addOnSuccessListener { result ->
                groupList.clear()
                
                // Get user's joined groups
                db.collection("users")
                    .document(currentUserId)
                    .collection("joined_groups")
                    .get()
                    .addOnSuccessListener { joinedGroups ->
                        val joinedGroupIds = joinedGroups.documents.map { it.id }.toSet()
                        
                        for (doc in result) {
                            val id = doc.id
                            val name = doc.getString("name") ?: ""
                            val desc = doc.getString("description") ?: ""
                            
                            // Get member count
                            doc.reference.collection("members")
                                .get()
                                .addOnSuccessListener { members ->
                                    val memberCount = members.size()
                                    val isJoined = joinedGroupIds.contains(id)
                                    
                                    groupList.add(Group(id, name, desc, memberCount, isJoined))
                                    
                                    // Update adapter when all groups are processed
                                    if (groupList.size == result.size()) {
                                        updateUI()
                                    }
                                }
                        }
                        
                        if (result.isEmpty()) {
                            showEmptyState()
                        }
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error loading groups", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading groups", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        progressBar.visibility = View.GONE
        
        val joinedCount = groupList.count { it.isJoined }
        tvGroupsJoined.text = joinedCount.toString()
        
        // Update stats (can be enhanced later)
        tvTotalPosts.text = "0"
        tvMyActivity.text = "0"
        
        if (groupList.isEmpty()) {
            showEmptyState()
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }

    private fun navigateToGroupsList() {
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
            Toast.makeText(requireContext(), "Error navigating: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToGroupDetail(group: Group) {
        // This will be handled by GroupsAdapter
    }

    override fun onResume() {
        super.onResume()
        // Refresh groups when fragment resumes
        if (::recyclerView.isInitialized) {
            loadUserGroups()
        }
    }
}

