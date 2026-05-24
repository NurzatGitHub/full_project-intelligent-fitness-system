package com.example.fitnesscoachai.ui.workout.squat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
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
import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import com.example.fitnesscoachai.ui.workout.shared.PoseRotation
import com.example.fitnesscoachai.ui.workout.shared.PoseStabilizer
import com.example.fitnesscoachai.ui.workout.shared.RgbaToBitmap
import com.example.fitnesscoachai.ui.workout.shared.Segment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SquatActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView

    private lateinit var tvTimer: android.widget.TextView
    private lateinit var tvExerciseName: android.widget.TextView
    private lateinit var tvReps: android.widget.TextView
    private lateinit var tvFeedback: android.widget.TextView

    private lateinit var btnSwitchCamera: android.widget.ImageButton
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnFinish: MaterialButton

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var squatModel: SquatModel
    private var poseHelper: PoseLandmarkerHelper? = null

    private val stabilizer = PoseStabilizer()
    private val readyCheck = SquatReadyCheck()

    private var isWorkoutActive = false
    private var elapsedSeconds: Long = 0
    private var timer: CountDownTimer? = null
    private var repCount = 0

    private enum class SquatPhase { UP, DOWN }

    private var phase = SquatPhase.UP
    private var downStreak = 0
    private var upStreak = 0

    private var reachedDepthInCurrentRep = false
    private var bottomHoldStreak = 0

    private var readyStreak = 0
    private val readyStreakNeed = 3
    private var isReady = false

    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        initViews()
        initDependencies()
        setupListeners()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                10
            )
        }
    }

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        tvTimer = findViewById(R.id.tvTimer)
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvReps = findViewById(R.id.tvReps)
        tvFeedback = findViewById(R.id.tvFeedback)

        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)

        overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        tvTimer.text = "00:00"
        tvReps.text = "0"
        tvFeedback.text = "Tap Start"
        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Squat Training"
    }

    private fun initDependencies() {
        squatModel = SquatModel(this)
        poseHelper = PoseLandmarkerHelper(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener {
            if (isWorkoutActive) pauseWorkout() else startWorkout()
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
        tvTimer.text = "00:00"
        btnStartPause.text = "Pause"

        resetWorkoutState()
        startTimer()
    }

    private fun pauseWorkout() {
        isWorkoutActive = false
        btnStartPause.text = "Resume"
        timer?.cancel()
    }

    private fun finishWorkout() {
        timer?.cancel()

        val exerciseName = intent.getStringExtra("exercise_name") ?: "Squat"
        val exerciseSlug = intent.getStringExtra("exercise_slug") ?: "squat"
        val weeklyPlanDayId = intent.getIntExtra("weekly_plan_day_id", -1)

        val summaryIntent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("exercise_name", exerciseName)
            putExtra("exercise_slug", exerciseSlug)
            if (weeklyPlanDayId > 0) {
                putExtra("weekly_plan_day_id", weeklyPlanDayId)
            }
            putExtra("duration", elapsedSeconds.toInt())
            putExtra("reps", repCount)
        }

        startActivity(summaryIntent)
        finish()
    }

    private fun resetWorkoutState() {
        stabilizer.reset()
        readyCheck.reset()

        repCount = 0
        phase = SquatPhase.UP
        downStreak = 0
        upStreak = 0
        reachedDepthInCurrentRep = false
        bottomHoldStreak = 0

        readyStreak = 0
        isReady = false

        tvReps.text = "0"
        tvFeedback.text = "Stand up straight to begin"
    }

    private fun startTimer() {
        timer?.cancel()
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

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                cameraProvider = future.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(this)
        )
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
                val (displayWidth, displayHeight) =
                    if (rotation == 90 || rotation == 270) {
                        imageProxy.height to imageProxy.width
                    } else {
                        imageProxy.width to imageProxy.height
                    }

                overlayView.setImageSize(displayWidth, displayHeight)

                val bitmap = RgbaToBitmap.toBitmap(imageProxy)
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = poseHelper?.detectVideo(mpImage, System.currentTimeMillis())
                val pose = result?.landmarks()?.firstOrNull() ?: return@setAnalyzer

                val mapped = PoseMapper.mapTo18(pose)
                val stable = stabilizer.apply(mapped) ?: mapped
                val fixed = PoseRotation.rotate(stable, rotation)

                val uiState = if (!isReady) {
                    handleReadyState(fixed)
                } else {
                    handleActiveState(fixed)
                }

                runOnUiThread {
                    tvReps.text = repCount.toString()
                    tvFeedback.text = uiState.feedback
                    overlayView.updatePose(fixed, uiState.segments)
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

    private fun handleReadyState(points: List<PosePoint>): UiState {
        val readyResult = readyCheck.check(points)

        if (readyResult.isReady) {
            readyStreak++
        } else {
            readyStreak = 0
        }

        if (readyStreak >= readyStreakNeed) {
            isReady = true
            return UiState(
                feedback = "Start squatting",
                segments = buildReadySegments()
            )
        }

        return UiState(
            feedback = if (readyResult.isReady) "Ready" else readyResult.hint.ifBlank { "Stand upright" },
            segments = if (readyResult.isReady) buildReadySegments() else emptyList()
        )
    }

    private fun handleActiveState(fixed: List<PosePoint>): UiState {
        val features = SquatFeatureExtractor.extract(fixed)
            ?: return UiState(
                feedback = "Return to squat position",
                segments = buildInvalidSegments()
            )

        val leftKnee = features[1]
        val rightKnee = features[2]
        val kneeDiff = features[3]
        val kneeCaveRatio = features[5]
        val depthRatio = features[6]
        val trunkAngle = features[7]

        val lSh = fixed[4]
        val rSh = fixed[5]
        val lHip = fixed[10]
        val rHip = fixed[11]
        val lKneeP = fixed[12]
        val rKneeP = fixed[13]
        val lAnk = fixed[14]
        val rAnk = fixed[15]

        val shoulderLevelOk = abs(lSh.y - rSh.y) < 0.12f
        val hipLevelOk = abs(lHip.y - rHip.y) < 0.12f
        val kneeLevelOk = abs(lKneeP.y - rKneeP.y) < 0.14f
        val ankleLevelOk = abs(lAnk.y - rAnk.y) < 0.12f

        val hipWidth = abs(lHip.x - rHip.x)
        val kneeWidth = abs(lKneeP.x - rKneeP.x)
        val ankleWidth = abs(lAnk.x - rAnk.x)

        val stanceRatio = kneeWidth / (hipWidth + 1e-6f)
        val ankleRatio = ankleWidth / (hipWidth + 1e-6f)

        val stanceOk = hipWidth > 0.01f &&
                stanceRatio in 0.45f..1.80f &&
                ankleRatio in 0.55f..2.20f

        val supportOk = ankleLevelOk && kneeLevelOk && stanceOk
        val symmetryOk = shoulderLevelOk && hipLevelOk && kneeDiff < 22f

        val fullStandingReturn =
            leftKnee > 163f &&
                    rightKnee > 163f &&
                    trunkAngle < 23f &&
                    depthRatio < 0.82f &&
                    supportOk &&
                    symmetryOk

        val almostStanding =
            leftKnee > 152f &&
                    rightKnee > 152f &&
                    trunkAngle < 28f &&
                    supportOk &&
                    symmetryOk

        val squatBottomValid =
            leftKnee in 70f..138f &&
                    rightKnee in 70f..138f &&
                    depthRatio < 0.92f &&
                    trunkAngle < 34f &&
                    supportOk &&
                    symmetryOk &&
                    kneeCaveRatio < 2.35f

        val obviouslyInvalid =
            !supportOk ||
                    !symmetryOk ||
                    trunkAngle > 40f ||
                    kneeCaveRatio >= 2.75f ||
                    leftKnee < 50f ||
                    rightKnee < 50f

        if (obviouslyInvalid) {
            downStreak = 0
            upStreak = 0
            bottomHoldStreak = 0

            return UiState(
                feedback = "Return to squat position",
                segments = buildInvalidSegments()
            )
        }

        if (squatBottomValid) {
            bottomHoldStreak++
            if (bottomHoldStreak >= 2) {
                reachedDepthInCurrentRep = true
            }
            downStreak++
            upStreak = 0

        } else if (fullStandingReturn) {
            upStreak++
            downStreak = 0
            bottomHoldStreak = 0

        } else if (almostStanding) {
            downStreak = 0
            upStreak = 0
            bottomHoldStreak = 0

            return UiState(
                feedback = "Stand fully upright",
                segments = buildPartialWarningSegments(
                    trunkGood = trunkAngle < 28f,
                    kneesGood = kneeCaveRatio < 2.35f && kneeDiff < 22f,
                    legsGood = supportOk
                )
            )

        } else {
            downStreak = 0
            upStreak = 0
            bottomHoldStreak = 0

            return UiState(
                feedback = "Return to squat position",
                segments = buildPartialWarningSegments(
                    trunkGood = trunkAngle < 35f,
                    kneesGood = kneeCaveRatio < 2.35f && kneeDiff < 22f,
                    legsGood = supportOk
                )
            )
        }

        if (phase == SquatPhase.UP && downStreak >= 3) {
            phase = SquatPhase.DOWN
            downStreak = 0
        }

        if (phase == SquatPhase.DOWN && upStreak >= 3) {
            phase = SquatPhase.UP
            upStreak = 0

            if (reachedDepthInCurrentRep) {
                repCount++
            }

            reachedDepthInCurrentRep = false
            bottomHoldStreak = 0
        }

        val feedback = when {
            !supportOk -> "Keep both feet grounded"
            kneeCaveRatio >= 2.35f -> "Keep your knees out"
            trunkAngle >= 34f -> "Keep your chest up"
            squatBottomValid -> "Good squat"
            fullStandingReturn -> "Ready"
            almostStanding -> "Stand fully upright"
            else -> "Return to squat position"
        }

        val segments = when {
            squatBottomValid -> buildGoodSquatSegments()
            fullStandingReturn -> buildReadySegments()
            else -> buildPartialWarningSegments(
                trunkGood = trunkAngle < 35f,
                kneesGood = kneeCaveRatio < 2.35f && kneeDiff < 22f,
                legsGood = supportOk
            )
        }

        return UiState(
            feedback = feedback,
            segments = segments
        )
    }

    private fun buildInvalidSegments(): List<Segment> {
        val red = "#FF0000"
        return listOf(
            Segment(4, 5, red),
            Segment(10, 11, red),
            Segment(4, 10, red),
            Segment(5, 11, red),
            Segment(10, 12, red),
            Segment(12, 14, red),
            Segment(11, 13, red),
            Segment(13, 15, red),
        )
    }

    private fun buildReadySegments(): List<Segment> {
        val green = "#00FF00"
        return listOf(
            Segment(4, 5, green),
            Segment(10, 11, green),
            Segment(4, 10, green),
            Segment(5, 11, green),
            Segment(10, 12, green),
            Segment(12, 14, green),
            Segment(11, 13, green),
            Segment(13, 15, green),
        )
    }

    private fun buildGoodSquatSegments(): List<Segment> {
        val green = "#00FF00"
        return listOf(
            Segment(4, 5, green),
            Segment(10, 11, green),
            Segment(4, 10, green),
            Segment(5, 11, green),
            Segment(10, 12, green),
            Segment(12, 14, green),
            Segment(11, 13, green),
            Segment(13, 15, green),
        )
    }

    private fun buildPartialWarningSegments(
        trunkGood: Boolean,
        kneesGood: Boolean,
        legsGood: Boolean
    ): List<Segment> {
        val green = "#00FF00"
        val red = "#FF0000"

        val trunkColor = if (trunkGood) green else red
        val kneeColor = if (kneesGood) green else red
        val legColor = if (legsGood) green else red

        return listOf(
            Segment(4, 5, trunkColor),
            Segment(10, 11, trunkColor),
            Segment(4, 10, trunkColor),
            Segment(5, 11, trunkColor),
            Segment(10, 12, kneeColor),
            Segment(11, 13, kneeColor),
            Segment(12, 14, legColor),
            Segment(13, 15, legColor),
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
        squatModel.close()
    }

    private data class UiState(
        val feedback: String,
        val segments: List<Segment>
    )
}