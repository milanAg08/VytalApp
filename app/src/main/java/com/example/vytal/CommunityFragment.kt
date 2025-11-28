package com.example.vytal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommunityFragment : Fragment() {

    private var groupId: String = ""
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString("groupId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_community, container, false)

        val joinBtn = view.findViewById<Button>(R.id.btnJoinCommunities)
        val recycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabCreatePost)

        // ----------------------------
        // MODE 1 → JOIN SCREEN
        // ----------------------------
        if (groupId.isEmpty()) {

            // SHOW join button
            joinBtn.visibility = View.VISIBLE

            // HIDE post UI
            recycler.visibility = View.GONE
            fab.visibility = View.GONE

            joinBtn.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.home_container, CommunityGroupsFragment())
                    .addToBackStack(null)
                    .commit()
            }

            return view
        }

        // ----------------------------
        // MODE 2 → POSTS SCREEN
        // ----------------------------

        joinBtn.visibility = View.GONE   // hide in post mode
        recycler.visibility = View.VISIBLE
        fab.visibility = View.VISIBLE

        val posts = ArrayList<Post>()
        val adapter = PostsAdapter(posts, groupId)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                posts.clear()
                snap?.forEach { doc ->
                    val p = doc.toObject(Post::class.java)
                    p.postId = doc.id
                    posts.add(p)
                }
                adapter.notifyDataSetChanged()
            }

        fab.setOnClickListener {
            CreatePostDialog.newInstance(groupId)
                .show(parentFragmentManager, "createPost")
        }

        return view
    }
}
