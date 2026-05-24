package com.example.fitnesscoachai.ui.workout.generic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import com.google.android.material.button.MaterialButton
import java.util.concurrent.TimeUnit

class GenericWorkoutActivity : AppCompatActivity() {

    private lateinit var tvExerciseName: TextView
    private lateinit var tvRecommended: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvReps: TextView
    private lateinit var btnMinusReps: ImageButton
    private lateinit var btnPlusReps: ImageButton
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnFinish: MaterialButton
    private var ivExercisePreview: ImageView? = null

    private var exerciseName: String = "Exercise"
    private var exerciseSlug: String? = null
    private var assetImageName: String? = null
    private var weeklyPlanDayId: Int? = null

    private var defaultReps: Int = 0
    private var defaultDurationSec: Int = 0

    private var currentReps: Int = 0
    private var elapsedSeconds: Long = 0
    private var isRunning = false
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generic_workout)

        exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: "Exercise"
        exerciseSlug = intent.getStringExtra(EXTRA_EXERCISE_SLUG)
        assetImageName = intent.getStringExtra(EXTRA_ASSET_IMAGE_NAME)
        defaultReps = intent.getIntExtra(EXTRA_DEFAULT_REPS, 0)
        defaultDurationSec = intent.getIntExtra(EXTRA_DEFAULT_DURATION_SEC, 0)

        val rawPlanDayId = intent.getIntExtra(EXTRA_WEEKLY_PLAN_DAY_ID, -1)
        weeklyPlanDayId = if (rawPlanDayId > 0) rawPlanDayId else null

        initViews()
        bindInitialData()
        setupListeners()
    }

    private fun initViews() {
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvRecommended = findViewById(R.id.tvRecommended)
        tvTimer = findViewById(R.id.tvTimer)
        tvReps = findViewById(R.id.tvReps)
        btnMinusReps = findViewById(R.id.btnMinusReps)
        btnPlusReps = findViewById(R.id.btnPlusReps)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)
        ivExercisePreview = findViewById(R.id.ivExercisePreview)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun bindInitialData() {
        tvExerciseName.text = exerciseName

        // Smart fallback: server defaults > 12 reps for strength moves >
        // 0 only if the move is purely time-based.
        val initialReps = when {
            defaultReps > 0 -> defaultReps
            defaultDurationSec > 0 -> 0   // pure timer exercise like plank
            else -> 12                    // sane strength fallback
        }
        currentReps = initialReps
        tvReps.text = currentReps.toString()

        val recommendedParts = buildList {
            if (initialReps > 0) add("$initialReps reps suggested")
            if (defaultDurationSec > 0) add("${defaultDurationSec / 60} min")
        }

        tvRecommended.text = if (recommendedParts.isEmpty()) {
            "Tap Start when you’re ready"
        } else {
            "Coach suggests: ${recommendedParts.joinToString(" · ")}"
        }

        tvTimer.text = formatTime(defaultDurationSec)
        elapsedSeconds = defaultDurationSec.toLong()
        btnStartPause.text = "Start"

        bindExercisePreview()
    }

    private fun bindExercisePreview() {
        val iv = ivExercisePreview ?: return
        val rawName = assetImageName
            ?.substringBeforeLast(".")
            ?.trim()
            ?.lowercase()
            ?.replace("-", "_")
            ?.replace(" ", "_")
            .orEmpty()

        val resId = if (rawName.isNotBlank()) {
            resources.getIdentifier(rawName, "drawable", packageName)
        } else 0

        if (resId != 0) {
            iv.setImageResource(resId)
            iv.visibility = ImageView.VISIBLE
        } else {
            iv.visibility = ImageView.GONE
        }
    }

    private fun setupListeners() {
        btnMinusReps.setOnClickListener {
            if (currentReps > 0) {
                currentReps--
                tvReps.text = currentReps.toString()
            }
        }

        btnPlusReps.setOnClickListener {
            currentReps++
            tvReps.text = currentReps.toString()
        }

        btnStartPause.setOnClickListener {
            if (isRunning) pauseWorkout() else startWorkout()
        }

        btnFinish.setOnClickListener {
            finishWorkout()
        }
    }

    private fun startWorkout() {
        isRunning = true
        btnStartPause.text = "Pause"

        timer?.cancel()
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                tvTimer.text = formatTime(elapsedSeconds.toInt())
            }

            override fun onFinish() = Unit
        }.start()
    }

    private fun pauseWorkout() {
        isRunning = false
        btnStartPause.text = "Resume"
        timer?.cancel()
    }

    private fun finishWorkout() {
        timer?.cancel()

        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("exercise_name", exerciseName)
            putExtra("exercise_slug", exerciseSlug)
            putExtra("duration", elapsedSeconds.toInt())
            putExtra("reps", currentReps)
            weeklyPlanDayId?.let { putExtra("weekly_plan_day_id", it) }
        }

        startActivity(intent)
        finish()
    }

    private fun formatTime(totalSec: Int): String {
        val minutes = TimeUnit.SECONDS.toMinutes(totalSec.toLong())
        val seconds = totalSec % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    companion object {
        private const val EXTRA_EXERCISE_NAME = "extra_exercise_name"
        private const val EXTRA_EXERCISE_SLUG = "extra_exercise_slug"
        private const val EXTRA_ASSET_IMAGE_NAME = "extra_asset_image_name"
        private const val EXTRA_DEFAULT_REPS = "extra_default_reps"
        private const val EXTRA_DEFAULT_DURATION_SEC = "extra_default_duration_sec"
        private const val EXTRA_WEEKLY_PLAN_DAY_ID = "extra_weekly_plan_day_id"

        fun newIntent(
            context: Context,
            exerciseName: String,
            exerciseSlug: String,
            defaultReps: Int,
            defaultDurationSec: Int,
            assetImageName: String? = null,
            weeklyPlanDayId: Int? = null
        ): Intent {
            return Intent(context, GenericWorkoutActivity::class.java).apply {
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
                putExtra(EXTRA_EXERCISE_SLUG, exerciseSlug)
                putExtra(EXTRA_DEFAULT_REPS, defaultReps)
                putExtra(EXTRA_DEFAULT_DURATION_SEC, defaultDurationSec)
                assetImageName?.let { putExtra(EXTRA_ASSET_IMAGE_NAME, it) }
                weeklyPlanDayId?.let { putExtra(EXTRA_WEEKLY_PLAN_DAY_ID, it) }
            }
        }
    }
}
