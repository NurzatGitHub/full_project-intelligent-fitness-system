package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator

class SignUpWorkoutPlaceActivity : AppCompatActivity() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var chipHome: Chip
    private lateinit var chipOutdoor: Chip
    private lateinit var chipGym: Chip
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_workout_place)

        chipGroup = findViewById(R.id.chipGroupWorkoutPlace)
        chipHome = findViewById(R.id.chipHome)
        chipOutdoor = findViewById(R.id.chipOutdoor)
        chipGym = findViewById(R.id.chipGym)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(75, true)
        btnNext.isEnabled = false

        btnBack.setOnClickListener { finish() }
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            btnNext.isEnabled = checkedIds.isNotEmpty()
        }

        btnNext.setOnClickListener {
            val workoutPlace = when (chipGroup.checkedChipId) {
                R.id.chipHome -> "home"
                R.id.chipOutdoor -> "outdoor"
                R.id.chipGym -> "gym"
                else -> return@setOnClickListener
            }

            val email = intent.getStringExtra("email")
            val password = intent.getStringExtra("password")
            val age = intent.getIntExtra("age", -1)
            val height = intent.getIntExtra("height_cm", -1)
            val weight = intent.getFloatExtra("weight_kg", -1f)
            val fitnessLevel = intent.getStringExtra("fitness_level")
            val goal = intent.getStringExtra("goal")
            val enduranceLevel = intent.getStringExtra("endurance_level")
            val gender = intent.getStringExtra("gender")
            val fromGoogle = intent.getBooleanExtra("from_google", false)

            val next = Intent(this, SignUpLimitationsActivity::class.java).apply {
                putExtra("workout_place", workoutPlace)
                putExtra("from_google", fromGoogle)

                if (email != null) putExtra("email", email)
                if (password != null) putExtra("password", password)
                if (age != -1) putExtra("age", age)
                if (height != -1) putExtra("height_cm", height)
                if (weight != -1f) putExtra("weight_kg", weight)
                if (fitnessLevel != null) putExtra("fitness_level", fitnessLevel)
                if (goal != null) putExtra("goal", goal)
                if (enduranceLevel != null) putExtra("endurance_level", enduranceLevel)
                if (gender != null) putExtra("gender", gender)
            }

            startActivity(next)
        }
    }
}