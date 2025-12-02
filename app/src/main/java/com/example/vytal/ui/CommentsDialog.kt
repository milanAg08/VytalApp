package com.example.vytal.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vytal.Comment
import com.example.vytal.CommentsAdapter
import com.example.vytal.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommentsDialog : DialogFragment() {

    private lateinit var commentsRecycler: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var adapter: CommentsAdapter
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val commentsList = ArrayList<Comment>()
    
    private var postId: String = ""
    private var groupId: String = ""

    companion object {
        fun newInstance(postId: String, groupId: String): CommentsDialog {
            val dialog = CommentsDialog()
            dialog.arguments = Bundle().apply {
                putString("postId", postId)
                putString("groupId", groupId)
            }
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        postId = arguments?.getString("postId") ?: ""
        groupId = arguments?.getString("groupId") ?: ""
        
        val view = View.inflate(requireContext(), R.layout.dialog_comments, null)

        commentsRecycler = view.findViewById(R.id.commentsRecycler)
        etComment = view.findViewById(R.id.etComment)
        btnSubmit = view.findViewById(R.id.btnSubmitComment)

        commentsRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CommentsAdapter(commentsList)
        commentsRecycler.adapter = adapter

        loadComments()

        btnSubmit.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                addComment(text)
                etComment.text.clear()
            } else {
                Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Comments")
            .setView(view)
            .setNegativeButton("Close", null)
            .create()
    }

    private fun loadComments() {
        db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading comments: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                commentsList.clear()
                snapshot?.documents?.forEach { doc ->
                    val comment = doc.toObject(Comment::class.java)
                    comment?.let {
                        commentsList.add(it.copy(id = doc.id))
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun addComment(text: String) {
        val currentUser = auth.currentUser ?: return
        val userName = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "Anonymous"

        val comment = hashMapOf(
            "postId" to postId,
            "userId" to currentUser.uid,
            "userName" to userName,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        val commentRef = db.collection("community_groups")
            .document(groupId)
            .collection("posts")
            .document(postId)
            .collection("comments")
            .document()

        commentRef.set(comment)
            .addOnSuccessListener {
                // Update comments count
                db.collection("community_groups")
                    .document(groupId)
                    .collection("posts")
                    .document(postId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val currentCount = doc.getLong("commentsCount")?.toInt() ?: 0
                        db.collection("community_groups")
                            .document(groupId)
                            .collection("posts")
                            .document(postId)
                            .update("commentsCount", currentCount + 1)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to add comment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

