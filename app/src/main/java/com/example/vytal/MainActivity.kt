package com.example.vytal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import com.example.vytal.ui.HomeFragment
import com.example.vytal.ui.CommunityFragment
import com.example.vytal.ui.EventFragment

class MainActivity : AppCompatActivity() {

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the layout resource to use for this activity
        setContentView(R.layout.activity_main)

        // Find the BottomNavigationView by ID
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

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
                R.id.nav_events -> EventFragment() // placeholder; create EventsFragment if needed
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()
            true
        }
    }
}
