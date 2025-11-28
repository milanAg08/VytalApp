package com.example.vytal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth   // keep this only here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Already logged in → go Home
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // UI Elements
        val emailField = findViewById<EditText>(R.id.etName)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val signupText = findViewById<TextView>(R.id.tvSignup)
        val forgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // LOGIN
        loginBtn.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)   // 🔥 Works now
            }
        }

        // SIGNUP
        signupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        // FORGOT PASSWORD
        forgotPassword.setOnClickListener {
            val email = emailField.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Password reset mail sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        this,
                        "Failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

        // LOGIN FUNCTION MUST BE OUTSIDE onCreate()
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
