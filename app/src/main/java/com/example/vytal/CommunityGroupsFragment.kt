package com.example.vytal

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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommunityGroupsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var btnLoadSamples: MaterialButton
    private val groupList = ArrayList<Group>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_community_groups, container, false)

        recyclerView = view.findViewById(R.id.recyclerGroups)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnLoadSamples = view.findViewById(R.id.btnLoadSamples)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val currentUserId = auth.currentUser?.uid ?: ""
        adapter = GroupsAdapter(groupList, currentUserId)
        recyclerView.adapter = adapter

        // Load samples button
        btnLoadSamples.setOnClickListener {
            loadSampleGroups()
        }

        loadGroupsFromFirestore()

        return view
    }

    private fun loadGroupsFromFirestore() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        
        db.collection("community_groups")
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                groupList.clear()

                for (doc in result) {
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    val desc = doc.getString("description") ?: ""
                    val members = doc.get("members") as? List<*> ?: emptyList<Any>()
                    val memberCount = members.size
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val category = doc.getString("category") ?: ""

                    groupList.add(Group(id, name, desc, memberCount, imageUrl, category))
                }

                adapter.notifyDataSetChanged()
                
                if (groupList.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "No communities available. Tap 'LOAD SAMPLES' to add sample communities."
                } else {
                    tvEmptyState.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = "Error loading communities: ${e.message}"
                Toast.makeText(requireContext(), "Failed to load communities", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadSampleGroups() {
        progressBar.visibility = View.VISIBLE
        
        val sampleGroups = listOf(
            hashMapOf(
                "name" to "Men's Health Forum",
                "description" to "Discuss men's health, fitness, and wellness in a supportive environment.",
                "members" to emptyList<String>(),
                "category" to "men_health"
            ),
            hashMapOf(
                "name" to "Fitness & Yoga Community",
                "description" to "Join fellow fitness enthusiasts! Share workout routines, yoga poses, and wellness tips.",
                "members" to emptyList<String>(),
                "category" to "fitness"
            ),
            hashMapOf(
                "name" to "Heart Health Hub",
                "description" to "Connect with others focused on cardiovascular wellness. Discuss exercise, diet, and heart-healthy habits.",
                "members" to emptyList<String>(),
                "category" to "heart_health"
            ),
            hashMapOf(
                "name" to "Diabetes Support Group",
                "description" to "A supportive community for people managing diabetes. Share tips, recipes, and encouragement!",
                "members" to emptyList<String>(),
                "category" to "diabetes"
            ),
            hashMapOf(
                "name" to "Mental Wellness Circle",
                "description" to "A safe space to discuss mental health, mindfulness, and emotional well-being.",
                "members" to emptyList<String>(),
                "category" to "mental_health"
            )
        )
        
        var addedCount = 0
        sampleGroups.forEach { groupData ->
            db.collection("community_groups")
                .add(groupData)
                .addOnSuccessListener {
                    addedCount++
                    if (addedCount == sampleGroups.size) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Sample communities loaded!", Toast.LENGTH_SHORT).show()
                        loadGroupsFromFirestore()
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error loading samples: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
