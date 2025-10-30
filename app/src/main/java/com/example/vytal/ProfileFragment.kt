package com.example.vytal

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.vytal.data.HealthRecord
import com.example.vytal.data.Profile
import com.example.vytal.data.ProfileViewModel
import com.example.vytal.data.HealthViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var imageProfile: ShapeableImageView
    private lateinit var editName: TextInputEditText
    private lateinit var editAge: TextInputEditText
    private lateinit var editEmail: TextInputEditText
    private lateinit var buttonSave: Button
    private lateinit var buttonPickImage: Button
    private lateinit var buttonRemoveImage: Button
    private lateinit var buttonAddRecord: Button

    private var imageUri: Uri? = null

    // ViewModels
    private val profileViewModel: ProfileViewModel by viewModels()
    private val healthViewModel: HealthViewModel by viewModels()

    // Image picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            imageUri = data?.data
            imageProfile.setImageURI(imageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize views
        imageProfile = view.findViewById(R.id.imageProfile)
        editName = view.findViewById(R.id.editName)
        editAge = view.findViewById(R.id.editAge)
        editEmail = view.findViewById(R.id.editEmail)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonPickImage = view.findViewById(R.id.buttonPickImage)
        buttonRemoveImage = view.findViewById(R.id.buttonRemoveImage)
        buttonAddRecord = view.findViewById(R.id.btnAddRecord)

        // Load saved profile
        CoroutineScope(Dispatchers.Main).launch {
            val profile = withContext(Dispatchers.IO) { profileViewModel.getProfile() }
            profile?.let {
                editName.setText(it.name)
                editAge.setText(it.age.toString())
                editEmail.setText(it.email)
                it.imageUri?.let { uri -> imageProfile.setImageURI(Uri.parse(uri)) }
                imageUri = it.imageUri?.let { uri -> Uri.parse(uri) }
            }
        }

        // Pick image
        buttonPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        // Remove image
        buttonRemoveImage.setOnClickListener {
            imageProfile.setImageResource(android.R.drawable.ic_menu_camera)
            imageUri = null
        }

        // Save profile
        buttonSave.setOnClickListener {
            val name = editName.text.toString()
            val age = editAge.text.toString().toIntOrNull() ?: 0
            val email = editEmail.text.toString()

            if (name.isBlank() || email.isBlank()) {
                Toast.makeText(requireContext(), "Please fill all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profile = Profile(
                name = name,
                age = age,
                email = email,
                imageUri = imageUri?.toString()
            )

            profileViewModel.saveProfile(profile)
            Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show()
        }

        // Add record
        buttonAddRecord.setOnClickListener {
            showAddRecordDialog()
        }

        return view
    }

    private fun showAddRecordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_record, null)
        val editSystolic = dialogView.findViewById<EditText>(R.id.editSystolic)
        val editDiastolic = dialogView.findViewById<EditText>(R.id.editDiastolic)
        val editSugarLevel = dialogView.findViewById<EditText>(R.id.editSugarLevel)
        val editWeight = dialogView.findViewById<EditText>(R.id.editWeight)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Health Record")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val systolic = editSystolic.text.toString().toIntOrNull() ?: 0
                val diastolic = editDiastolic.text.toString().toIntOrNull() ?: 0
                val sugarLevel = editSugarLevel.text.toString().toFloatOrNull() ?: 0f
                val weight = editWeight.text.toString().toFloatOrNull() ?: 0f

                val record = HealthRecord(
                    systolic = systolic,
                    diastolic = diastolic,
                    sugarLevel = sugarLevel,
                    weight = weight
                )

                healthViewModel.insertRecord(record)

                Toast.makeText(requireContext(), "Record added!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
