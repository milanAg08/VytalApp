package com.example.vytal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var logoutBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        logoutBtn = findViewById(R.id.btnLogout)

        // Load the FIRST SCREEN (CommunityFragment with NO groupId)
        val firstFragment = CommunityFragment()
        firstFragment.arguments = Bundle().apply {
            putString("groupId", "")   // IMPORTANT → empty groupId
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.home_container, firstFragment)
            .commit()

        // LOGOUT BUTTON ACTION
        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // LISTEN FOR FRAGMENT CHANGES to show/hide logout
        supportFragmentManager.addOnBackStackChangedListener {
            updateLogoutVisibility()
        }
    }

    override fun onResume() {
        super.onResume()
        updateLogoutVisibility()
    }

    private fun updateLogoutVisibility() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.home_container)

        // SHOW logout ONLY when CommunityFragment has EMPTY groupId
        if (currentFragment is CommunityFragment &&
            currentFragment.arguments?.getString("groupId").isNullOrEmpty()
        ) {
            logoutBtn.visibility = Button.VISIBLE
        } else {
            logoutBtn.visibility = Button.GONE
        }
    }
}
