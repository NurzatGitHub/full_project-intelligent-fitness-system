package com.example.fitnesscoachai.ui.workout.shoulderpress

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.*

object ShoulderPressFeatureExtractor {

    private const val EPS = 1e-6f
    const val MIN_VIS = 0.20f

    // Фаза упражнения
    const val DOWN_TH = 83.8f
    const val UP_TH = 158.8f

    private fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x
        val bay = a.y - b.y
        val bcx = c.x - b.x
        val bcy = c.y - b.y

        val magBA = sqrt(bax * bax + bay * bay)
        val magBC = sqrt(bcx * bcx + bcy * bcy)

        if (magBA < EPS || magBC < EPS) return Float.NaN

        val cos = (bax * bcx + bay * bcy) / (magBA * magBC)
        return Math.toDegrees(acos(cos.coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    private fun dist(a: PosePoint, b: PosePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun mid(a: PosePoint, b: PosePoint): PosePoint {
        return PosePoint(
            (a.x + b.x) / 2f,
            (a.y + b.y) / 2f,
            min(a.v, b.v)
        )
    }

    fun extract(points: List<PosePoint>): FloatArray? {
        if (points.size < 18) return null

        val chest = points[3]
        val lSh = points[4]
        val rSh = points[5]
        val lEl = points[6]
        val rEl = points[7]
        val lWr = points[8]
        val rWr = points[9]
        val lHip = points[10]
        val rHip = points[11]

        val required = listOf(chest, lSh, rSh, lEl, rEl, lWr, rWr, lHip, rHip)
        if (required.any { it.v < MIN_VIS }) return null

        val leftElbow = angle(lSh, lEl, lWr)
        val rightElbow = angle(rSh, rEl, rWr)
        if (!leftElbow.isFinite() || !rightElbow.isFinite()) return null

        val minElbow = min(leftElbow, rightElbow)
        val maxElbow = max(leftElbow, rightElbow)
        val elbowSymmetry = abs(leftElbow - rightElbow)

        val hipMid = mid(lHip, rHip)

        // Наклон торса относительно вертикали вниз
        val verticalBelowChest = PosePoint(chest.x, chest.y + 0.2f, 1f)
        val torsoLean = angle(verticalBelowChest, chest, hipMid).let {
            if (it.isFinite()) it else 0f
        }

        val leftShoulderAbd = angle(lHip, lSh, lEl)
        val rightShoulderAbd = angle(rHip, rSh, rEl)
        if (!leftShoulderAbd.isFinite() || !rightShoulderAbd.isFinite()) return null

        val shoulderSymmetry = abs(leftShoulderAbd - rightShoulderAbd)

        val shoulderWidth = dist(lSh, rSh)
        if (shoulderWidth < EPS) return null

        val wristAlignLeft = (lWr.x - lSh.x) / shoulderWidth
        val wristAlignRight = (rWr.x - rSh.x) / shoulderWidth

        return floatArrayOf(
            minElbow,           // 0
            maxElbow,           // 1
            elbowSymmetry,      // 2
            torsoLean,          // 3
            leftShoulderAbd,    // 4
            rightShoulderAbd,   // 5
            wristAlignLeft,     // 6
            wristAlignRight,    // 7
            shoulderSymmetry    // 8
        )
    }

    fun phaseFromFeatures(features: FloatArray): ShoulderPressPhase {
        return when {
            features[1] >= UP_TH -> ShoulderPressPhase.UP
            features[1] <= DOWN_TH -> ShoulderPressPhase.DOWN
            else -> ShoulderPressPhase.MID
        }
    }
}