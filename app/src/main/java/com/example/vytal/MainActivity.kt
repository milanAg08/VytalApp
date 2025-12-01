package com.example.vytal

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import com.example.vytal.ui.HomeFragment
import com.example.vytal.ui.CommunityFragment
import com.example.vytal.ui.EventFragment
import com.example.vytal.ui.MySpaceFragment
import com.example.vytal.ui.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var isBottomNavVisible = true

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the layout resource to use for this activity
        setContentView(R.layout.activity_main)

        // Initialize notification channel and schedule reminders
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.scheduleDailyReminders(this)

        // Find the BottomNavigationView by ID
        bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // Set default fragment when activity launches
        if (savedInstanceState == null) {
            // Replace the fragment container with HomeFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        // Handle bottom navigation item selection
        bottomNav.setOnItemSelectedListener { menuItem ->
            val selectedFragment: Fragment = when (menuItem.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_community -> CommunityFragment()
                R.id.nav_events -> EventFragment()
                R.id.nav_myspace -> MySpaceFragment() // placeholder; create EventsFragment if needed
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()
            // Show bottom nav when switching tabs
            showBottomNavigation()
            true
        }
    }

    fun showBottomNavigation() {
        if (::bottomNav.isInitialized && !isBottomNavVisible) {
            isBottomNavVisible = true
            bottomNav.visibility = View.VISIBLE
            bottomNav.animate()
                .translationY(0f)
                .setDuration(300)
                .start()
        }
    }

    fun hideBottomNavigation() {
        if (::bottomNav.isInitialized && isBottomNavVisible) {
            isBottomNavVisible = false
            // Use measured height or a fixed value if not measured yet
            val height = if (bottomNav.height > 0) bottomNav.height.toFloat() else 80f
            bottomNav.animate()
                .translationY(height)
                .setDuration(300)
                .start()
        }
    }
}
