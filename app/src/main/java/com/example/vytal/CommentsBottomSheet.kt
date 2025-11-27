package com.example.vytal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentsBottomSheet : BottomSheetDialogFragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var commentsRecycler: RecyclerView
    private lateinit var adapter: CommentsAdapter
    private val commentsList = ArrayList<Comment>()

    private lateinit var groupId: String
    private lateinit var postId: String

    companion object {
        fun newInstance(groupId: String, postId: String): CommentsBottomSheet {
            val sheet = CommentsBottomSheet()
            val args = Bundle()
            args.putString("groupId", groupId)
            args.putString("postId", postId)
            sheet.arguments = args
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottomsheet_comments, container, false)

        groupId = arguments?.getString("groupId") ?: ""
        postId = arguments?.getString("postId") ?: ""

        commentsRecycler = view.findViewById(R.id.commentsRecycler)
        commentsRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CommentsAdapter(commentsList)
        commentsRecycler.adapter = adapter

        val commentInput = view.findViewById<EditText>(R.id.commentInput)
        val btnSend = view.findViewById<Button>(R.id.btnSend)

        // Load old comments
        loadComments()

        // Add new comment
        btnSend.setOnClickListener {
            val text = commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val data = hashMapOf(
                    "userId" to FirebaseAuth.getInstance().uid,
                    "text" to text,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("community_groups")
                    .document(groupId)
                    .collection("posts")
                    .document(postId)
                    .collection("comments")
                    .add(data)

                commentInput.text.clear()
            }
        }

        return view
    }

    private fun loadComments() {
        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                commentsList.clear()
                for (doc in snapshot!!) {
                    val text = doc.getString("text") ?: ""
                    commentsList.add(Comment(text))
                }
                adapter.notifyDataSetChanged()
            }
    }
}