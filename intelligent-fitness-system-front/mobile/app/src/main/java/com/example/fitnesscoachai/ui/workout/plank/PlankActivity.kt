package com.example.fitnesscoachai.ui.workout.plank

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

class PlankActivity : AppCompatActivity() {

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
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private lateinit var plankModel: PlankModel
    private var poseHelper: PoseLandmarkerHelper? = null

    private val stabilizer = PoseStabilizer()
    private val readyCheck = PlankReadyCheck()

    private val CORRECT_STREAK_START = 3
    private val INCORRECT_STREAK_BREAK = 8
    private val READY_STREAK_NEED = 2

    private var isWorkoutActive = false
    private var elapsedSeconds = 0L
    private var holdSeconds = 0L
    private var timer: CountDownTimer? = null

    private val formScore = FormScoreTracker()

    private var isHolding = false
    private var correctStreak = 0
    private var incorrectStreak = 0

    private var readyStreak = 0
    private var isReady = false

    private val TAG = "PlankActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plank)

        initViews()

        plankModel = PlankModel(this)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
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
        plankModel.close()
    }

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvTimer = findViewById(R.id.tvTimer)
        tvReps = findViewById(R.id.tvReps)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)
        tvExerciseName = findViewById(R.id.tvExerciseName)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)

        overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        tvTimer.text = "00:00"
        tvReps.text = "00:00"
        tvFeedback.text = "Tap Start"
        btnStartPause.text = "Start"
        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Plank"
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener {
            when {
                isWorkoutActive -> pauseWorkout()
                elapsedSeconds == 0L -> startWorkout()
                else -> resumeWorkout()
            }
        }

        btnFinish.setOnClickListener {
            finishWorkout()
        }

        btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)
            bindCameraUseCases()
        }
    }

    private fun startWorkout() {
        isWorkoutActive = true
        elapsedSeconds = 0L
        holdSeconds = 0L
        formScore.reset()
        correctStreak = 0
        incorrectStreak = 0
        readyStreak = 0
        isReady = false
        isHolding = false

        stabilizer.reset()
        readyCheck.reset()

        tvTimer.text = "00:00"
        tvReps.text = "00:00"
        tvFeedback.text = "Get into plank position"
        btnStartPause.text = "Pause"

        startSessionTimer()
    }

    private fun pauseWorkout() {
        isWorkoutActive = false
        isHolding = false
        btnStartPause.text = "Resume"
        timer?.cancel()
    }

    private fun resumeWorkout() {
        isWorkoutActive = true
        btnStartPause.text = "Pause"
        startSessionTimer()
    }

    private fun startSessionTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isWorkoutActive) return

                elapsedSeconds++

                if (isHolding) {
                    holdSeconds++
                    Log.d(TAG, "holding: $holdSeconds")
                }

                runOnUiThread {
                    tvTimer.text = formatTime(elapsedSeconds)
                    tvReps.text = formatTime(holdSeconds)
                }
            }

            override fun onFinish() = Unit
        }.start()
    }

    private fun finishWorkout() {
        timer?.cancel()
        isWorkoutActive = false

        val exerciseName = intent.getStringExtra("exercise_name") ?: "Plank"
        val exerciseSlug = intent.getStringExtra("exercise_slug") ?: "plank"
        val weeklyPlanDayId = intent.getIntExtra("weekly_plan_day_id", -1)

        // Plank is isometric — one finished session = 1 rep on the backend
        // regardless of how long the user held the pose. The seconds-held
        // is shipped separately so SummaryActivity can show "33 seconds held"
        // in the UI and history without inflating total_reps by dozens per
        // session. Previously we packed hold seconds INTO reps and that
        // exploded total_reps after every plank (73 → 106 from one set).
        val heldAtLeastOnce = holdSeconds > 0L
        val backendReps = if (heldAtLeastOnce) 1 else 0
        val displayHoldSeconds = holdSeconds.toInt().coerceAtLeast(0)

        Log.d(TAG, "finish: elapsed=$elapsedSeconds hold=$holdSeconds backendReps=$backendReps")

        val summaryIntent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("exercise_name", exerciseName)
            putExtra("exercise_slug", exerciseSlug)
            if (weeklyPlanDayId > 0) {
                putExtra("weekly_plan_day_id", weeklyPlanDayId)
            }
            putExtra("duration", elapsedSeconds.toInt())
            // What the backend sees in total_reps. Always 0 or 1 for plank.
            putExtra("reps", backendReps)
            // What the UI + local history display. Seconds-held, never
            // forwarded to the server's rep count.
            putExtra("hold_seconds", displayHoldSeconds)
            // Flag so SummaryActivity knows to render "X seconds held"
            // instead of "X reps" for this isometric exercise.
            putExtra("is_isometric", true)
            // -1f if no analyzed frames; SummaryActivity skips it in that case.
            putExtra("form_score", formScore.percent())
        }

        startActivity(summaryIntent)
        finish()
    }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                cameraProvider = future.get()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this))
        }
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
                processFrame(imageProxy)
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

    private fun processFrame(imageProxy: ImageProxy) {
        if (!isWorkoutActive) return

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
        val pose = result?.landmarks()?.firstOrNull() ?: return

        val mapped = PoseMapper.mapTo18(pose)
        val stable = stabilizer.apply(mapped) ?: mapped
        val fixed = PoseRotation.rotate(stable, rotation)

        val (segments, feedbackText) = if (!isReady) {
            processReadyGate(fixed)
        } else {
            processPlankAnalysis(fixed)
        }

        runOnUiThread {
            tvFeedback.text = feedbackText
            overlayView.updatePose(fixed, segments)
        }
    }

    private fun processReadyGate(fixed: List<PosePoint>): Pair<List<Segment>, String> {
        val readyResult = readyCheck.check(fixed)
        val feedbackText = readyResult.hint.ifEmpty { "Get into plank position" }

        return if (readyResult.isReady) {
            readyStreak++
            if (readyStreak >= READY_STREAK_NEED) {
                isReady = true
                isHolding = false
                correctStreak = 0
                incorrectStreak = 0
                Log.d(TAG, "ReadyGate PASSED")
            }
            PoseSkeleton.segments.map { it.copy(color = "#AAAAAA") } to feedbackText
        } else {
            readyStreak = 0
            emptyList<Segment>() to feedbackText
        }
    }

    private fun processPlankAnalysis(fixed: List<PosePoint>): Pair<List<Segment>, String> {
        val features = PlankFeatureExtractor.extract(fixed) ?: run {
            isHolding = false
            correctStreak = 0
            incorrectStreak = 0
            return emptyList<Segment>() to "Show full body"
        }

        val bodyLineAngle = features[0]
        val hipOffset = features[1]
        val shHipKnee = features[2]
        val hipKneeAnkle = features[3]
        val trunkAngle = features[4]
        val elShHip = features[5]
        val headOffset = features[6]

        val prediction = plankModel.predict(features)

        // Смягчённые пороги = золотая середина
        val bodyLineGood = bodyLineAngle >= 148f
        val hipsGood = hipOffset in -0.14f..0.14f
        val trunkGood = trunkAngle <= 24f
        val legsGood = hipKneeAnkle >= 138f

        // Почти хорошо — не красим сразу всё в красный
        val almostGood =
            bodyLineAngle >= 142f &&
                    hipOffset in -0.18f..0.18f &&
                    trunkAngle <= 28f &&
                    hipKneeAnkle >= 130f

        val geometryCorrect = bodyLineGood && hipsGood && trunkGood && legsGood
        val finalCorrect = prediction.label == "correct" && geometryCorrect

        // Feed the combined verdict into the form-score aggregator so plank
        // sessions actually appear in the Form Stats chart. Using `finalCorrect`
        // (model + geometry) makes the score reward staying perfectly in the
        // plank pose; any frame where the user drops out counts against it.
        formScore.sample(if (finalCorrect) "correct" else "incorrect")

        var segments = PoseSkeleton.segments
        var feedbackText = when {
            hipOffset > 0.14f -> "Drop your hips"
            hipOffset < -0.14f -> "Raise your hips"
            !bodyLineGood && !almostGood -> "Keep your body straight"
            !trunkGood && !almostGood -> "Keep your torso level"
            !legsGood && !almostGood -> "Straighten your legs"
            finalCorrect -> "Hold!"
            almostGood -> "Almost there"
            else -> "Keep going"
        }

        if (finalCorrect) {
            correctStreak++
            incorrectStreak = 0

            if (!isHolding && correctStreak >= CORRECT_STREAK_START) {
                isHolding = true
                correctStreak = 0
                Log.d(TAG, "Hold STARTED")
            }

            if (isHolding) {
                segments = segments.map { it.copy(color = "#00C853") }
                feedbackText = "Hold!"
            } else {
                segments = segments.map { it.copy(color = "#00C853") }
            }

        } else {
            incorrectStreak++
            correctStreak = 0

            segments = if (almostGood) {
                // не рубим всё красным, если поза почти норм
                PoseSkeleton.segments.map { it.copy(color = "#00C853") }
            } else {
                PoseSkeleton.segments.map { it.copy(color = "#FF0000") }
            }

            if (isHolding && incorrectStreak >= INCORRECT_STREAK_BREAK) {
                isHolding = false
                Log.d(TAG, "Hold BROKEN")
            }
        }

        Log.d(
            TAG,
            "bodyLine=%.1f hipOffset=%.3f shHipKnee=%.1f hipKneeAnkle=%.1f trunk=%.1f elShHip=%.1f head=%.3f geom=%s almost=%s model=%s conf=%s hold=%s"
                .format(
                    bodyLineAngle,
                    hipOffset,
                    shHipKnee,
                    hipKneeAnkle,
                    trunkAngle,
                    elShHip,
                    headOffset,
                    geometryCorrect,
                    almostGood,
                    prediction.label,
                    prediction.confidence?.toString() ?: "null",
                    isHolding
                )
        )

        return segments to feedbackText
    }

    private fun formatTime(seconds: Long): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format("%02d:%02d", min, sec)
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
}