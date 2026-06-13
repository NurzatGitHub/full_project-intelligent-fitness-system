package com.example.fitnesscoachai.ui.exercise

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.ui.workout.generic.GenericWorkoutActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class ExerciseInstructionActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    private var loadedExerciseName: String = ""
    private var loadedExerciseSlug: String = ""
    private var loadedDefaultReps: Int = 0
    private var loadedDefaultDurationSec: Int = 0
    private var loadedAssetImageName: String? = null
    private var loadedWeeklyPlanDayId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_instruction)

        val toolbar =
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val exerciseSlug = intent.getStringExtra(EXTRA_EXERCISE_SLUG)
        if (exerciseSlug.isNullOrBlank()) {
            finish()
            return
        }

        loadedExerciseSlug = exerciseSlug

        val rawPlanDayId = intent.getIntExtra(EXTRA_WEEKLY_PLAN_DAY_ID, -1)
        loadedWeeklyPlanDayId = if (rawPlanDayId > 0) rawPlanDayId else null

        playerView = findViewById(R.id.playerView)

        findViewById<MaterialButton>(R.id.btnCompleteExercise).setOnClickListener {
            if (loadedExerciseName.isBlank()) {
                Toast.makeText(this, "Exercise is still loading", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(
                GenericWorkoutActivity.newIntent(
                    context = this,
                    exerciseName = loadedExerciseName,
                    exerciseSlug = loadedExerciseSlug,
                    defaultReps = loadedDefaultReps,
                    defaultDurationSec = loadedDefaultDurationSec,
                    assetImageName = loadedAssetImageName,
                    weeklyPlanDayId = loadedWeeklyPlanDayId
                )
            )
        }

        loadExerciseDetail(exerciseSlug)
    }

    private fun getBearerToken(): String? {
        // Source of truth: encrypted TokenStore.
        val secure = com.example.fitnesscoachai.data.api.RetrofitClient.tokenStore().accessToken
        val token = secure
            ?: getSharedPreferences("auth", Context.MODE_PRIVATE).getString("access_token", null)
        return if (token.isNullOrBlank()) null else "Bearer $token"
    }

    private fun loadExerciseDetail(slug: String) {
        val token = getBearerToken()
        if (token == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val stepsContainer = findViewById<LinearLayout>(R.id.stepsContainer)
        val tipsContainer = findViewById<LinearLayout>(R.id.tipsContainer)
        val videoContainer = findViewById<View>(R.id.videoContainer)
        val tvQuickCompleteHint = findViewById<TextView>(R.id.tvQuickCompleteHint)
        val btnCompleteExercise = findViewById<MaterialButton>(R.id.btnCompleteExercise)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getExerciseDetail(
                    bearer = token,
                    slug = slug
                )

                if (!response.isSuccessful || response.body() == null) {
                    Toast.makeText(
                        this@ExerciseInstructionActivity,
                        "Failed to load exercise",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                val exercise = response.body()!!

                loadedExerciseName = exercise.name
                loadedExerciseSlug = exercise.slug
                loadedDefaultReps = exercise.default_reps ?: 0
                loadedDefaultDurationSec = (exercise.default_duration_min ?: 0) * 60
                loadedAssetImageName = exercise.asset_image_name?.takeIf { it.isNotBlank() }

                supportActionBar?.title = exercise.name
                findViewById<TextView>(R.id.tvExerciseName).text = exercise.name
                findViewById<TextView>(R.id.tvExerciseMeta).text = buildString {
                    if (exercise.equipment.isNotBlank()) {
                        append(exercise.equipment.replaceFirstChar { it.uppercase() })
                    }
                    if (exercise.difficulty.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(exercise.difficulty.replaceFirstChar { it.uppercase() })
                    }
                    if (isEmpty()) append("-")
                }
                findViewById<TextView>(R.id.tvDescription).text = exercise.description

                val quickMeta = buildList {
                    if ((exercise.default_reps ?: 0) > 0) add("${exercise.default_reps} reps")
                    if ((exercise.default_duration_min ?: 0) > 0) add("${exercise.default_duration_min} min")
                }.joinToString(" • ")

                tvQuickCompleteHint.text =
                    if (quickMeta.isBlank()) {
                        "Start a workout and enter your real results."
                    } else {
                        "Recommended: $quickMeta"
                    }

                btnCompleteExercise.text = "Start workout"

                val videoAssetName = exercise.asset_video_name.trim()
                if (videoAssetName.isNotEmpty()) {
                    val cached = cacheVideoIfNeeded("videos/${videoAssetName}.mp4")
                    if (cached != null && cached.exists()) {
                        videoContainer.visibility = View.VISIBLE
                        initializePlayer(cached)
                    } else {
                        videoContainer.visibility = View.GONE
                    }
                } else {
                    videoContainer.visibility = View.GONE
                }

                stepsContainer.removeAllViews()
                exercise.steps.forEachIndexed { index, step ->
                    val view = LayoutInflater.from(this@ExerciseInstructionActivity)
                        .inflate(R.layout.item_step, stepsContainer, false)
                    (view as TextView).text = "${index + 1}. $step"
                    stepsContainer.addView(view)
                }

                tipsContainer.removeAllViews()
                exercise.tips_list.forEach { tip ->
                    val view = LayoutInflater.from(this@ExerciseInstructionActivity)
                        .inflate(R.layout.item_step, tipsContainer, false)
                    (view as TextView).text = "• $tip"
                    tipsContainer.addView(view)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ExerciseInstructionActivity,
                    "Network error",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun cacheVideoIfNeeded(assetPath: String): File? {
        return try {
            val fileName = assetPath.substringAfterLast('/')
            val outFile = File(cacheDir, fileName)
            assets.open(assetPath).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: IOException) {
            null
        }
    }

    private fun initializePlayer(file: File) {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exoPlayer ->
                playerView?.player = exoPlayer
                val mediaItem = MediaItem.fromUri(file.toURI().toString())
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } else {
            val exoPlayer = player!!
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    private fun releasePlayer() {
        playerView?.player = null
        player?.release()
        player = null
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    companion object {
        const val EXTRA_EXERCISE_SLUG = "extra_exercise_slug"
        const val EXTRA_WEEKLY_PLAN_DAY_ID = "extra_weekly_plan_day_id"

        fun newIntent(
            context: Context,
            exerciseSlug: String,
            weeklyPlanDayId: Int? = null
        ) = android.content.Intent(context, ExerciseInstructionActivity::class.java).apply {
            putExtra(EXTRA_EXERCISE_SLUG, exerciseSlug)
            weeklyPlanDayId?.let { putExtra(EXTRA_WEEKLY_PLAN_DAY_ID, it) }
        }
    }
}