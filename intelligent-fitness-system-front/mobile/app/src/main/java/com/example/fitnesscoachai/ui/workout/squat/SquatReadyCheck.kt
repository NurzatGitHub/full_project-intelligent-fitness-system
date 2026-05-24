package com.example.fitnesscoachai.ui.workout.squat

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.abs

class SquatReadyCheck {

    private val visThreshold = 0.15f
    private val minGoodPoints = 6

    private val neededIds = setOf(3, 4, 5, 10, 11, 12, 13, 14, 15)

    private val minBodySpanY = 0.35f
    private val maxBodySpanX = 0.50f
    private val hipBelowShoulderY = 0.05f
    private val kneeBelowHipY = 0.05f
    private val minScore = 4

    private val smoothBuffer = ArrayDeque<List<PosePoint>>(5)

    data class ReadyResult(
        val isReady: Boolean,
        val score: Int,
        val hint: String
    )

    fun reset() {
        smoothBuffer.clear()
    }

    fun check(points: List<PosePoint>): ReadyResult {
        if (points.size < 18) return notReady("Show your full body")

        val good = neededIds.count { index -> points[index].v >= visThreshold }
        if (good < minGoodPoints) {
            return notReady("Show your legs and torso fully")
        }

        if (smoothBuffer.size >= 5) smoothBuffer.removeFirst()
        smoothBuffer.addLast(points)

        val smoothed = if (smoothBuffer.size >= 3) {
            List(18) { idx ->
                PosePoint(
                    smoothBuffer.map { it[idx].x }.average().toFloat(),
                    smoothBuffer.map { it[idx].y }.average().toFloat(),
                    smoothBuffer.map { it[idx].v }.average().toFloat()
                )
            }
        } else {
            points
        }

        return geometryCheck(smoothed)
    }

    private fun geometryCheck(p: List<PosePoint>): ReadyResult {
        val lSh = p[4]
        val rSh = p[5]
        val lHip = p[10]
        val rHip = p[11]
        val lKnee = p[12]
        val rKnee = p[13]
        val lAnk = p[14]
        val rAnk = p[15]

        val shMidY = (lSh.y + rSh.y) / 2f
        val hipMidY = (lHip.y + rHip.y) / 2f
        val kneeMidY = (lKnee.y + rKnee.y) / 2f
        val ankMidY = (lAnk.y + rAnk.y) / 2f

        val shMidX = (lSh.x + rSh.x) / 2f
        val ankMidX = (lAnk.x + rAnk.x) / 2f

        val spanY = abs(shMidY - ankMidY)
        val spanX = abs(shMidX - ankMidX)

        val condVertical = spanY > spanX
        val condTallEnough = spanY > minBodySpanY
        val condNotTooWide = spanX < maxBodySpanX
        val condHipBelowShoulders = hipMidY > shMidY + hipBelowShoulderY
        val condKneeBelowHip = kneeMidY > hipMidY + kneeBelowHipY

        val score = listOf(
            condVertical,
            condTallEnough,
            condNotTooWide,
            condHipBelowShoulders,
            condKneeBelowHip
        ).count { it }

        val isReady = score >= minScore

        val hint = when {
            isReady -> ""
            !condTallEnough -> "Step back so your full body is visible"
            !condVertical -> "Stand upright"
            !condNotTooWide -> "Keep your whole body inside the frame"
            !condHipBelowShoulders -> "Stand upright"
            !condKneeBelowHip -> "Show your legs fully"
            else -> "Get into the start position"
        }

        return ReadyResult(isReady, score, hint)
    }

    private fun notReady(hint: String) = ReadyResult(
        isReady = false,
        score = 0,
        hint = hint
    )
}