package com.example.vytal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CommunityGroupsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val groupList = ArrayList<Group>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_community_groups, container, false)

        recyclerView = view.findViewById(R.id.recyclerGroups)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = GroupsAdapter(groupList) { selectedGroup ->
            openCommunity(selectedGroup.id)
        }

        loadGroups()

        return view
    }

    private fun loadGroups() {
        db.collection("community_groups")
            .get()
            .addOnSuccessListener { result ->

                groupList.clear()

                for (doc in result) {

                    val id = doc.id                                // ALWAYS CORRECT ID
                    val name = doc.getString("name") ?: "No name"
                    val desc = doc.getString("description") ?: "No description"

                    groupList.add(Group(id, name, desc))
                }

                recyclerView.adapter?.notifyDataSetChanged()
            }
    }

    private fun openCommunity(groupId: String) {

        if (groupId.isEmpty()) {
            println("❌ ERROR: groupId EMPTY, cannot open")
            return
        }

        val fragment = CommunityFragment()
        fragment.arguments = Bundle().apply {
            putString("groupId", groupId)
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.home_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
