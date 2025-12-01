package com.example.vytal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vytal.ui.HomeFragment
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up welcome text and logout button
        val welcomeText = findViewById<TextView>(R.id.tvWelcome)
        val logoutBtn = findViewById<Button>(R.id.btnLogout)

        // Get current user email
        val user = auth.currentUser
        welcomeText.text = "Welcome to Vytal, ${user?.email ?: "Guest"}!"

        // Set up logout button
        logoutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Load HomeFragment only once
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.home_container, HomeFragment())
                .commit()
        }
    }
}
