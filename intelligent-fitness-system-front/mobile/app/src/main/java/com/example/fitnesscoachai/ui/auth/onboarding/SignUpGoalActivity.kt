package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator

class SignUpGoalActivity : AppCompatActivity() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_goal)

        chipGroup = findViewById(R.id.chipGroupGoal)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(67, true)
        findViewById<Chip>(R.id.chipLoseWeight).isChecked = true

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            val goal = when (chipGroup.checkedChipId) {
                R.id.chipLoseWeight -> "Lose weight"
                R.id.chipBuildMuscle -> "Build muscle"
                R.id.chipGetStronger -> "Get stronger"
                R.id.chipImproveEndurance -> "Improve endurance"
                R.id.chipStayHealthy -> "Stay healthy"
                else -> "Lose weight"
            }

            val email = intent.getStringExtra("email")
            val password = intent.getStringExtra("password")
            val age = intent.getIntExtra("age", -1)
            val height = intent.getIntExtra("height_cm", -1)
            val weight = intent.getFloatExtra("weight_kg", -1f)
            val fitnessLevel = intent.getStringExtra("fitness_level")
            val enduranceLevel = intent.getStringExtra("endurance_level")
            val gender = intent.getStringExtra("gender")
            val fromGoogle = intent.getBooleanExtra("from_google", false)

            val next = Intent(this, SignUpWorkoutPlaceActivity::class.java).apply {
                putExtra("goal", goal)
                putExtra("from_google", fromGoogle)

                if (email != null) putExtra("email", email)
                if (password != null) putExtra("password", password)
                if (age != -1) putExtra("age", age)
                if (height != -1) putExtra("height_cm", height)
                if (weight != -1f) putExtra("weight_kg", weight)
                if (fitnessLevel != null) putExtra("fitness_level", fitnessLevel)
                if (enduranceLevel != null) putExtra("endurance_level", enduranceLevel)
                if (gender != null) putExtra("gender", gender)
            }
            startActivity(next)
        }
    }
}