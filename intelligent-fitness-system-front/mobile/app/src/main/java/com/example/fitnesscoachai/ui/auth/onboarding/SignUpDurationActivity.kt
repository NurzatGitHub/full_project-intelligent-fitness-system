package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitnesscoachai.MainActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.RegisterRequest
import com.example.fitnesscoachai.data.models.UpdateProfileRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

class SignUpDurationActivity : AppCompatActivity() {

    private lateinit var tvDurationValue: TextView
    private lateinit var tvDurationDescription: TextView
    private lateinit var sliderDuration: Slider
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var loadingIndicator: ProgressBar

    private var selectedDuration: String = "30-60"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_duration)

        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(100, true)

        tvDurationValue = findViewById(R.id.tvDurationValue)
        tvDurationDescription = findViewById(R.id.tvDurationDescription)
        sliderDuration = findViewById(R.id.sliderDuration)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        btnBack.setOnClickListener { finish() }

        val email = intent.getStringExtra("email").orEmpty()
        val password = intent.getStringExtra("password").orEmpty()

        val age = intent.getIntExtra("age", -1).takeIf { it != -1 }
        val heightCm = intent.getIntExtra("height_cm", -1).takeIf { it != -1 }
        val weightKg = intent.getFloatExtra("weight_kg", -1f).takeIf { it != -1f }

        val fitnessLevel = intent.getStringExtra("fitness_level").orEmpty().ifEmpty { "beginner" }
        val goal = intent.getStringExtra("goal").orEmpty()
        val limitations = intent.getStringExtra("limitations").orEmpty()
        val frequency = intent.getStringExtra("frequency").orEmpty()
        val workoutPlace = intent.getStringExtra("workout_place").orEmpty()
        val enduranceLevel = intent.getStringExtra("endurance_level").orEmpty()
        val gender = intent.getStringExtra("gender").orEmpty()
        val fromGoogle = intent.getBooleanExtra("from_google", false)

        fun updateDurationUI(value: Float) {
            when (value.toInt()) {
                0 -> {
                    selectedDuration = "15-30"
                    tvDurationValue.text = "15–30 min"
                    tvDurationDescription.text = "Short and effective workouts"
                }
                1 -> {
                    selectedDuration = "30-60"
                    tvDurationValue.text = "30–60 min"
                    tvDurationDescription.text = "Balanced workouts for steady progress"
                }
                2 -> {
                    selectedDuration = "60+"
                    tvDurationValue.text = "60+ min"
                    tvDurationDescription.text = "Long workouts for maximum intensity"
                }
            }
        }

        sliderDuration.value = 1f
        updateDurationUI(sliderDuration.value)
        btnNext.isEnabled = true

        sliderDuration.addOnChangeListener { _, value, _ ->
            updateDurationUI(value)
        }

        btnNext.setOnClickListener {
            if (fromGoogle) {
                updateProfileForGoogle(
                    age = age,
                    height = heightCm?.toFloat(),
                    weight = weightKg,
                    fitnessLevel = fitnessLevel,
                    goal = goal,
                    limitations = limitations,
                    frequency = frequency,
                    duration = selectedDuration,
                    workoutPlace = workoutPlace,
                    enduranceLevel = enduranceLevel,
                    gender = gender
                )
            } else {
                registerUser(
                    email = email,
                    password = password,
                    age = age,
                    height = heightCm?.toFloat(),
                    weight = weightKg,
                    fitnessLevel = fitnessLevel,
                    goal = goal,
                    limitations = limitations,
                    frequency = frequency,
                    duration = selectedDuration,
                    workoutPlace = workoutPlace,
                    enduranceLevel = enduranceLevel,
                    gender = gender
                )
            }
        }
    }

    private fun updateProfileForGoogle(
        age: Int?,
        height: Float?,
        weight: Float?,
        fitnessLevel: String,
        goal: String,
        limitations: String,
        frequency: String,
        duration: String,
        workoutPlace: String,
        enduranceLevel: String,
        gender: String
    ) {
        setLoading(true)

        // Source of truth: encrypted TokenStore; fall back to legacy pref.
        val token = com.example.fitnesscoachai.data.api.RetrofitClient.tokenStore().accessToken
            ?: getSharedPreferences("auth", MODE_PRIVATE).getString("access_token", null)

        if (token.isNullOrBlank()) {
            Toast.makeText(this, "No access token. Please login again.", Toast.LENGTH_LONG).show()
            setLoading(false)
            return
        }

        val body = UpdateProfileRequest(
            age = age,
            height = height,
            weight = weight,
            fitness_level = fitnessLevel,
            goal = goal,
            limitations = limitations,
            frequency = frequency,
            workout_duration = duration,
            workout_place = workoutPlace,
            endurance_level = enduranceLevel,
            gender = gender
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateMe("Bearer $token", body)

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!

                    getSharedPreferences("auth", MODE_PRIVATE).edit()
                        .putString("user_name", user.username)
                        .putString("user_email", user.email)
                        .apply()

                    startActivity(
                        Intent(this@SignUpDurationActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                } else {
                    val err = response.errorBody()?.string() ?: "Profile update failed"
                    Toast.makeText(this@SignUpDurationActivity, err, Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SignUpDurationActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                setLoading(false)
            }
        }
    }

    private fun registerUser(
        email: String,
        password: String,
        age: Int?,
        height: Float?,
        weight: Float?,
        fitnessLevel: String,
        goal: String,
        limitations: String,
        frequency: String,
        duration: String,
        workoutPlace: String,
        enduranceLevel: String,
        gender: String
    ) {
        setLoading(true)

        val request = RegisterRequest(
            email = email,
            password = password,
            age = age,
            height = height,
            weight = weight,
            fitness_level = fitnessLevel,
            goal = goal,
            limitations = limitations,
            frequency = frequency,
            workout_duration = duration,
            workout_place = workoutPlace,
            endurance_level = enduranceLevel,
            gender = gender
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Tokens → encrypted TokenStore. Other crumbs stay in legacy prefs
                    // since lots of screens already read them.
                    com.example.fitnesscoachai.data.api.RetrofitClient
                        .tokenStore()
                        .saveTokens(access = body.access, refresh = body.refresh)

                    getSharedPreferences("auth", MODE_PRIVATE).edit()
                        .putBoolean("isLoggedIn", true)
                        .putBoolean("isGuest", false)
                        .putString("user_email", body.user.email)
                        .putString("user_name", body.user.username)
                        .putInt("user_id", body.user.id)
                        .remove("access_token")
                        .remove("refresh_token")
                        .apply()

                    startActivity(
                        Intent(this@SignUpDurationActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(this@SignUpDurationActivity, errorMsg, Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SignUpDurationActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        btnNext.isEnabled = !loading
        btnBack.isEnabled = !loading
        sliderDuration.isEnabled = !loading
    }
}