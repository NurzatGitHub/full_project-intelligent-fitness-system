package com.example.fitnesscoachai.ui.workout.plank

import com.example.fitnesscoachai.ui.workout.shared.*
import kotlin.math.*

/**
 * Извлекает 7 признаков для ONNX-модели planк.
 *
 * ⚠️ ВАЖНО: признаки должны ТОЧНО совпадать с тем что ожидает модель plank_model_ir9.onnx
 * Модель обучена на признаках из feature_utils_plank.py (Python):
 *
 *   [0] body_line_angle         — angle(shMid → hipMid → ankleMid)      норма ~170-180°
 *   [1] hip_offset              — point_to_line_offset(hipMid, shMid, ankleMid)
 *                                  >0 = таз высоко (Too-high), <0 = таз низко (Too-low)
 *   [2] shoulder_hip_knee_angle — angle(shMid → hipMid → kneeMid)
 *   [3] hip_knee_ankle_angle    — angle(hipMid → kneeMid → ankleMid)    норма ~170-180°
 *   [4] trunk_horizontal_angle  — угол туловища от горизонтали           норма ~0-15°
 *   [5] elbow_shoulder_hip_angle— angle(elMid → shMid → hipMid)
 *   [6] head_spine_offset       — point_to_line_offset(nose, shMid, ankleMid)
 *
 * PoseMapper.mapTo18 индексы:
 *   0=nose  1=neck  2=rEye  3=lEye
 *   4=lSh   5=rSh   6=lEl   7=rEl   8=lWr  9=rWr
 *   10=lHip 11=rHip 12=lKnee 13=rKnee 14=lAnkle 15=rAnkle
 *   16=lEar 17=rEar
 */
object PlankFeatureExtractor {

    private const val EPS     = 1e-6f
    private const val MIN_VIS = 0.15f   // снижено для бокового вида

    // Признак [4]: планка горизонтальна → trunk_horizontal_angle < MAX_TRUNK_ANGLE
    // Если больше — тело вертикальное (стоит/сидит), не передаём в модель
    private const val MAX_TRUNK_ANGLE = 35f

    // ── Геометрия ─────────────────────────────────────────────

    private fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x; val bay = a.y - b.y
        val bcx = c.x - b.x; val bcy = c.y - b.y
        val magBA = sqrt(bax * bax + bay * bay)
        val magBC = sqrt(bcx * bcx + bcy * bcy)
        if (magBA < EPS || magBC < EPS) return Float.NaN
        val cos = (bax * bcx + bay * bcy) / (magBA * magBC)
        return Math.toDegrees(acos(cos.coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    private fun mid(a: PosePoint, b: PosePoint) = PosePoint(
        (a.x + b.x) / 2f,
        (a.y + b.y) / 2f,
        min(a.v, b.v)
    )

    /**
     * Знаковое смещение точки P от прямой A→B.
     * Совпадает с Python point_to_line_offset().
     */
    private fun pointToLineOffset(p: PosePoint, a: PosePoint, b: PosePoint): Float {
        val abx = b.x - a.x; val aby = b.y - a.y
        val abLen = sqrt(abx * abx + aby * aby) + EPS
        val abNx = abx / abLen; val abNy = aby / abLen
        // перпендикуляр: (-abNy, abNx)
        val perpX = -abNy; val perpY = abNx
        return (p.x - a.x) * perpX + (p.y - a.y) * perpY
    }

    /**
     * Угол туловища от горизонтали (0° = горизонталь, 90° = вертикаль).
     * Совпадает с Python trunk_horizontal_angle.
     */
    private fun trunkHorizontalAngle(sh: PosePoint, ank: PosePoint): Float {
        val dx = ank.x - sh.x
        val dy = ank.y - sh.y
        val len = sqrt(dx * dx + dy * dy) + EPS
        // cos угла с горизонталью = |dx| / len
        val cosA = (abs(dx) / len).coerceIn(0f, 1f)
        return Math.toDegrees(acos(cosA).toDouble()).toFloat()
    }

    // ── Публичный метод ───────────────────────────────────────

    fun extract(points: List<PosePoint>): FloatArray? {
        if (points.size < 18) return null

        val avgV = points.map { it.v }.average().toFloat()
        if (avgV < MIN_VIS) return null

        val nose   = points[0]
        val lSh    = points[4];  val rSh    = points[5]
        val lEl    = points[6];  val rEl    = points[7]
        val lHip   = points[10]; val rHip   = points[11]
        val lKnee  = points[12]; val rKnee  = points[13]
        val lAnkle = points[14]; val rAnkle = points[15]

        val shMid    = mid(lSh,    rSh)
        val elMid    = mid(lEl,    rEl)
        val hipMid   = mid(lHip,   rHip)
        val kneeMid  = mid(lKnee,  rKnee)
        val ankleMid = mid(lAnkle, rAnkle)

        // [4] trunk_horizontal_angle — вычисляем первым для ранней проверки
        val trunkAngle = trunkHorizontalAngle(shMid, ankleMid)

        // Защита: если тело вертикальное — это не планка, не передаём в модель
        if (trunkAngle > MAX_TRUNK_ANGLE) return null

        // [0] body_line_angle: shMid → hipMid → ankleMid
        val bodyLineAngle = angle(shMid, hipMid, ankleMid)
        if (!bodyLineAngle.isFinite()) return null

        // [1] hip_offset: смещение бедра от прямой плечо-лодыжка
        val hipOffset = pointToLineOffset(hipMid, shMid, ankleMid)

        // [2] shoulder_hip_knee_angle: shMid → hipMid → kneeMid
        val shHipKnee = angle(shMid, hipMid, kneeMid)
        if (!shHipKnee.isFinite()) return null

        // [3] hip_knee_ankle_angle: hipMid → kneeMid → ankleMid
        val hipKneeAnkle = angle(hipMid, kneeMid, ankleMid)
        if (!hipKneeAnkle.isFinite()) return null

        // [5] elbow_shoulder_hip_angle: elMid → shMid → hipMid
        val elShHip = angle(elMid, shMid, hipMid)
        if (!elShHip.isFinite()) return null

        // [6] head_spine_offset: смещение носа от линии тела
        val headOffset = if (nose.v >= MIN_VIS)
            pointToLineOffset(nose, shMid, ankleMid)
        else
            0f

        return floatArrayOf(
            bodyLineAngle,   // [0]
            hipOffset,       // [1]
            shHipKnee,       // [2]
            hipKneeAnkle,    // [3]
            trunkAngle,      // [4]
            elShHip,         // [5]
            headOffset       // [6]
        )
    }
}