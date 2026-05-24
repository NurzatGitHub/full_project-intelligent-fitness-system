package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignUpLimitationsActivity : AppCompatActivity() {

    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton

    private lateinit var chipNone: Chip
    private lateinit var chipBackPain: Chip
    private lateinit var chipKneePain: Chip
    private lateinit var chipShoulderPain: Chip
    private lateinit var chipWristPain: Chip
    private lateinit var chipOther: Chip

    private lateinit var tilOther: TextInputLayout
    private lateinit var etOther: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_limitations)

        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        // after adding workout place, this step shifted
        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(83, true)

        chipNone = findViewById(R.id.chipNone)
        chipBackPain = findViewById(R.id.chipBackPain)
        chipKneePain = findViewById(R.id.chipKneePain)
        chipShoulderPain = findViewById(R.id.chipShoulderPain)
        chipWristPain = findViewById(R.id.chipWristPain)
        chipOther = findViewById(R.id.chipOther)

        tilOther = findViewById(R.id.tilOther)
        etOther = findViewById(R.id.etOther)

        chipNone.isChecked = true
        syncNoneRule()

        chipNone.setOnCheckedChangeListener { _, _ -> syncNoneRule() }

        val otherListener = { _: View ->
            tilOther.visibility = if (chipOther.isChecked) View.VISIBLE else View.GONE
            if (!chipOther.isChecked) {
                tilOther.error = null
                etOther.setText("")
            }
        }
        chipOther.setOnClickListener(otherListener)

        val painChips = listOf(chipBackPain, chipKneePain, chipShoulderPain, chipWristPain, chipOther)
        painChips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) chipNone.isChecked = false
                ensureAtLeastOneSelected()
                if (chip == chipOther) otherListener(chip)
            }
        }

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            val limitations = buildLimitationsString() ?: return@setOnClickListener

            val email = intent.getStringExtra("email")
            val password = intent.getStringExtra("password")
            val age = intent.getIntExtra("age", -1)
            val height = intent.getIntExtra("height_cm", -1)
            val weight = intent.getFloatExtra("weight_kg", -1f)
            val fitnessLevel = intent.getStringExtra("fitness_level")
            val fromGoogle = intent.getBooleanExtra("from_google", false)
            val enduranceLevel = intent.getStringExtra("endurance_level")
            val gender = intent.getStringExtra("gender")
            val goal = intent.getStringExtra("goal")
            val workoutPlace = intent.getStringExtra("workout_place")

            val next = Intent(this, SignUpFrequencyActivity::class.java).apply {
                putExtra("limitations", limitations)
                putExtra("from_google", fromGoogle)

                if (email != null) putExtra("email", email)
                if (password != null) putExtra("password", password)
                if (age != -1) putExtra("age", age)
                if (height != -1) putExtra("height_cm", height)
                if (weight != -1f) putExtra("weight_kg", weight)
                if (fitnessLevel != null) putExtra("fitness_level", fitnessLevel)
                if (enduranceLevel != null) putExtra("endurance_level", enduranceLevel)
                if (gender != null) putExtra("gender", gender)
                if (goal != null) putExtra("goal", goal)
                if (workoutPlace != null) putExtra("workout_place", workoutPlace)
            }
            startActivity(next)
        }
    }

    private fun syncNoneRule() {
        if (chipNone.isChecked) {
            chipBackPain.isChecked = false
            chipKneePain.isChecked = false
            chipShoulderPain.isChecked = false
            chipWristPain.isChecked = false
            chipOther.isChecked = false

            tilOther.visibility = View.GONE
            tilOther.error = null
            etOther.setText("")
        } else {
            ensureAtLeastOneSelected()
        }
    }

    private fun ensureAtLeastOneSelected() {
        val anyChecked = chipNone.isChecked ||
                chipBackPain.isChecked ||
                chipKneePain.isChecked ||
                chipShoulderPain.isChecked ||
                chipWristPain.isChecked ||
                chipOther.isChecked

        if (!anyChecked) chipNone.isChecked = true
    }

    private fun buildLimitationsString(): String? {
        tilOther.error = null

        if (chipNone.isChecked) return "No issues"

        val list = mutableListOf<String>()
        if (chipBackPain.isChecked) list += "Back pain"
        if (chipKneePain.isChecked) list += "Knee pain"
        if (chipShoulderPain.isChecked) list += "Shoulder pain"
        if (chipWristPain.isChecked) list += "Wrist pain"

        if (chipOther.isChecked) {
            val other = etOther.text?.toString()?.trim().orEmpty()
            if (other.isBlank()) {
                tilOther.error = "Please describe your limitation"
                Toast.makeText(this, "Fill the 'Other' field", Toast.LENGTH_SHORT).show()
                return null
            }
            list += "Other: $other"
        }

        return if (list.isEmpty()) "No issues" else list.joinToString(", ")
    }
}