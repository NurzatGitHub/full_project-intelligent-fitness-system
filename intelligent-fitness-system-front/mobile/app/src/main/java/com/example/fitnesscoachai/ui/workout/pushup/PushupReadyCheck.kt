package com.example.fitnesscoachai.ui.workout.pushup

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min

class PushupReadyCheck {

    private val VIS_TH = 0.12f
    private val MIN_GOOD = 7

    // ключевые точки для push-up
    private val NEEDED_IDS = setOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)

    private val BODYLINE_READY_MIN = 150f
    private val MIN_TORSO_LEN = 0.10f
    private val MIN_ARM_LEN = 0.10f
    private val MAX_CHEST_HIP_DY = 0.16f
    private val MAX_HIP_KNEE_DY = 0.18f
    private val MIN_SHOULDER_WIDTH = 0.08f
    private val MIN_HIP_WIDTH = 0.06f

    private val smoothBuf = ArrayDeque<List<PosePoint>>(5)

    fun reset() {
        smoothBuf.clear()
    }

    data class ReadyResult(
        val isReady: Boolean,
        val bodyLine: Float,
        val score: Int,
        val hint: String
    )

    fun check(points: List<PosePoint>): ReadyResult {
        if (points.size < 18) return notReady("Show full body")

        val good = NEEDED_IDS.count { i -> points[i].v >= VIS_TH }
        if (good < MIN_GOOD) return notReady("Show arms, shoulders and hips")

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
        val chest = p[3]
        val lSh = p[4]
        val rSh = p[5]
        val lEl = p[6]
        val rEl = p[7]
        val lWr = p[8]
        val rWr = p[9]
        val lHip = p[10]
        val rHip = p[11]
        val lKnee = p[12]
        val rKnee = p[13]

        val hipMid = mid(lHip, rHip)
        val kneeMid = mid(lKnee, rKnee)
        val shMid = mid(lSh, rSh)
        val elMid = mid(lEl, rEl)
        val wristMid = mid(lWr, rWr)

        val bodyLine = angle3(chest, hipMid, kneeMid)

        val shoulderWidth = dist(lSh, rSh)
        val hipWidth = dist(lHip, rHip)
        val torsoLen = dist(chest, hipMid)
        val armLen = (dist(lSh, lWr) + dist(rSh, rWr)) / 2f

        val chestHipDy = abs(chest.y - hipMid.y)
        val hipKneeDy = abs(hipMid.y - kneeMid.y)

        // кисти должны быть ниже плеч в кадре
        val wristsBelowShoulders = wristMid.y > shMid.y + 0.02f

        // локти/кисти должны быть вынесены вперёд относительно корпуса
        val armsEngaged =
            wristMid.y > elMid.y && armLen > MIN_ARM_LEN

        // корпус виден и не схлопнулся
        val torsoVisible =
            torsoLen > MIN_TORSO_LEN &&
                    shoulderWidth > MIN_SHOULDER_WIDTH &&
                    hipWidth > MIN_HIP_WIDTH

        // корпус примерно прямой
        val torsoFlat =
            chestHipDy < MAX_CHEST_HIP_DY &&
                    hipKneeDy < MAX_HIP_KNEE_DY &&
                    bodyLine >= BODYLINE_READY_MIN

        // принимаем и боковой, и диагональный фронтальный plank
        val diagonalFrontPlank =
            torsoVisible &&
                    wristsBelowShoulders &&
                    armsEngaged &&
                    torsoFlat

        val score = listOf(
            torsoVisible,
            wristsBelowShoulders,
            armsEngaged,
            chestHipDy < MAX_CHEST_HIP_DY,
            hipKneeDy < MAX_HIP_KNEE_DY,
            bodyLine >= BODYLINE_READY_MIN
        ).count { it }

        val isReady = diagonalFrontPlank && score >= 5

        val hint = when {
            isReady -> ""
            !torsoVisible -> "Move back a little — show torso and hips"
            !wristsBelowShoulders -> "Place hands under shoulders"
            !armsEngaged -> "Straighten arms and lock plank"
            bodyLine < BODYLINE_READY_MIN -> "Straighten your body"
            chestHipDy >= MAX_CHEST_HIP_DY -> "Keep hips level with shoulders"
            hipKneeDy >= MAX_HIP_KNEE_DY -> "Keep legs and hips aligned"
            else -> "Get into push-up position"
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
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun angle3(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val ax = a.x - b.x
        val ay = a.y - b.y
        val cx = c.x - b.x
        val cy = c.y - b.y
        val dot = ax * cx + ay * cy
        val na = sqrt(ax * ax + ay * ay)
        val nc = sqrt(cx * cx + cy * cy)
        if (na == 0f || nc == 0f) return 0f
        return Math.toDegrees(
            acos((dot / (na * nc)).coerceIn(-1f, 1f)).toDouble()
        ).toFloat()
    }
}