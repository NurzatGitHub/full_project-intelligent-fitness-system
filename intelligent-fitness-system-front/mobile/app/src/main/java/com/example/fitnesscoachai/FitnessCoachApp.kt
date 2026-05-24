package com.example.fitnesscoachai

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.fitnesscoachai.data.api.RetrofitClient

/**
 * Custom Application that applies the user's persisted theme preference
 * before any Activity is created, so cold starts render in the right mode.
 * Also initializes Retrofit (which needs a Context for the encrypted token
 * store + token-refresh authenticator).
 */
class FitnessCoachApp : Application() {

    override fun onCreate() {
        super.onCreate()
        applyPersistedNightMode()
        RetrofitClient.init(this)
    }

    private fun applyPersistedNightMode() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val mode = prefs.getString("theme_mode", "system") ?: "system"
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
