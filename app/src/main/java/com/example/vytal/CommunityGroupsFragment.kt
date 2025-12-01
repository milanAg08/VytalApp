package com.example.vytal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommunityGroupsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: GroupsAdapter
    private val groupList = ArrayList<Group>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community_groups, container, false)

        recyclerView = view.findViewById(R.id.recyclerGroups)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = GroupsAdapter(groupList) { group ->
            // Handle group click - navigate to group detail
        }
        recyclerView.adapter = adapter

        loadGroupsFromFirestore()

        return view
    }

    private fun loadGroupsFromFirestore() {
        progressBar.visibility = View.VISIBLE
        
        db.collection("community_groups")
            .get()
            .addOnSuccessListener { result ->
                groupList.clear()

                if (result.isEmpty()) {
                    progressBar.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // Get user's joined groups if logged in
                if (currentUserId.isNotEmpty()) {
                    db.collection("users")
                        .document(currentUserId)
                        .collection("joined_groups")
                        .get()
                        .addOnSuccessListener { joinedGroups ->
                            val joinedGroupIds = joinedGroups.documents.map { it.id }.toSet()
                            loadGroupsWithStatus(result, joinedGroupIds)
                        }
                        .addOnFailureListener {
                            loadGroupsWithStatus(result, emptySet())
                        }
                } else {
                    loadGroupsWithStatus(result, emptySet())
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading groups", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadGroupsWithStatus(result: com.google.firebase.firestore.QuerySnapshot, joinedGroupIds: Set<String>) {
        var loadedCount = 0
        val totalCount = result.size()

        if (totalCount == 0) {
            progressBar.visibility = View.GONE
            adapter.notifyDataSetChanged()
            return
        }

        result.forEach { doc ->
            val id = doc.id
            val name = doc.getString("name") ?: ""
            val desc = doc.getString("description") ?: ""
            val isJoined = joinedGroupIds.contains(id)

            // Get member count
            doc.reference.collection("members")
                .get()
                .addOnSuccessListener { members ->
                    val memberCount = members.size()
                    groupList.add(Group(id, name, desc, memberCount, isJoined))
                    loadedCount++

                    if (loadedCount == totalCount) {
                        progressBar.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    }
                }
                .addOnFailureListener {
                    // If member count fails, still add group with 0 count
                    groupList.add(Group(id, name, desc, 0, isJoined))
                    loadedCount++

                    if (loadedCount == totalCount) {
                        progressBar.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }
}
