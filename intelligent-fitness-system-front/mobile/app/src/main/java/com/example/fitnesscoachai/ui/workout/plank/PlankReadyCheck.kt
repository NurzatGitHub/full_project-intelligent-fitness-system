package com.example.fitnesscoachai.ui.workout.plank

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.*

/**
 * Checks whether the user is in a valid plank position before starting the hold timer.
 * Designed for a side-view camera (or diagonal front) — mirrors PushupReadyCheck style.
 */
class PlankReadyCheck {

    private val VIS_TH  = 0.12f
    private val MIN_GOOD = 7

    // keypoints needed: shoulders, elbows, wrists, hips, knees, ankles
    private val NEEDED_IDS = setOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    // body line (shoulder–hip–knee) must be close to 180° for plank
    private val BODYLINE_MIN = 155f   // stricter than push-up ready check

    private val MIN_TORSO_LEN    = 0.10f
    private val MIN_SHOULDER_WIDTH = 0.06f
    private val MIN_HIP_WIDTH    = 0.05f
    private val MAX_HIP_OFFSET   = 0.12f   // hip must not sag or pike too much

    private val smoothBuf = ArrayDeque<List<PosePoint>>(5)

    fun reset() { smoothBuf.clear() }

    data class ReadyResult(
        val isReady: Boolean,
        val bodyLine: Float,
        val score: Int,
        val hint: String
    )

    fun check(points: List<PosePoint>): ReadyResult {
        if (points.size < 18) return notReady("Show full body")

        val good = NEEDED_IDS.count { i -> points[i].v >= VIS_TH }
        if (good < MIN_GOOD) return notReady("Show shoulders, hips and feet")

        if (smoothBuf.size >= 5) smoothBuf.removeFirst()
        smoothBuf.addLast(points)

        val smoothed: List<PosePoint> = if (smoothBuf.size >= 3) {
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
        val lSh    = p[4];  val rSh    = p[5]
        val lEl    = p[6];  val rEl    = p[7]
        val lWr    = p[8];  val rWr    = p[9]
        val lHip   = p[10]; val rHip   = p[11]
        val lKnee  = p[12]; val rKnee  = p[13]
        val lAnkle = p[14]; val rAnkle = p[15]

        val shMid     = mid(lSh,    rSh)
        val elMid     = mid(lEl,    rEl)
        val wrMid     = mid(lWr,    rWr)
        val hipMid    = mid(lHip,   rHip)
        val kneeMid   = mid(lKnee,  rKnee)
        val ankleMid  = mid(lAnkle, rAnkle)

        val bodyLine = angle3(shMid, hipMid, kneeMid)

        val torsoLen       = dist(shMid, hipMid)
        val shoulderWidth  = dist(lSh, rSh)
        val hipWidth       = dist(lHip, rHip)
        val hipOffset      = hipMid.y - ((shMid.y + kneeMid.y) / 2f)

        // arms should be bearing weight: wrists below shoulders, elbows engaged
        val wristsBelowShoulders = wrMid.y > shMid.y + 0.02f
        val armsEngaged = dist(shMid, wrMid) > 0.08f

        // legs extended: ankle below knee
        val legsExtended = ankleMid.y > kneeMid.y + 0.02f

        val torsoVisible = torsoLen > MIN_TORSO_LEN &&
                shoulderWidth > MIN_SHOULDER_WIDTH &&
                hipWidth > MIN_HIP_WIDTH

        val bodyFlat = bodyLine >= BODYLINE_MIN && abs(hipOffset) < MAX_HIP_OFFSET

        val score = listOf(
            torsoVisible,
            wristsBelowShoulders,
            armsEngaged,
            legsExtended,
            bodyLine >= BODYLINE_MIN,
            abs(hipOffset) < MAX_HIP_OFFSET
        ).count { it }

        val isReady = torsoVisible && wristsBelowShoulders &&
                armsEngaged && legsExtended && bodyFlat && score >= 5

        val hint = when {
            isReady -> ""
            !torsoVisible        -> "Move back — show full body"
            !wristsBelowShoulders -> "Place hands under shoulders"
            !armsEngaged         -> "Extend arms and hold plank"
            !legsExtended        -> "Straighten your legs"
            bodyLine < BODYLINE_MIN -> "Straighten your body line"
            abs(hipOffset) >= MAX_HIP_OFFSET ->
                if (hipOffset > 0) "Drop your hips — don't pike" else "Raise your hips — don't sag"
            else -> "Get into plank position"
        }

        return ReadyResult(isReady, bodyLine, score, hint)
    }

    private fun notReady(hint: String) = ReadyResult(false, 0f, 0, hint)

    private fun mid(a: PosePoint, b: PosePoint) = PosePoint(
        (a.x + b.x) / 2f,
        (a.y + b.y) / 2f,
        min(a.v, b.v)
    )

    private fun dist(a: PosePoint, b: PosePoint): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun angle3(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val ax = a.x - b.x; val ay = a.y - b.y
        val cx = c.x - b.x; val cy = c.y - b.y
        val dot = ax * cx + ay * cy
        val na = sqrt(ax * ax + ay * ay)
        val nc = sqrt(cx * cx + cy * cy)
        if (na == 0f || nc == 0f) return 0f
        return Math.toDegrees(
            acos((dot / (na * nc)).coerceIn(-1f, 1f)).toDouble()
        ).toFloat()
    }
}
