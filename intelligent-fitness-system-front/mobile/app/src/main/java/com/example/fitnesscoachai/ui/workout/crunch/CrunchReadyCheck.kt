package com.example.fitnesscoachai.ui.workout.crunch

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.*

class CrunchReadyCheck {

    private val VIS_TH  = 0.15f
    private val MIN_GOOD = 6

    // Обязательные точки для crunch: chest, shoulders, hips, knees
    private val NEEDED_IDS = setOf(3, 4, 5, 10, 11, 12, 13)

    // Геометрические условия для позиции лёжа с согнутыми коленями
    private val MAX_TRUNK_ANGLE     = 140f   // корпус не должен быть полностью вертикален
    private val MAX_BODY_SPAN_X     = 0.55f  // тело не слишком широко (фронтальный вид)
    private val MIN_BODY_SPAN_Y     = 0.20f  // тело занимает достаточно кадра по вертикали
    private val MIN_TORSO_LEN       = 0.08f  // туловище видно
    private val MIN_SHOULDER_WIDTH  = 0.05f  // плечи видны
    private val MAX_KNEE_ANGLE      = 155f   // колени согнуты (не вытянуты прямо)
    private val MIN_SCORE           = 4

    private val smoothBuf = ArrayDeque<List<PosePoint>>(5)

    fun reset() {
        smoothBuf.clear()
    }

    data class ReadyResult(
        val isReady: Boolean,
        val score: Int,
        val hint: String
    )

    fun check(points: List<PosePoint>): ReadyResult {
        if (points.size < 18) return notReady("Show your full body")

        val good = NEEDED_IDS.count { i -> points[i].v >= VIS_TH }
        if (good < MIN_GOOD) return notReady("Show shoulders, hips and knees")

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
        val chest   = p[3]
        val lSh     = p[4]
        val rSh     = p[5]
        val lHip    = p[10]
        val rHip    = p[11]
        val lKnee   = p[12]
        val rKnee   = p[13]
        val lAnk    = p[14]
        val rAnk    = p[15]

        val hipMid  = mid(lHip,  rHip)
        val kneeMid = mid(lKnee, rKnee)
        val shMid   = mid(lSh,   rSh)
        val ankMid  = mid(lAnk,  rAnk)

        val torsoLen       = dist(shMid, hipMid)
        val shoulderWidth  = dist(lSh, rSh)

        // Span всего тела
        val spanY = abs(shMid.y - ankMid.y)
        val spanX = abs(shMid.x - ankMid.x)

        // trunk angle chest-hip-knee
        val trunkAngle = angle3(chest, hipMid, kneeMid)

        // Среднее колено hip-knee-ankle
        val kaL = if (lAnk.v >= 0.15f) angle3(lHip, lKnee, lAnk) else 180f
        val kaR = if (rAnk.v >= 0.15f) angle3(rHip, rKnee, rAnk) else 180f
        val kneeAngleAvg = (kaL + kaR) / 2f

        // Условия готовности к crunch:
        val condTorsoVisible    = torsoLen > MIN_TORSO_LEN && shoulderWidth > MIN_SHOULDER_WIDTH
        val condBodyInFrame     = spanY > MIN_BODY_SPAN_Y
        val condNotTooWide      = spanX < MAX_BODY_SPAN_X
        val condLying           = trunkAngle < MAX_TRUNK_ANGLE   // не стоит вертикально
        val condKneesBent       = kneeAngleAvg < MAX_KNEE_ANGLE  // колени согнуты
        val condHipBelowShoulder = hipMid.y > shMid.y + 0.03f   // бёдра ниже плеч в кадре

        val score = listOf(
            condTorsoVisible,
            condBodyInFrame,
            condNotTooWide,
            condLying,
            condKneesBent,
            condHipBelowShoulder
        ).count { it }

        val isReady = score >= MIN_SCORE

        val hint = when {
            isReady               -> ""
            !condTorsoVisible     -> "Step back — show torso and shoulders"
            !condBodyInFrame      -> "Show your full body in frame"
            !condLying            -> "Lie down on your back"
            !condKneesBent        -> "Bend your knees"
            !condHipBelowShoulder -> "Lie flat — align your body"
            !condNotTooWide       -> "Keep body inside the frame"
            else                  -> "Lie on your back, knees bent"
        }

        return ReadyResult(isReady, score, hint)
    }

    private fun notReady(hint: String) = ReadyResult(false, 0, hint)

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
        val ax = a.x - b.x; val ay = a.y - b.y
        val cx = c.x - b.x; val cy = c.y - b.y
        val dot = ax * cx + ay * cy
        val na  = sqrt(ax * ax + ay * ay)
        val nc  = sqrt(cx * cx + cy * cy)
        if (na == 0f || nc == 0f) return 180f
        return Math.toDegrees(
            acos((dot / (na * nc)).coerceIn(-1f, 1f)).toDouble()
        ).toFloat()
    }
}
