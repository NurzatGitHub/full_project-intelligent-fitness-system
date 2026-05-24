package com.example.fitnesscoachai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fitnesscoachai.ui.assistant.AssistantFragment
import com.example.fitnesscoachai.ui.auth.AuthActivity
import com.example.fitnesscoachai.ui.exercise.ExerciseSelectFragment
import com.example.fitnesscoachai.ui.home.HomeFragment
import com.example.fitnesscoachai.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private companion object {
        const val TAG_HOME = "home"
        const val TAG_EXERCISE = "exercise"
        const val TAG_ASSISTANT = "assistant"
        const val TAG_PROFILE = "profile"
    }

    private lateinit var homeFragment: Fragment
    private lateinit var exerciseFragment: Fragment
    private lateinit var assistantFragment: Fragment
    private lateinit var profileFragment: Fragment

    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLoggedIn = getSharedPreferences("auth", MODE_PRIVATE)
            .getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            // Fresh start: create fragments and attach all of them at once.
            homeFragment = HomeFragment()
            exerciseFragment = ExerciseSelectFragment()
            assistantFragment = AssistantFragment()
            profileFragment = ProfileFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.container, profileFragment, TAG_PROFILE)
                .hide(profileFragment)
                .add(R.id.container, assistantFragment, TAG_ASSISTANT)
                .hide(assistantFragment)
                .add(R.id.container, exerciseFragment, TAG_EXERCISE)
                .hide(exerciseFragment)
                .add(R.id.container, homeFragment, TAG_HOME)
                .commitNow()

            activeFragment = homeFragment
            bottomNavigation.selectedItemId = R.id.nav_home
        } else {
            // After config change / theme switch: reuse fragments from
            // the FragmentManager instead of creating fresh instances.
            homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) ?: HomeFragment()
            exerciseFragment = supportFragmentManager.findFragmentByTag(TAG_EXERCISE) ?: ExerciseSelectFragment()
            assistantFragment = supportFragmentManager.findFragmentByTag(TAG_ASSISTANT) ?: AssistantFragment()
            profileFragment = supportFragmentManager.findFragmentByTag(TAG_PROFILE) ?: ProfileFragment()

            activeFragment = listOf(homeFragment, exerciseFragment, assistantFragment, profileFragment)
                .firstOrNull { it.isAdded && !it.isHidden }
                ?: homeFragment
        }

        bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> switchTo(homeFragment)
                R.id.nav_camera -> switchTo(exerciseFragment)
                R.id.nav_assistant -> switchTo(assistantFragment)
                R.id.nav_profile -> switchTo(profileFragment)
            }
            true
        }
    }

    private fun switchTo(target: Fragment) {
        if (!::activeFragment.isInitialized) {
            activeFragment = target
            return
        }
        if (target === activeFragment) return

        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()

        activeFragment = target
    }
}
