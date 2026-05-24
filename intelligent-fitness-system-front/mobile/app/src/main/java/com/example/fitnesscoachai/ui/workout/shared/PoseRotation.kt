package com.example.fitnesscoachai.ui.workout.shared

object PoseRotation {

    // rotationDegrees: 0 / 90 / 180 / 270 (как CameraX rotationDegrees)
    fun rotate(points: List<PosePoint>, rotationDegrees: Int): List<PosePoint> {
        val rot = ((rotationDegrees % 360) + 360) % 360
        return points.map { p ->
            val (x, y) = when (rot) {
                0 -> p.x to p.y
                90 -> (1f - p.y) to p.x      // ✅ CW 90
                180 -> (1f - p.x) to (1f - p.y)
                270 -> p.y to (1f - p.x)     // ✅ CW 270
                else -> p.x to p.y
            }
            PosePoint(x, y, p.v)
        }
    }
}