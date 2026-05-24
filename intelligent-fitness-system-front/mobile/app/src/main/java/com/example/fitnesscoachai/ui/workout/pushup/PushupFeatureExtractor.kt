package com.example.fitnesscoachai.ui.workout.pushup

import com.example.fitnesscoachai.ui.workout.shared.*
import kotlin.math.*

object PushupFeatureExtractor {

    private const val EPS = 1e-6f
    private const val MIN_VIS = 0.20f

    private fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x
        val bay = a.y - b.y

        val bcx = c.x - b.x
        val bcy = c.y - b.y

        val magBA = sqrt(bax * bax + bay * bay)
        val magBC = sqrt(bcx * bcx + bcy * bcy)

        if (magBA < EPS || magBC < EPS) return Float.NaN

        val dot = bax * bcx + bay * bcy
        val cos = dot / (magBA * magBC)

        return Math.toDegrees(
            acos(cos.coerceIn(-1f, 1f)).toDouble()
        ).toFloat()
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

        val avgV = points.map { it.v }.average().toFloat()
        if (avgV < MIN_VIS) return null

        val chest = points[3]

        val lSh = points[4]
        val rSh = points[5]

        val lEl = points[6]
        val rEl = points[7]

        val lWr = points[8]
        val rWr = points[9]

        val lHip = points[10]
        val rHip = points[11]

        val lKnee = points[12]
        val rKnee = points[13]

        val leftElbow = angle(lSh, lEl, lWr)
        val rightElbow = angle(rSh, rEl, rWr)

        if (!leftElbow.isFinite() || !rightElbow.isFinite()) return null

        val minElbow = min(leftElbow, rightElbow)
        val diff = abs(leftElbow - rightElbow)

        val shoulderWidth = dist(lSh, rSh)
        if (shoulderWidth < EPS) return null

        val elbowWidth = dist(lEl, rEl)
        val elbowRatio = elbowWidth / shoulderWidth

        val hipMid = mid(lHip, rHip)
        val kneeMid = mid(lKnee, rKnee)

        val bodyLine = angle(chest, hipMid, kneeMid)
        val hipOffset = hipMid.y - ((chest.y + kneeMid.y) / 2f)

        return floatArrayOf(
            minElbow,
            diff,
            bodyLine,
            elbowRatio,
            leftElbow,
            rightElbow,
            hipOffset
        )
    }
}