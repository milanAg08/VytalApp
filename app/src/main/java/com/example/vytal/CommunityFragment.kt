package com.example.vytal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommunityFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val groupId = "YOUR_GROUP_ID"   // Change later

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_community, container, false)

        // JOIN BUTTON
        val btnJoin = view.findViewById<Button>(R.id.btnJoinCommunities)
        btnJoin.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.home_container, CommunityGroupsFragment())
                .addToBackStack(null)
                .commit()
        }

        // POSTS RECYCLER VIEW
        val recycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val posts = ArrayList<Post>()
        val adapter = PostsAdapter(posts, groupId)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // LIVE LOAD POSTS FROM FIRESTORE
        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (value != null) {
                    posts.clear()
                    for (doc in value.documents) {
                        val post = doc.toObject(Post::class.java)
                        if (post != null) {
                            post.postId = doc.id
                            posts.add(post)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }

        // ⭐ FIXED FAB ID HERE (NO MORE CRASH)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabCreatePost)
        fab.setOnClickListener {
            CreatePostDialog.newInstance(groupId)
                .show(parentFragmentManager, "createPost")
        }

        return view
    }
}

