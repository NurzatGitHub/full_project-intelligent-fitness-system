package com.example.fitnesscoachai.ui.workout.squat

object SquatFeedback {

    fun build(
        predictionLabel: String,
        confidence: Float?,
        kneeCaveRatio: Float,
        depthRatio: Float,
        trunkAngle: Float
    ): String {
        val label = predictionLabel.lowercase()

        if (label == "correct") {
            return when {
                depthRatio >= 0.82f -> "Go lower"
                trunkAngle > 35f -> "Keep your chest up"
                else -> "Good squat"
            }
        }

        val reasons = mutableListOf<String>()

        if (depthRatio >= 0.82f) reasons += "go lower"
        if (kneeCaveRatio >= 2.8f) reasons += "keep your knees out"
        if (trunkAngle > 35f) reasons += "keep your chest up"

        return when {
            reasons.isEmpty() && confidence != null && confidence < 0.75f ->
                "Almost there"
            reasons.isEmpty() ->
                "Fix your squat form"
            reasons.size == 1 ->
                reasons[0].replaceFirstChar { it.uppercase() }
            else ->
                reasons.joinToString(", ").replaceFirstChar { it.uppercase() }
        }
    }

    fun readyHint(isReady: Boolean, hint: String): String {
        if (isReady) return "Ready"
        return if (hint.isBlank()) "Stand up straight to begin" else hint
    }
}