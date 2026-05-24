package com.example.fitnesscoachai.ui.workout.shared

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.min

object PoseMapper {

    // MediaPipe PoseLandmarker indices (33)
    private const val NOSE = 0
    private const val L_EYE_INNER = 1
    private const val R_EYE_INNER = 4

    private const val L_SHOULDER = 11
    private const val R_SHOULDER = 12
    private const val L_ELBOW = 13
    private const val R_ELBOW = 14
    private const val L_WRIST = 15
    private const val R_WRIST = 16

    private const val L_HIP = 23
    private const val R_HIP = 24
    private const val L_KNEE = 25
    private const val R_KNEE = 26
    private const val L_ANKLE = 27
    private const val R_ANKLE = 28

    private const val L_FOOT = 31
    private const val R_FOOT = 32

    // твой формат 0..17:
    // 0 dummy, 1 dummy, 2 dummy, 3 chest(mid shoulders),
    // 4 L_sh,5 R_sh,6 L_el,7 R_el,8 L_wr,9 R_wr,
    // 10 L_hip,11 R_hip,12 L_knee,13 R_knee,14 L_ankle,15 R_ankle,
    // 16 L_foot,17 R_foot
    fun mapTo18(lm: List<NormalizedLandmark>): List<PosePoint> {
        require(lm.size >= 33) { "Expected 33 landmarks, got ${lm.size}" }

        fun vis(i: Int): Float {
            // visibility() у тебя Optional — берём безопасно
            return runCatching { lm[i].visibility().orElse(0.0f) }.getOrDefault(0.0f)
        }

        fun p(i: Int): PosePoint = PosePoint(lm[i].x(), lm[i].y(), vis(i))

        fun mid(a: PosePoint, b: PosePoint): PosePoint =
            PosePoint(
                (a.x + b.x) / 2f,
                (a.y + b.y) / 2f,
                min(a.v, b.v)
            )

        val lSh = p(L_SHOULDER)
        val rSh = p(R_SHOULDER)

        // dummy точки (не используются в backend, просто чтобы было 18)
        val d0 = p(NOSE)
        val d1 = p(L_EYE_INNER)
        val d2 = p(R_EYE_INNER)

        val chest = mid(lSh, rSh)

        return listOf(
            d0, d1, d2, chest,
            lSh, rSh,
            p(L_ELBOW), p(R_ELBOW),
            p(L_WRIST), p(R_WRIST),
            p(L_HIP), p(R_HIP),
            p(L_KNEE), p(R_KNEE),
            p(L_ANKLE), p(R_ANKLE),
            p(L_FOOT), p(R_FOOT),
        )
    }
}