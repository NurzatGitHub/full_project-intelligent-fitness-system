package com.example.fitnesscoachai.ui.workout.shoulderpress

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import com.example.fitnesscoachai.ui.workout.shared.OverlayView
import com.example.fitnesscoachai.ui.workout.shared.PoseLandmarkerHelper
import com.example.fitnesscoachai.ui.workout.shared.PoseMapper
import com.example.fitnesscoachai.ui.workout.shared.PoseRotation
import com.example.fitnesscoachai.ui.workout.shared.PoseSkeleton
import com.example.fitnesscoachai.ui.workout.shared.PoseStabilizer
import com.example.fitnesscoachai.ui.workout.shared.RgbaToBitmap
import com.example.fitnesscoachai.ui.workout.shared.Segment
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class ShoulderPressPhase { DOWN, MID, UP }

class ShoulderPressActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView

    private lateinit var tvTimer: TextView
    private lateinit var tvExerciseName: TextView
    private lateinit var tvReps: TextView
    private lateinit var tvFeedback: TextView

    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnFinish: MaterialButton

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var shoulderPressModel: ShoulderPressModel
    private var poseHelper: PoseLandmarkerHelper? = null

    private val stabilizer = PoseStabilizer()
    private val readyCheck = ShoulderPressReadyCheck()

    private var isWorkoutActive = false
    private var elapsedSeconds: Long = 0
    private var timer: CountDownTimer? = null

    private var repCount = 0

    private var phase = ShoulderPressPhase.UP
    private var downStreak = 0
    private var upStreak = 0

    private var readyStreak = 0
    private val READY_STREAK_NEED = 3
    private var isReady = false

    private val PHASE_STREAK_NEED = 3

    private var singleArmMode = false
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        singleArmMode = intent.getBooleanExtra("single_arm", false)

        initializeViews()

        shoulderPressModel = ShoulderPressModel(this)
        poseHelper = PoseLandmarkerHelper(this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                10
            )
        }

        setupListeners()
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        tvTimer = findViewById(R.id.tvTimer)
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvReps = findViewById(R.id.tvReps)
        tvFeedback = findViewById(R.id.tvFeedback)

        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)

        overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)

        tvTimer.text = "00:00"
        tvReps.text = "0"
        tvFeedback.text = "Tap Start"
        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Shoulder Press"
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener {
            if (!isWorkoutActive) startWorkout() else pauseWorkout()
        }

        btnFinish.setOnClickListener {
            finishWorkout()
        }

        btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

            overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)
            bindCameraUseCases()
        }
    }

    private fun startWorkout() {
        isWorkoutActive = true
        elapsedSeconds = 0
        repCount = 0

        downStreak = 0
        upStreak = 0
        readyStreak = 0
        isReady = false
        phase = ShoulderPressPhase.UP

        stabilizer.reset()
        readyCheck.reset()

        tvTimer.text = "00:00"
        tvReps.text = "0"
        tvFeedback.text = "Get into shoulder press position"
        btnStartPause.text = "Pause"

        timer?.cancel()
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                val minutes = TimeUnit.SECONDS.toMinutes(elapsedSeconds)
                val seconds = elapsedSeconds % 60
                tvTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() = Unit
        }.start()
    }

    private fun pauseWorkout() {
        isWorkoutActive = false
        btnStartPause.text = "Resume"
        timer?.cancel()
    }

    private fun finishWorkout() {
        timer?.cancel()

        val exerciseName = intent.getStringExtra("exercise_name") ?: "Shoulder Press"
        val exerciseSlug = intent.getStringExtra("exercise_slug") ?: "shoulder-press"
        val weeklyPlanDayId = intent.getIntExtra("weekly_plan_day_id", -1)

        startActivity(Intent(this, SummaryActivity::class.java).apply {
            putExtra("exercise_name", exerciseName)
            putExtra("exercise_slug", exerciseSlug)
            if (weeklyPlanDayId > 0) {
                putExtra("weekly_plan_day_id", weeklyPlanDayId)
            }
            putExtra("duration", elapsedSeconds.toInt())
            putExtra("reps", repCount)
        })

        finish()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            cameraProvider = future.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                if (!isWorkoutActive) return@setAnalyzer

                val rotation = imageProxy.imageInfo.rotationDegrees

                val (dispW, dispH) = if (rotation == 90 || rotation == 270) {
                    imageProxy.height to imageProxy.width
                } else {
                    imageProxy.width to imageProxy.height
                }
                overlayView.setImageSize(dispW, dispH)

                val bitmap = RgbaToBitmap.toBitmap(imageProxy)
                val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
                val result = poseHelper?.detectVideo(mpImage, System.currentTimeMillis())
                val pose = result?.landmarks()?.firstOrNull() ?: return@setAnalyzer

                val mapped = PoseMapper.mapTo18(pose)
                val stable = stabilizer.apply(mapped) ?: mapped
                val fixed = PoseRotation.rotate(stable, rotation)

                var segments = emptyList<Segment>()
                var feedbackText = ""

                if (!isReady) {
                    val readyResult = readyCheck.check(fixed)

                    feedbackText = readyResult.hint.ifEmpty {
                        "Get into shoulder press position"
                    }

                    if (readyResult.isReady) {
                        readyStreak++
                        segments = PoseSkeleton.segments.map { it.copy(color = "#AAAAAA") }
                    } else {
                        readyStreak = 0
                        segments = emptyList()
                    }

                    if (readyStreak >= READY_STREAK_NEED) {
                        isReady = true
                        phase = ShoulderPressPhase.DOWN
                        downStreak = 0
                        upStreak = 0
                    }

                } else {
                    val features = ShoulderPressFeatureExtractor.extract(fixed)

                    if (features != null) {
                        val currentPhase = ShoulderPressFeatureExtractor.phaseFromFeatures(features)

                        val prediction = if (singleArmMode) {
                            ShoulderPressModel.PredictResult("correct", null)
                        } else {
                            shoulderPressModel.predict(features)
                        }

                        feedbackText = if (singleArmMode) {
                            "Single arm"
                        } else {
                            prediction.label
                        }

                        segments = PoseSkeleton.dynamic(
                            leftElbow = features[0],
                            rightElbow = features[1],
                            bodyLine = 180f,
                            hipOffset = features[3]
                        )

                        // ВАЖНО:
                        // не красим весь скелет в красный,
                        // чтобы сохранялась локальная подсветка ошибок
                        val canCountRep = prediction.label == "correct"

                        if (canCountRep) {
                            when (currentPhase) {
                                ShoulderPressPhase.DOWN -> {
                                    downStreak++
                                    upStreak = 0
                                }

                                ShoulderPressPhase.UP -> {
                                    upStreak++
                                    downStreak = 0
                                }

                                ShoulderPressPhase.MID -> {
                                    // промежуточная фаза, streak не сбрасываем
                                }
                            }

                            if (phase == ShoulderPressPhase.DOWN && upStreak >= PHASE_STREAK_NEED) {
                                phase = ShoulderPressPhase.UP
                                upStreak = 0
                                repCount++
                            }

                            if (phase == ShoulderPressPhase.UP && downStreak >= PHASE_STREAK_NEED) {
                                phase = ShoulderPressPhase.DOWN
                                downStreak = 0
                            }
                        } else {
                            downStreak = 0
                            upStreak = 0
                        }

                    } else {
                        feedbackText = "Show full upper body"
                        segments = emptyList()
                        downStreak = 0
                        upStreak = 0
                    }
                }

                val finalSegments = segments
                val finalFeedback = feedbackText

                runOnUiThread {
                    tvReps.text = repCount.toString()
                    tvFeedback.text = finalFeedback
                    overlayView.updatePose(fixed, finalSegments)
                }

            } finally {
                imageProxy.close()
            }
        }

        provider.unbindAll()
        provider.bindToLifecycle(
            this,
            CameraSelector.Builder().requireLensFacing(lensFacing).build(),
            preview,
            analysis
        )
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        cameraExecutor.shutdown()
        poseHelper?.close()
        shoulderPressModel.close()
    }
}