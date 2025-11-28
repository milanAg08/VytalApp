package com.example.vytal

import android.app.Dialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreatePostDialog : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()
    private var imageUri: Uri? = null

    private var selectedPreview: ImageView? = null
    private var pickImageBtn: ImageView? = null

    companion object {
        fun newInstance(groupId: String): CreatePostDialog {
            val dialog = CreatePostDialog()
            dialog.arguments = Bundle().apply {
                putString("groupId", groupId)
            }
            return dialog
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                selectedPreview?.visibility = View.VISIBLE
                selectedPreview?.setImageURI(uri)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_create_post)

        val groupId = arguments?.getString("groupId") ?: ""

        val postInput = dialog.findViewById<EditText>(R.id.postInput)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmitPost)

        // ⭐ Store preview once (NO NULL)
        selectedPreview = dialog.findViewById(R.id.selectedImagePreview)

        // ⭐ Correct button type
        val pickImageBtn = dialog.findViewById<Button>(R.id.btnPickImage)


        pickImageBtn?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSubmit.setOnClickListener {
            val text = postInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (imageUri != null) uploadImageAndPost(groupId, text)
            else createPost(groupId, text, "")
        }

        return dialog
    }

    private fun uploadImageAndPost(groupId: String, text: String) {
        val fileRef = FirebaseStorage.getInstance()
            .reference
            .child("posts/${System.currentTimeMillis()}.jpg")

        fileRef.putFile(imageUri!!)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { url ->
                    createPost(groupId, text, url.toString())
                }
            }
            .addOnFailureListener {
                createPost(groupId, text, "")
            }
    }

    private fun createPost(groupId: String, text: String, imageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: return

        FirebaseFirestore.getInstance().collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->

                val name = userDoc.getString("name") ?: "Unknown"
                val profilePic = userDoc.getString("profilePic") ?: ""

                val post = hashMapOf(
                    "userId" to uid,
                    "userName" to name,
                    "userProfilePic" to profilePic,
                    "text" to text,
                    "imageUrl" to imageUrl,
                    "timestamp" to System.currentTimeMillis(),
                    "likes" to ArrayList<String>()
                )

                db.collection("community_groups")
                    .document(groupId)
                    .collection("posts")
                    .add(post)

                dismiss()
            }
    }
}
