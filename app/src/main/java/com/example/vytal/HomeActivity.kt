package com.example.vytal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vytal.ui.HomeFragment

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Load HomeFragment only once
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.home_container, HomeFragment())
                .commit()
        }
    }
}
