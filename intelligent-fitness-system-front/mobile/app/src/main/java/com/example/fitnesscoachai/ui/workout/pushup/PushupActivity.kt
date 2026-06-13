package com.example.fitnesscoachai.ui.workout.pushup

import com.example.fitnesscoachai.ui.workout.shared.*

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
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

class PushupActivity : AppCompatActivity() {

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

    private lateinit var pushupModel: PushupModel
    private var poseHelper: PoseLandmarkerHelper? = null

    private val stabilizer = PoseStabilizer()
    private val readyCheck = PushupReadyCheck()

    private var isWorkoutActive = false
    private var elapsedSeconds: Long = 0
    private var timer: CountDownTimer? = null

    private var repCount = 0

    private val formScore = com.example.fitnesscoachai.ui.workout.shared.FormScoreTracker()

    private enum class PushupPhase { UP, DOWN }

    private var phase = PushupPhase.UP
    private var downStreak = 0
    private var upStreak = 0

    private val DOWN_T = 90f
    private val UP_T = 140f

    private val LABEL_BUF_SIZE = 5
    private val labelBuffer = ArrayDeque<String>(LABEL_BUF_SIZE)

    private var readyStreak = 0
    private val READY_STREAK_NEED = 3
    private var isReady = false

    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        initializeViews()

        pushupModel = PushupModel(this)
        poseHelper = PoseLandmarkerHelper(this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), 10
        )

        setupListeners()
    }

    private fun initializeViews() {
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
        tvReps.text = "0"
        tvFeedback.text = "Tap Start"
        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Push-up"
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
        downStreak = 0
        upStreak = 0
        readyStreak = 0
        isReady = false
        phase = PushupPhase.UP
        labelBuffer.clear()
        formScore.reset()

        btnStartPause.text = "Pause"

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

        val exerciseName = intent.getStringExtra("exercise_name") ?: "Push-up"
        val exerciseSlug = intent.getStringExtra("exercise_slug") ?: "push-up"
        val weeklyPlanDayId = intent.getIntExtra("weekly_plan_day_id", -1)

        val summaryIntent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("exercise_name", exerciseName)
            putExtra("exercise_slug", exerciseSlug)
            if (weeklyPlanDayId > 0) {
                putExtra("weekly_plan_day_id", weeklyPlanDayId)
            }
            putExtra("duration", elapsedSeconds.toInt())
            putExtra("reps", repCount)
            putExtra("form_score", formScore.percent())
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

                    feedbackText = readyResult.hint.ifEmpty {
                        "Get into push-up position"
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
                        phase = PushupPhase.UP
                        downStreak = 0
                        upStreak = 0
                    }

                } else {
                    // Проверяем — человек ещё в позиции отжимания или встал?
                    val stillInPosition = readyCheck.check(fixed).isReady

                    if (!stillInPosition) {
                        feedbackText = "Return to push-up position"
                        segments = PoseSkeleton.segments.map { it.copy(color = "#FF0000") }
                        downStreak = 0
                        upStreak = 0
                    } else {
                        val features = PushupFeatureExtractor.extract(fixed)

                        if (features != null) {
                            val minElbow = features[0]

                            // Проверка колени на земле: угол hip→knee→ankle
                            val lHip = fixed[10]; val lKnee = fixed[12]; val lAnk = fixed[14]
                            val rHip = fixed[11]; val rKnee = fixed[13]; val rAnk = fixed[15]
                            fun kneeAngle(h: com.example.fitnesscoachai.ui.workout.shared.PosePoint,
                                          k: com.example.fitnesscoachai.ui.workout.shared.PosePoint,
                                          a: com.example.fitnesscoachai.ui.workout.shared.PosePoint): Float {
                                val bax = h.x - k.x; val bay = h.y - k.y
                                val bcx = a.x - k.x; val bcy = a.y - k.y
                                val mag = kotlin.math.sqrt((bax*bax+bay*bay)*(bcx*bcx+bcy*bcy))
                                if (mag < 1e-6f) return 180f
                                return Math.toDegrees(kotlin.math.acos(((bax*bcx+bay*bcy)/mag).coerceIn(-1f,1f)).toDouble()).toFloat()
                            }
                            val leftKneeAngle = kneeAngle(lHip, lKnee, lAnk)
                            val rightKneeAngle = kneeAngle(rHip, rKnee, rAnk)
                            val kneesOnGround = leftKneeAngle < 150f || rightKneeAngle < 150f

                            if (kneesOnGround) {
                                feedbackText = "Keep knees off the ground"
                                segments = PoseSkeleton.segments.map { it.copy(color = "#FF0000") }
                                downStreak = 0
                                upStreak = 0
                            } else {

                            val prediction = pushupModel.predict(features)

                            if (labelBuffer.size >= LABEL_BUF_SIZE) labelBuffer.removeFirst()
                            labelBuffer.addLast(prediction.label)
                            val smoothedLabel =
                                if (labelBuffer.count { it == "correct" } > labelBuffer.size / 2)
                                    "correct" else "incorrect"

                            feedbackText = smoothedLabel
                            formScore.sample(smoothedLabel)

                            segments = if (smoothedLabel == "correct") {
                                PoseSkeleton.segments.map { it.copy(color = "#00FF00") }
                            } else {
                                PoseSkeleton.dynamic(
                                    leftElbow = features[4],
                                    rightElbow = features[5],
                                    bodyLine = features[2],
                                    hipOffset = features[6]
                                )
                            }

                            when {
                                minElbow < DOWN_T -> {
                                    downStreak++
                                    upStreak = 0
                                }
                                minElbow > UP_T -> {
                                    upStreak++
                                    downStreak = 0
                                }
                                else -> {
                                    downStreak = 0
                                    upStreak = 0
                                }
                            }

                            if (phase == PushupPhase.UP && downStreak >= 3) {
                                phase = PushupPhase.DOWN
                                downStreak = 0
                            }

                            if (phase == PushupPhase.DOWN && upStreak >= 3) {
                                phase = PushupPhase.UP
                                upStreak = 0
                                repCount++
                            }

                            } // end kneesOnGround else

                        } else {
                            feedbackText = "Show full body"
                            segments = emptyList()
                            downStreak = 0
                            upStreak = 0
                        }
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
        pushupModel.close()
    }
}