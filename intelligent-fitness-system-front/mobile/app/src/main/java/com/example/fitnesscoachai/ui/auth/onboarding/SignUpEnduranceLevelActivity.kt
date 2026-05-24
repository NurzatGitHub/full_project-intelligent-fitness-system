package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator

class SignUpEnduranceLevelActivity : AppCompatActivity() {

    private lateinit var optionLow: MaterialCardView
    private lateinit var optionMedium: MaterialCardView
    private lateinit var optionHigh: MaterialCardView
    private lateinit var tvTitleLow: TextView
    private lateinit var tvTitleMedium: TextView
    private lateinit var tvTitleHigh: TextView
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton

    private var selectedEnduranceLevel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_endurance_level)

        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(58, true)

        optionLow = findViewById(R.id.optionLow)
        optionMedium = findViewById(R.id.optionMedium)
        optionHigh = findViewById(R.id.optionHigh)

        tvTitleLow = findViewById(R.id.tvTitleLow)
        tvTitleMedium = findViewById(R.id.tvTitleMedium)
        tvTitleHigh = findViewById(R.id.tvTitleHigh)

        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        val email = intent.getStringExtra("email").orEmpty()
        val password = intent.getStringExtra("password").orEmpty()
        val age = intent.getIntExtra("age", -1)
        val height = intent.getIntExtra("height_cm", -1)
        val weight = intent.getFloatExtra("weight_kg", -1f)
        val fitnessLevel = intent.getStringExtra("fitness_level").orEmpty()
        val gender = intent.getStringExtra("gender").orEmpty()
        val fromGoogle = intent.getBooleanExtra("from_google", false)

        btnNext.isEnabled = false
        btnBack.setOnClickListener { finish() }

        optionLow.setOnClickListener { selectOption("low") }
        optionMedium.setOnClickListener { selectOption("medium") }
        optionHigh.setOnClickListener { selectOption("high") }

        btnNext.setOnClickListener {
            val enduranceLevel = selectedEnduranceLevel ?: return@setOnClickListener

            val i = Intent(this, SignUpGoalActivity::class.java).apply {
                putExtra("email", email)
                putExtra("password", password)
                putExtra("age", age)
                putExtra("height_cm", height)
                putExtra("weight_kg", weight)
                putExtra("fitness_level", fitnessLevel)
                putExtra("endurance_level", enduranceLevel)
                putExtra("gender", gender)
                putExtra("from_google", fromGoogle)
            }
            startActivity(i)
        }
    }

    private fun selectOption(level: String) {
        selectedEnduranceLevel = level
        btnNext.isEnabled = true

        resetOption(optionLow, tvTitleLow)
        resetOption(optionMedium, tvTitleMedium)
        resetOption(optionHigh, tvTitleHigh)

        when (level) {
            "low" -> highlightOption(optionLow, tvTitleLow)
            "medium" -> highlightOption(optionMedium, tvTitleMedium)
            "high" -> highlightOption(optionHigh, tvTitleHigh)
        }
    }

    private fun resetOption(card: MaterialCardView, title: TextView) {
        card.strokeWidth = 0
        card.strokeColor = Color.TRANSPARENT
        title.setTextColor(Color.WHITE)
    }

    private fun highlightOption(card: MaterialCardView, title: TextView) {
        card.strokeWidth = 3
        val accent = ContextCompat.getColor(this, R.color.brand_primary_300)
        card.strokeColor = accent
        title.setTextColor(accent)
    }
}