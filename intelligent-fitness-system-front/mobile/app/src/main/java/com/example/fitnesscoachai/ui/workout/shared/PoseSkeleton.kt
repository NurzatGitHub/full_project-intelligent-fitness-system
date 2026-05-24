package com.example.fitnesscoachai.ui.workout.shared

import kotlin.math.abs

/**
 * Динамический скелет — цвет сегментов зависит от качества позы.
 * Логика 1:1 из backend consumers.py build_segments()
 */
object PoseSkeleton {

    // Пороги из consumers.py
    private const val BODYLINE_GOOD    = 165f   // тело достаточно прямое
    private const val ELBOW_GOOD_MIN   = 70f    // локоть не слишком согнут
    private const val HIP_OFFSET_GOOD  = 0.06f  // бёдра не провисают/не задраны

    private const val GREEN = "#00FF00"
    private const val RED   = "#FF0000"

    /** Статические сегменты (когда нет данных о позе — всё зелёное) */
    val segments: List<Segment> = buildStatic(GREEN)

    /**
     * Динамические сегменты с цветом по качеству.
     * Вызывается каждый кадр после вычисления фич.
     *
     * @param leftElbow   — угол левого локтя (features[4])
     * @param rightElbow  — угол правого локтя (features[5])
     * @param bodyLine    — угол chest→hip→knee (features[2])
     * @param hipOffset   — смещение бедра (features[6])
     */
    fun dynamic(
        leftElbow: Float,
        rightElbow: Float,
        bodyLine: Float,
        hipOffset: Float
    ): List<Segment> {
        val goodBody  = bodyLine >= BODYLINE_GOOD && abs(hipOffset) <= HIP_OFFSET_GOOD
        val goodLeft  = leftElbow  >= ELBOW_GOOD_MIN
        val goodRight = rightElbow >= ELBOW_GOOD_MIN

        val armLeft  = if (goodLeft)  GREEN else RED
        val armRight = if (goodRight) GREEN else RED
        val body     = if (goodBody)  GREEN else RED

        return listOf(
            // Левая рука
            Segment(4,  6,  armLeft),
            Segment(6,  8,  armLeft),
            // Правая рука
            Segment(5,  7,  armRight),
            Segment(7,  9,  armRight),
            // Торс (box: плечи + бёдра + вертикали)
            Segment(4,  5,  body),
            Segment(10, 11, body),
            Segment(4,  10, body),
            Segment(5,  11, body),
            // Ноги
            Segment(10, 12, body),
            Segment(11, 13, body),
            Segment(12, 14, body),
            Segment(13, 15, body),
        )
    }

    private fun buildStatic(color: String) = listOf(
        Segment(4,  6,  color), Segment(6,  8,  color),
        Segment(5,  7,  color), Segment(7,  9,  color),
        Segment(4,  5,  color), Segment(10, 11, color),
        Segment(4,  10, color), Segment(5,  11, color),
        Segment(10, 12, color), Segment(11, 13, color),
        Segment(12, 14, color), Segment(13, 15, color),
    )
}