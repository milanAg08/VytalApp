package com.example.vytal

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreatePostDialog : DialogFragment() {

    private var pickedImageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private var selectedPreview: ImageView? = null

    companion object {
        fun newInstance(groupId: String): CreatePostDialog {
            val dialog = CreatePostDialog()
            dialog.arguments = Bundle().apply {
                putString("groupId", groupId)
            }
            return dialog
        }
    }

    // RESTORE URI AFTER RECREATION
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("savedImageUri", pickedImageUri?.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = savedInstanceState?.getString("savedImageUri")
        if (saved != null) {
            pickedImageUri = Uri.parse(saved)
            Log.d("RESTORE", "Restored pickedImageUri = $pickedImageUri")
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            Log.d("DEBUG_PICK", "Picked = $uri")
            if (uri != null) {
                pickedImageUri = uri
                selectedPreview?.visibility = android.view.View.VISIBLE
                selectedPreview?.setImageURI(uri)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_create_post)

        val groupId = arguments?.getString("groupId") ?: ""

        val postInput = dialog.findViewById<EditText>(R.id.postInput)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmitPost)
        val pickImageBtn = dialog.findViewById<Button>(R.id.btnPickImage)

        selectedPreview = dialog.findViewById(R.id.selectedImagePreview)

        // SHOW RESTORED PREVIEW
        pickedImageUri?.let {
            selectedPreview?.visibility = android.view.View.VISIBLE
            selectedPreview?.setImageURI(it)
        }

        pickImageBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSubmit.setOnClickListener {
            Log.d("DEBUG_POST", "Submit clicked. pickedImageUri = $pickedImageUri")

            val text = postInput.text.toString().trim()

            if (text.isEmpty() && pickedImageUri == null) {
                Toast.makeText(requireContext(), "Write something or choose an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pickedImageUri != null) {
                uploadImageAndPost(groupId, text)
            } else {
                createPost(groupId, text, "")
            }
        }

        return dialog
    }

    private fun uploadImageAndPost(groupId: String, text: String) {

        Log.d("DEBUG_UPLOAD", "Uploading pickedImageUri = $pickedImageUri")

        val fileRef = FirebaseStorage.getInstance()
            .reference
            .child("posts/${System.currentTimeMillis()}.jpg")

        val uri = pickedImageUri ?: return createPost(groupId, text, "")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { url ->
                    Log.d("DEBUG_UPLOAD", "Firebase URL = $url")
                    createPost(groupId, text, url.toString())
                }
            }
            .addOnFailureListener {
                createPost(groupId, text, "")
            }
    }

    private fun createPost(groupId: String, text: String, imageUrl: String) {

        Log.d("CREATE_POST", "Saving imageUrl = $imageUrl")

        val uid = FirebaseAuth.getInstance().uid ?: return

        db.collection("users").document(uid).get()
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
