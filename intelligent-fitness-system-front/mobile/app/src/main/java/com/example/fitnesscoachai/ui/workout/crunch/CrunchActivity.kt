package com.example.fitnesscoachai.ui.workout.crunch

import com.example.fitnesscoachai.ui.workout.shared.*

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CrunchActivity : AppCompatActivity() {

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

    private lateinit var crunchModel: CrunchModel
    private var poseHelper: PoseLandmarkerHelper? = null

    private val stabilizer = PoseStabilizer()
    private val readyCheck = CrunchReadyCheck()

    private var isWorkoutActive = false
    private var elapsedSeconds: Long = 0
    private var timer: CountDownTimer? = null

    private var repCount = 0

    private enum class CrunchPhase { UP, DOWN }
    private var phase = CrunchPhase.UP

    private var downStreak = 0
    private var upStreak = 0

    private var reachedDepthInCurrentRep = false
    private var validDownHoldStreak = 0

    private var readyStreak = 0
    private var isReady = false
    private var waitingForFirstDown = true

    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    companion object {
        private const val READY_STREAK_NEED = 3

        // Пороги
        private const val FLAT_T = 158f
        private const val UP_T = 132f
        private const val DOWN_T = 100f

        private const val DOWN_STREAK_NEED = 2
        private const val UP_STREAK_NEED = 2
        private const val VALID_DOWN_HOLD_NEED = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        initViews()

        crunchModel = CrunchModel(this)
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

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvTimer = findViewById(R.id.tvTimer)
        tvReps = findViewById(R.id.tvReps)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        tvExerciseName = findViewById(R.id.tvExerciseName)

        overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        tvTimer.text = "00:00"
        tvReps.text = "0"
        tvFeedback.text = "Tap Start"
        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Crunch"
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener {
            if (!isWorkoutActive) startWorkout() else pauseWorkout()
        }

        btnFinish.setOnClickListener { finishWorkout() }

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
        tvTimer.text = "00:00"

        stabilizer.reset()
        readyCheck.reset()

        repCount = 0
        phase = CrunchPhase.UP
        downStreak = 0
        upStreak = 0
        reachedDepthInCurrentRep = false
        validDownHoldStreak = 0
        readyStreak = 0
        isReady = false
        waitingForFirstDown = true

        tvReps.text = "0"
        tvFeedback.text = "Get ready"
        btnStartPause.text = "Pause"

        timer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
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

        val exerciseName = intent.getStringExtra("exercise_name") ?: "Crunch"
        val exerciseSlug = intent.getStringExtra("exercise_slug") ?: "crunch"
        val weeklyPlanDayId = intent.getIntExtra("weekly_plan_day_id", -1)

        val summaryIntent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("exercise_name", exerciseName)
            putExtra("exercise_slug", exerciseSlug)
            if (weeklyPlanDayId > 0) putExtra("weekly_plan_day_id", weeklyPlanDayId)
            putExtra("duration", elapsedSeconds.toInt())
            putExtra("reps", repCount)
        }
        startActivity(summaryIntent)
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
                    feedbackText = readyResult.hint.ifEmpty { "Lie on your back, knees bent" }

                    if (readyResult.isReady) {
                        readyStreak++
                        segments = PoseSkeleton.segments.map { it.copy(color = "#AAAAAA") }
                    } else {
                        readyStreak = 0
                        segments = emptyList()
                    }

                    if (readyStreak >= READY_STREAK_NEED) {
                        isReady = true
                        phase = CrunchPhase.UP
                        downStreak = 0
                        upStreak = 0
                        reachedDepthInCurrentRep = false
                        validDownHoldStreak = 0
                        waitingForFirstDown = true
                        feedbackText = "Start crunching"
                    }

                } else {
                    val features = CrunchFeatureExtractor.extract(fixed)

                    if (features != null) {
                        val trunkAngle = features[0]
                        val neckAngle = features[1]
                        val hipLift = features[2]
                        val elbowToKnee = features[3]
                        val symmetryDiff = features[4]
                        val kneeAngleAvg = features[5]
                        val shoulderTilt = features[6]

                        val modelResult = crunchModel.predict(features)

                        val isFlat = trunkAngle >= FLAT_T
                        val isUpEnough = trunkAngle >= UP_T
                        val isDownByTrunk = trunkAngle <= DOWN_T

                        val isNeckOnly = neckAngle < 125f && trunkAngle > 105f
                        val isElbowClose = elbowToKnee <= 1.20f
                        val kneesOk = kneeAngleAvg in 25f..130f
                        val hipsOk = hipLift in -1.55f..-0.20f

                        // Только для логов, не для блокировки rep
                        val bodySymmetric = symmetryDiff <= 0.35f
                        val shouldersOk = shoulderTilt <= 50f

                        val validDown = isDownByTrunk &&
                                isElbowClose &&
                                kneesOk &&
                                hipsOk &&
                                !isNeckOnly

                        val almostDown = trunkAngle < 118f && kneesOk && !isNeckOnly

                        feedbackText = when {
                            isFlat -> "Lift your upper body"
                            isNeckOnly -> "Lift shoulders, not only neck"
                            !kneesOk -> "Keep knees bent"
                            !hipsOk -> "Keep hips stable"
                            validDown -> "Good crunch"
                            almostDown -> "Almost there"
                            else -> "Keep going"
                        }

                        val trunkGood = validDown || almostDown || isUpEnough
                        val neckGood = !isNeckOnly

                        segments = buildCrunchSegments(
                            trunkGood = trunkGood,
                            neckGood = neckGood
                        )

                        val crunchDown = validDown
                        val crunchUp = isUpEnough

                        if (crunchDown) {
                            validDownHoldStreak++
                            if (validDownHoldStreak >= VALID_DOWN_HOLD_NEED) {
                                reachedDepthInCurrentRep = true
                            }
                        } else {
                            validDownHoldStreak = 0
                        }

                        when {
                            crunchDown -> {
                                downStreak++
                                upStreak = 0
                            }
                            crunchUp -> {
                                upStreak++
                                downStreak = 0
                            }
                            else -> {
                                downStreak = 0
                                upStreak = 0
                            }
                        }

                        if (phase == CrunchPhase.UP && downStreak >= DOWN_STREAK_NEED) {
                            phase = CrunchPhase.DOWN
                            downStreak = 0
                            waitingForFirstDown = false
                        }

                        if (phase == CrunchPhase.DOWN && upStreak >= UP_STREAK_NEED && !waitingForFirstDown) {
                            phase = CrunchPhase.UP
                            upStreak = 0

                            if (reachedDepthInCurrentRep) {
                                repCount++
                            }

                            reachedDepthInCurrentRep = false
                            validDownHoldStreak = 0
                        }

                        Log.d(
                            "CrunchDebug",
                            "trunk=%.1f neck=%.1f hip=%.2f ek=%.2f sym=%.3f tilt=%.1f knee=%.1f validDown=%s almost=%s up=%s phase=%s model=%s conf=%s reps=%d shouldersOk=%s bodySym=%s"
                                .format(
                                    trunkAngle,
                                    neckAngle,
                                    hipLift,
                                    elbowToKnee,
                                    symmetryDiff,
                                    shoulderTilt,
                                    kneeAngleAvg,
                                    validDown,
                                    almostDown,
                                    crunchUp,
                                    phase.name,
                                    modelResult.label,
                                    modelResult.confidence?.toString() ?: "null",
                                    repCount,
                                    shouldersOk,
                                    bodySymmetric
                                )
                        )
                    } else {
                        feedbackText = "Show hips and knees"
                        segments = emptyList()
                        downStreak = 0
                        upStreak = 0
                        validDownHoldStreak = 0
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

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(this, selector, preview, analysis)
    }

    private fun buildCrunchSegments(
        trunkGood: Boolean,
        neckGood: Boolean
    ): List<Segment> {
        val green = "#00FF00"
        val red = "#FF0000"

        val neckColor = if (neckGood) green else red
        val trunkColor = if (trunkGood) green else red

        return listOf(
            Segment(0, 3, neckColor),
            Segment(3, 4, trunkColor),
            Segment(3, 5, trunkColor),
            Segment(4, 5, trunkColor),
            Segment(4, 10, trunkColor),
            Segment(5, 11, trunkColor),
            Segment(10, 11, trunkColor),
            Segment(10, 12, trunkColor),
            Segment(11, 13, trunkColor),
        )
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

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
        crunchModel.close()
    }
}