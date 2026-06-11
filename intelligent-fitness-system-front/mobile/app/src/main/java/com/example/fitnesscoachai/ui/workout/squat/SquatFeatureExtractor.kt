package com.example.fitnesscoachai.ui.workout.squat

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.*

object SquatFeatureExtractor {

    private const val EPS = 1e-6f
    private const val MIN_VIS = 0.35f

    private const val VALID_KNEE_MIN = 20f
    private const val VALID_KNEE_MAX = 180f
    private const val VALID_TRUNK_MIN = 0.5f
    private const val VALID_TRUNK_MAX = 60f  // глубокий присед даёт наклон до 50-55°

    private fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x
        val bay = a.y - b.y
        val bcx = c.x - b.x
        val bcy = c.y - b.y

        val mag = sqrt(bax * bax + bay * bay) * sqrt(bcx * bcx + bcy * bcy)
        if (mag < EPS) return Float.NaN

        val cos = ((bax * bcx + bay * bcy) / mag).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }

    private fun dist(a: PosePoint, b: PosePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun midX(a: PosePoint, b: PosePoint) = (a.x + b.x) / 2f
    private fun midY(a: PosePoint, b: PosePoint) = (a.y + b.y) / 2f

    fun extract(points: List<PosePoint>): FloatArray? {
        if (points.size < 18) return null

        val needed = listOf(3, 4, 5, 10, 11, 12, 13, 14, 15)
        if (needed.any { points[it].v < MIN_VIS }) return null

        val chest = points[3]
        val lSh = points[4]
        val rSh = points[5]
        val lHip = points[10]
        val rHip = points[11]
        val lKnee = points[12]
        val rKnee = points[13]
        val lAnk = points[14]
        val rAnk = points[15]

        val leftKneeAngle = angle(lHip, lKnee, lAnk)
        val rightKneeAngle = angle(rHip, rKnee, rAnk)
        if (!leftKneeAngle.isFinite() || !rightKneeAngle.isFinite()) return null

        val minKneeAngle = min(leftKneeAngle, rightKneeAngle)
        val kneeDiff = abs(leftKneeAngle - rightKneeAngle)

        if (minKneeAngle < VALID_KNEE_MIN || minKneeAngle > VALID_KNEE_MAX) return null

        val leftHipAngle = angle(lSh, lHip, lKnee)
        val rightHipAngle = angle(rSh, rHip, rKnee)
        if (!leftHipAngle.isFinite() || !rightHipAngle.isFinite()) return null

        val minHipAngle = min(leftHipAngle, rightHipAngle)

        val hipW = dist(lHip, rHip)
        val kneeW = dist(lKnee, rKnee)
        val kneeCaveRatio = kneeW / (hipW + EPS)

        val hipMidY = midY(lHip, rHip)
        val ankMidY = midY(lAnk, rAnk)
        val depthRatio = hipMidY / (ankMidY + EPS)

        // FIX: как в Python — pelvis -> chest берём через pelvis - chest
        val pelvisX = midX(lHip, rHip)
        val pelvisY = midY(lHip, rHip)
        val torsoX = pelvisX - chest.x
        val torsoY = pelvisY - chest.y

        val torsoMag = sqrt(torsoX * torsoX + torsoY * torsoY)
        if (torsoMag < EPS) return null

        val trunkAngle = Math.toDegrees(
            acos((torsoY / torsoMag).coerceIn(-1f, 1f)).toDouble()
        ).toFloat()

        if (!trunkAngle.isFinite()) return null
        if (trunkAngle < VALID_TRUNK_MIN || trunkAngle > VALID_TRUNK_MAX) return null

        return floatArrayOf(
            minKneeAngle,
            leftKneeAngle,
            rightKneeAngle,
            kneeDiff,
            minHipAngle,
            kneeCaveRatio,
            depthRatio,
            trunkAngle
        )
    }
}