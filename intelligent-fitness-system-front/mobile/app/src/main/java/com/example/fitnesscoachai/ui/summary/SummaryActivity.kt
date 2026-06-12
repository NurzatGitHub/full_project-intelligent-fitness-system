package com.example.fitnesscoachai.ui.summary

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitnesscoachai.MainActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.WorkoutExerciseRequest
import com.example.fitnesscoachai.data.models.WorkoutSessionRequest
import com.example.fitnesscoachai.ui.history.HistoryActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.example.fitnesscoachai.ui.home.HomeFragment

class SummaryActivity : AppCompatActivity() {

    private lateinit var tvExerciseName: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvTotalReps: TextView
    private lateinit var tvCommonMistakes: TextView
    private lateinit var tvOverallPerformance: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnGoToHistory: MaterialButton
    private lateinit var btnBackToHome: MaterialButton

    private var isSaving = false
    private var exerciseSlug: String? = null
    private var weeklyPlanDayId: Int? = null

    /** -1f means "no AI verdicts captured during this workout"; null on POST. */
    private var formScorePercent: Float = -1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val exerciseName = intent.getStringExtra("exercise_name") ?: "Exercise"
        exerciseSlug = intent.getStringExtra("exercise_slug")

        val rawPlanDayId = intent.getIntExtra("weekly_plan_day_id", -1)
        weeklyPlanDayId = if (rawPlanDayId > 0) rawPlanDayId else null

        val duration = intent.getIntExtra("duration", 0)
        val reps = intent.getIntExtra("reps", 0)
        formScorePercent = intent.getFloatExtra("form_score", -1f)

        initializeViews()
        populateData(exerciseName, duration, reps)
        setupListeners()
    }

    private fun initializeViews() {
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvDuration = findViewById(R.id.tvDuration)
        tvTotalReps = findViewById(R.id.tvTotalReps)
        tvCommonMistakes = findViewById(R.id.tvCommonMistakes)
        tvOverallPerformance = findViewById(R.id.tvOverallPerformance)
        btnSave = findViewById(R.id.btnSave)
        btnGoToHistory = findViewById(R.id.btnGoToHistory)
        btnBackToHome = findViewById(R.id.btnBackToHome)
    }

    private fun populateData(exerciseName: String, duration: Int, reps: Int) {
        tvExerciseName.text = exerciseName
        tvTotalReps.text = reps.toString()

        val minutes = TimeUnit.SECONDS.toMinutes(duration.toLong())
        val seconds = duration % 60
        tvDuration.text = String.format("%02d:%02d", minutes, seconds)

        tvCommonMistakes.text = "Keep your back straight\nMaintain proper form"

        // Overall performance now derives from the real form score when we
        // have one, falling back to a rep-count heuristic when the AI didn't
        // produce any verdicts (e.g. user out of frame the whole time).
        val performance = if (formScorePercent >= 0f) {
            val rounded = formScorePercent.toInt()
            when {
                rounded >= 85 -> "Excellent · $rounded%"
                rounded >= 70 -> "Good · $rounded%"
                rounded >= 50 -> "Average · $rounded%"
                else -> "Needs work · $rounded%"
            }
        } else {
            when {
                reps > 20 -> "Excellent"
                reps > 10 -> "Good"
                reps > 5 -> "Average"
                else -> "Keep practicing"
            }
        }
        tvOverallPerformance.text = performance
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            if (isSaving) return@setOnClickListener
            saveWorkout()
        }

        btnGoToHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }

        btnBackToHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun saveWorkout() {
        isSaving = true
        btnSave.isEnabled = false

        val exerciseName = tvExerciseName.text.toString()
        val durationSec = getDurationInSeconds()
        val reps = tvTotalReps.text.toString().toIntOrNull() ?: 0

        saveWorkoutLocally(exerciseName, durationSec, reps)
        sendWorkoutToBackend(exerciseName, durationSec, reps)
    }

    private fun saveWorkoutLocally(exerciseName: String, durationSec: Int, reps: Int) {
        // User-scoped: switches between history_count_<userId> and
        // history_count_guest under the hood so counters don't leak.
        com.example.fitnesscoachai.data.local.WorkoutHistoryStore.appendSession(
            this, exerciseName, durationSec, reps,
        )
    }

    private fun sendWorkoutToBackend(exerciseName: String, durationSec: Int, reps: Int) {
        // Source of truth: encrypted TokenStore; fall back to legacy pref.
        val token = com.example.fitnesscoachai.data.api.RetrofitClient.tokenStore().accessToken
            ?: getSharedPreferences("auth", MODE_PRIVATE).getString("access_token", null)

        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Workout saved locally", Toast.LENGTH_SHORT).show()
            finishSaving()
            return
        }

        // Only send the form score when the AI actually had something to
        // score — otherwise pass null and let the backend treat the session
        // as "unscored" so it doesn't pollute the avg with 0%.
        val realFormScore: Float? = if (formScorePercent >= 0f) formScorePercent else null

        val request = WorkoutSessionRequest(
            title = exerciseName,
            weekly_plan_day_id = weeklyPlanDayId,
            total_duration_sec = durationSec,
            total_reps = reps,
            avg_form_score = realFormScore,
            exercises = listOf(
                WorkoutExerciseRequest(
                    exercise_slug = exerciseSlug,
                    exercise_name = exerciseName,
                    completed_reps = reps,
                    duration_sec = durationSec,
                    avg_form_score = realFormScore,
                )
            )
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createWorkoutSession(
                    "Bearer $token",
                    request
                )

                if (response.isSuccessful) {
                    HomeFragment.clearCache()
                    Toast.makeText(
                        this@SummaryActivity,
                        "Workout saved",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@SummaryActivity,
                        "Saved locally, backend sync failed: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@SummaryActivity,
                    "Saved locally, backend sync error",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                finishSaving()
            }
        }
    }

    private fun finishSaving() {
        isSaving = false
        btnSave.isEnabled = true
    }

    private fun getDurationInSeconds(): Int {
        val durationText = tvDuration.text.toString()
        val parts = durationText.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts[1].toIntOrNull() ?: 0
            return minutes * 60 + seconds
        }
        return 0
    }
}