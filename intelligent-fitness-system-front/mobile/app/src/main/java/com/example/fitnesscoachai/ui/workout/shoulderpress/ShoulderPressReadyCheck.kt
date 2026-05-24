package com.example.fitnesscoachai.ui.workout.shoulderpress

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.*

class ShoulderPressReadyCheck {

    private val VIS_TH = 0.15f
    private val MIN_GOOD = 7

    private val NEEDED_IDS = setOf(3, 4, 5, 6, 7, 8, 9, 10, 11)

    private val MIN_SHOULDER_WIDTH = 0.06f
    private val MIN_TORSO_LEN = 0.08f
    private val ELBOW_READY_MAX = ShoulderPressFeatureExtractor.DOWN_TH
    private val WRIST_HEIGHT_TOL = 0.18f
    private val MAX_TORSO_TILT_X = 0.16f

    private val smoothBuf = ArrayDeque<List<PosePoint>>(5)

    fun reset() {
        smoothBuf.clear()
    }

    data class ReadyResult(
        val isReady: Boolean,
        val maxElbow: Float,
        val score: Int,
        val hint: String
    )

    fun check(points: List<PosePoint>): ReadyResult {
        if (points.size < 18) return notReady("Show full body")

        val good = NEEDED_IDS.count { i -> points[i].v >= VIS_TH }
        if (good < MIN_GOOD) return notReady("Show shoulders, elbows and hips")

        if (smoothBuf.size >= 5) smoothBuf.removeFirst()
        smoothBuf.addLast(points)

        val smoothed = if (smoothBuf.size >= 3) {
            List(18) { idx ->
                PosePoint(
                    smoothBuf.map { it[idx].x }.average().toFloat(),
                    smoothBuf.map { it[idx].y }.average().toFloat(),
                    smoothBuf.map { it[idx].v }.average().toFloat()
                )
            }
        } else {
            points
        }

        return geometryCheck(smoothed)
    }

    private fun geometryCheck(p: List<PosePoint>): ReadyResult {
        val chest = p[3]
        val lSh = p[4]
        val rSh = p[5]
        val lEl = p[6]
        val rEl = p[7]
        val lWr = p[8]
        val rWr = p[9]
        val lHip = p[10]
        val rHip = p[11]

        val hipMid = mid(lHip, rHip)
        val shMid = mid(lSh, rSh)
        val wristMid = mid(lWr, rWr)

        val shoulderWidth = dist(lSh, rSh)
        val torsoLen = dist(chest, hipMid)

        val leftElbow = angle3(lSh, lEl, lWr)
        val rightElbow = angle3(rSh, rEl, rWr)
        val maxElbow = max(leftElbow, rightElbow)

        val bodyVisible = shoulderWidth > MIN_SHOULDER_WIDTH && torsoLen > MIN_TORSO_LEN

        // Разрешаем сидячий press, но корпус должен быть ровный
        val torsoUpright =
            abs(shMid.x - hipMid.x) < MAX_TORSO_TILT_X &&
                    shMid.y < hipMid.y - 0.03f

        val wristsAtShoulderHeight =
            abs(wristMid.y - shMid.y) < WRIST_HEIGHT_TOL

        val elbowsBent =
            maxElbow <= ELBOW_READY_MAX

        val score = listOf(
            bodyVisible,
            torsoUpright,
            wristsAtShoulderHeight,
            elbowsBent,
            shoulderWidth > MIN_SHOULDER_WIDTH
        ).count { it }

        val isReady =
            bodyVisible &&
                    torsoUpright &&
                    wristsAtShoulderHeight &&
                    elbowsBent &&
                    score >= 4

        val hint = when {
            isReady -> ""
            !bodyVisible -> "Show full upper body"
            !torsoUpright -> "Sit upright"
            !elbowsBent -> "Bend elbows to start position"
            !wristsAtShoulderHeight -> "Keep hands near shoulder level"
            else -> "Get into shoulder press position"
        }

        return ReadyResult(isReady, maxElbow, score, hint)
    }

    private fun notReady(hint: String) = ReadyResult(false, 0f, 0, hint)

    private fun mid(a: PosePoint, b: PosePoint): PosePoint {
        return PosePoint(
            (a.x + b.x) / 2f,
            (a.y + b.y) / 2f,
            min(a.v, b.v)
        )
    }

    private fun dist(a: PosePoint, b: PosePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun angle3(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val ax = a.x - b.x
        val ay = a.y - b.y
        val cx = c.x - b.x
        val cy = c.y - b.y

        val na = sqrt(ax * ax + ay * ay)
        val nc = sqrt(cx * cx + cy * cy)

        if (na == 0f || nc == 0f) return 0f

        return Math.toDegrees(
            acos(((ax * cx + ay * cy) / (na * nc)).coerceIn(-1f, 1f)).toDouble()
        ).toFloat()
    }
}