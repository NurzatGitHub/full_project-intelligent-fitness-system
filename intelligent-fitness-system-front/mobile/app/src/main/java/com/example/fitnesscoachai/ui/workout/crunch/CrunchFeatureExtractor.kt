package com.example.fitnesscoachai.ui.workout.crunch

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.*

object CrunchFeatureExtractor {

    private const val EPS     = 1e-6f
    private const val MIN_VIS = 0.15f

    private fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x;  val bay = a.y - b.y
        val bcx = c.x - b.x;  val bcy = c.y - b.y
        val magBA = sqrt(bax * bax + bay * bay)
        val magBC = sqrt(bcx * bcx + bcy * bcy)
        if (magBA < EPS || magBC < EPS) return Float.NaN
        val cos = (bax * bcx + bay * bcy) / (magBA * magBC)
        return Math.toDegrees(acos(cos.coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    private fun dist(a: PosePoint, b: PosePoint): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun mid(a: PosePoint, b: PosePoint) = PosePoint(
        (a.x + b.x) / 2f, (a.y + b.y) / 2f, min(a.v, b.v)
    )

    /**
     * Извлекает 7 признаков для crunch (съёмка сбоку).
     *
     * [0] trunk_angle    — угол shMid-hip-knee
     *                      мал (~≤90°) = плечи подняты = crunch выполнен
     *                      велик (~≥165°) = лежит плоско
     * [1] neck_angle     — угол nose-chest-hip (FIX: нос вместо рта — виден при съёмке сбоку)
     * [2] hip_lift       — (knee_y - hip_y) / torso_len
     * [3] elbow_to_knee  — min(dist(elbow,knee)) / torso_len
     * [4] symmetry_diff  — |L_sh.y - R_sh.y|
     * [5] knee_angle_avg — средний угол hip-knee-ankle
     * [6] shoulder_tilt  — угол линии плеч к горизонтали
     */
    fun extract(points: List<PosePoint>): FloatArray? {
        if (points.size < 18) return null

        val required = listOf(3, 10, 11, 12, 13)
        if (required.any { points[it].v < MIN_VIS }) return null

        // FIX: используем нос (0) вместо рта (2)
        // Нос лучше виден при съёмке сбоку чем рот
        val nose    = points[0]
        val chest   = points[3]
        val lSh     = points[4]
        val rSh     = points[5]
        val lEl     = points[6]
        val rEl     = points[7]
        val lHip    = points[10]
        val rHip    = points[11]
        val lKnee   = points[12]
        val rKnee   = points[13]
        val lAnk    = points[14]
        val rAnk    = points[15]

        val hipMid   = mid(lHip,  rHip)
        val kneeMid  = mid(lKnee, rKnee)
        val shMid    = if (lSh.v >= MIN_VIS && rSh.v >= MIN_VIS) mid(lSh, rSh) else chest

        val torsoLen = dist(shMid, hipMid).coerceAtLeast(EPS)

        // trunk_angle: shMid-hip-knee
        // shMid (середина плеч) — реальное движение при crunch сбоку
        val trunkAngle = angle(shMid, hipMid, kneeMid)
        if (!trunkAngle.isFinite()) return null

        // neck_angle: nose-chest-hip (FIX: нос вместо рта)
        val neckAngle = if (nose.v >= MIN_VIS) {
            val a = angle(nose, chest, hipMid)
            if (a.isFinite()) a else 180f
        } else {
            180f
        }

        // hip_lift
        val hipLift = (kneeMid.y - hipMid.y) / torsoLen

        // elbow_to_knee
        val dL = if (lEl.v >= MIN_VIS) dist(lEl, lKnee) else Float.MAX_VALUE
        val dR = if (rEl.v >= MIN_VIS) dist(rEl, rKnee) else Float.MAX_VALUE
        val minEK = minOf(dL, dR)
        val elbowToKnee = if (minEK == Float.MAX_VALUE) 1.5f else minEK / torsoLen

        // symmetry_diff
        val symmetryDiff = if (lSh.v >= MIN_VIS && rSh.v >= MIN_VIS)
            abs(lSh.y - rSh.y) else 0f

        // knee_angle_avg
        val kaL = if (lAnk.v >= MIN_VIS) { val a = angle(lHip, lKnee, lAnk); if (a.isFinite()) a else 90f } else 90f
        val kaR = if (rAnk.v >= MIN_VIS) { val a = angle(rHip, rKnee, rAnk); if (a.isFinite()) a else 90f } else 90f
        val kneeAngleAvg = (kaL + kaR) / 2f

        // shoulder_tilt
        val shoulderTilt = if (lSh.v >= MIN_VIS && rSh.v >= MIN_VIS) {
            val shDx = abs(rSh.x - lSh.x)
            val shDy = abs(rSh.y - lSh.y)
            Math.toDegrees(atan2(shDy, shDx + EPS).toDouble()).toFloat()
        } else 0f

        return floatArrayOf(
            trunkAngle,    // 0
            neckAngle,     // 1
            hipLift,       // 2
            elbowToKnee,   // 3
            symmetryDiff,  // 4
            kneeAngleAvg,  // 5
            shoulderTilt   // 6
        )
    }
}