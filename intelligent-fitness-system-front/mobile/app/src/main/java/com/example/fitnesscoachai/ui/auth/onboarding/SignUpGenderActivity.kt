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

class SignUpGenderActivity : AppCompatActivity() {

    private lateinit var optionMale: MaterialCardView
    private lateinit var optionFemale: MaterialCardView
    private lateinit var optionOther: MaterialCardView

    private lateinit var tvMale: TextView
    private lateinit var tvFemale: TextView
    private lateinit var tvOther: TextView

    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton

    private var selectedGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_gender)

        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(42, true)

        optionMale = findViewById(R.id.optionMale)
        optionFemale = findViewById(R.id.optionFemale)
        optionOther = findViewById(R.id.optionOther)

        tvMale = findViewById(R.id.tvMale)
        tvFemale = findViewById(R.id.tvFemale)
        tvOther = findViewById(R.id.tvOther)

        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        val email = intent.getStringExtra("email").orEmpty()
        val password = intent.getStringExtra("password").orEmpty()
        val age = intent.getIntExtra("age", -1)
        val height = intent.getIntExtra("height_cm", -1)
        val weight = intent.getFloatExtra("weight_kg", -1f)
        val fromGoogle = intent.getBooleanExtra("from_google", false)

        btnNext.isEnabled = false

        btnBack.setOnClickListener { finish() }

        optionMale.setOnClickListener { selectOption("male") }
        optionFemale.setOnClickListener { selectOption("female") }
        optionOther.setOnClickListener { selectOption("other") }

        btnNext.setOnClickListener {
            val gender = selectedGender ?: return@setOnClickListener

            val i = Intent(this, SignUpFitnessLevelActivity::class.java).apply {
                putExtra("email", email)
                putExtra("password", password)
                putExtra("age", age)
                putExtra("height_cm", height)
                putExtra("weight_kg", weight)
                putExtra("gender", gender)
                putExtra("from_google", fromGoogle)
            }
            startActivity(i)
        }
    }

    private fun selectOption(gender: String) {
        selectedGender = gender
        btnNext.isEnabled = true

        resetOption(optionMale, tvMale)
        resetOption(optionFemale, tvFemale)
        resetOption(optionOther, tvOther)

        when (gender) {
            "male" -> highlightOption(optionMale, tvMale)
            "female" -> highlightOption(optionFemale, tvFemale)
            "other" -> highlightOption(optionOther, tvOther)
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