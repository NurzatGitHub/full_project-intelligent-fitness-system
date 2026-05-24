package com.example.fitnesscoachai.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.MainActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {

    private lateinit var btnStartTraining: MaterialButton
    private lateinit var btnCompleteProfileLater: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        startActivity(Intent(this, com.example.fitnesscoachai.ui.auth.AuthActivity::class.java))
        finish()
    }
}
