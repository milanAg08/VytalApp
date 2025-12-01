package com.example.vytal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CommunityGroupsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupsAdapter
    private val groupList = ArrayList<Group>()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_community_groups, container, false)

        recyclerView = view.findViewById(R.id.recyclerGroups)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = GroupsAdapter(groupList)
        recyclerView.adapter = adapter

        loadGroupsFromFirestore()

        return view
    }

    private fun loadGroupsFromFirestore() {
        db.collection("community_groups")
            .get()
            .addOnSuccessListener { result ->
                groupList.clear()

                for (doc in result) {
                    val id = doc.id                      // <-- GET DOCUMENT ID
                    val name = doc.getString("name") ?: ""
                    val desc = doc.getString("description") ?: ""

                    groupList.add(Group(id, name, desc)) // <-- SEND ID, NAME, DESC
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Optional: show Toast message later
            }
    }
}
