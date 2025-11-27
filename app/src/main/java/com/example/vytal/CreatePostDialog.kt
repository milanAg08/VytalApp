package com.example.vytal

import android.app.Dialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreatePostDialog : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()
    private var imageUri: Uri? = null

    private fun requestImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                1001
            )
        }
    }

    companion object {
        fun newInstance(groupId: String): CreatePostDialog {
            val dialog = CreatePostDialog()
            val args = Bundle()
            args.putString("groupId", groupId)
            dialog.arguments = args
            return dialog
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                val preview = dialog?.findViewById<ImageView>(R.id.selectedImagePreview)
                preview?.visibility = ImageView.VISIBLE
                preview?.setImageURI(uri)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_create_post)  // MUST MATCH FILE NAME

        val groupId = arguments?.getString("groupId") ?: ""

        val postInput = dialog.findViewById<EditText>(R.id.postInput)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmitPost)
        val pickImageBtn = dialog.findViewById<ImageView>(R.id.btnPickImage)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        pickImageBtn?.setOnClickListener {
            requestImagePermission()
            pickImageLauncher.launch("image/*")
        }

        btnSubmit?.setOnClickListener {
            val text = postInput.text.toString().trim()

            if (text.isNotEmpty()) {
                if (imageUri != null) uploadImageAndPost(groupId, text)
                else createPost(groupId, text, "")
            }
        }

        return dialog
    }

    private fun uploadImageAndPost(groupId: String, text: String) {
        val fileRef = FirebaseStorage.getInstance()
            .reference
            .child("posts/${System.currentTimeMillis()}.jpg")

        fileRef.putFile(imageUri!!)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    createPost(groupId, text, downloadUrl.toString())
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

                val postData = hashMapOf(
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
                    .add(postData)

                dismiss()
            }
    }
}
