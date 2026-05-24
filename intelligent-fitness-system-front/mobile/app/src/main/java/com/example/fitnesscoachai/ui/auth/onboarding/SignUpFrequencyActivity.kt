package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator

class SignUpFrequencyActivity : AppCompatActivity() {

    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var chip2to3: Chip
    private lateinit var chip3to4: Chip
    private lateinit var chip5to6: Chip
    private lateinit var chipEveryday: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_frequency)

        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(92, true)

        chip2to3 = findViewById(R.id.chip2to3)
        chip3to4 = findViewById(R.id.chip3to4)
        chip5to6 = findViewById(R.id.chip5to6)
        chipEveryday = findViewById(R.id.chipEveryday)

        chip3to4.isChecked = true

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            val frequency = when {
                chip2to3.isChecked -> "2-3"
                chip3to4.isChecked -> "3-4"
                chip5to6.isChecked -> "5-6"
                else -> "everyday"
            }

            val fromGoogle = intent.getBooleanExtra("from_google", false)

            val email = intent.getStringExtra("email").orEmpty()
            val password = intent.getStringExtra("password").orEmpty()

            val age = intent.getIntExtra("age", -1)
            val heightCmInt = intent.getIntExtra("height_cm", -1)
            val weightKg = intent.getFloatExtra("weight_kg", -1f)

            val fitnessLevel = intent.getStringExtra("fitness_level").orEmpty().ifEmpty { "beginner" }
            val goal = intent.getStringExtra("goal").orEmpty()
            val limitations = intent.getStringExtra("limitations").orEmpty()
            val workoutPlace = intent.getStringExtra("workout_place").orEmpty()
            val enduranceLevel = intent.getStringExtra("endurance_level").orEmpty()
            val gender = intent.getStringExtra("gender").orEmpty()

            val i = Intent(this, SignUpDurationActivity::class.java).apply {
                putExtra("email", email)
                putExtra("password", password)

                if (age != -1) putExtra("age", age)
                if (heightCmInt != -1) putExtra("height_cm", heightCmInt)
                if (weightKg != -1f) putExtra("weight_kg", weightKg)

                putExtra("fitness_level", fitnessLevel)
                putExtra("goal", goal)
                putExtra("limitations", limitations)
                putExtra("frequency", frequency)
                putExtra("workout_place", workoutPlace)
                putExtra("endurance_level", enduranceLevel)
                putExtra("gender", gender)
                putExtra("from_google", fromGoogle)
            }

            startActivity(i)
        }
    }
}