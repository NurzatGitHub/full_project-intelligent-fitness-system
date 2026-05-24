package com.example.fitnesscoachai.ui.workout.shared

import kotlin.math.sqrt

class PoseStabilizer(
    private val alpha: Float = 0.35f,
    private val jumpThreshold: Float = 0.20f,
    private val minAvgV: Float = 0.10f,
    private val badFramesToReset: Int = 3
) {
    private var lastStable: List<PosePoint>? = null
    private var ema: MutableList<PosePoint>? = null
    private var badCount: Int = 0

    fun reset() {
        lastStable = null
        ema = null
        badCount = 0
    }

    fun apply(points: List<PosePoint>): List<PosePoint>? {
        if (points.size < 18) return null

        val avgV = points.map { it.v }.average().toFloat()
        if (avgV < minAvgV) return null

        // first frame
        if (ema == null) {
            ema = points.map { it.copy() }.toMutableList()
            lastStable = ema!!.toList()
            badCount = 0
            return lastStable
        }

        val prev = lastStable ?: return null

        val meanShift = meanDistance(prev, points)

        // если улёт
        if (meanShift > jumpThreshold) {
            badCount += 1
            // если улётов много подряд — "перезахват" (принимаем новые точки)
            if (badCount >= badFramesToReset) {
                ema = points.map { it.copy() }.toMutableList()
                lastStable = ema!!.toList()
                badCount = 0
                return lastStable
            }
            // иначе просто держим прошлый стабильный
            return prev
        }

        // нормальный кадр -> сбрасываем badCount
        badCount = 0

        // EMA
        val e = ema!!
        for (i in 0 until 18) {
            val p = points[i]
            val old = e[i]
            val nx = old.x + alpha * (p.x - old.x)
            val ny = old.y + alpha * (p.y - old.y)
            val nv = old.v + alpha * (p.v - old.v)
            e[i] = PosePoint(nx, ny, nv)
        }

        lastStable = e.toList()
        return lastStable
    }

    private fun meanDistance(a: List<PosePoint>, b: List<PosePoint>): Float {
        var sum = 0f
        for (i in 0 until 18) {
            val dx = a[i].x - b[i].x
            val dy = a[i].y - b[i].y
            sum += sqrt(dx * dx + dy * dy)
        }
        return sum / 18f
    }
}