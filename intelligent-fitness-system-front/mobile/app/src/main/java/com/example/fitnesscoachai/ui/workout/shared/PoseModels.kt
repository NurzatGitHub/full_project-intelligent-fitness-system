package com.example.fitnesscoachai.ui.workout.shared

/**
 * Точка позы (нормализованные координаты MediaPipe)
 * x,y — от 0 до 1
 * v — visibility
 */
data class PosePoint(
    val x: Float,
    val y: Float,
    val v: Float
)

/**
 * Сегмент скелета для рисования линий
 */
data class Segment(
    val a: Int,
    val b: Int,
    val color: String
)