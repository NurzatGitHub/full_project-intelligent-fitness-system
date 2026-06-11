package com.example.fitnesscoachai.ui.workout.shared

/**
 * Aggregates per-frame AI verdicts during a workout and emits a single
 * percentage score at the end.
 *
 * Usage from a workout Activity:
 *
 *     private val formScore = FormScoreTracker()
 *     // ... inside analyze(): formScore.sample(prediction.label)
 *     // ... on finish(): val score = formScore.percent()  // or -1f if no data
 *
 * The "correct" string matches the AI model's label vocabulary. Anything else
 * (e.g. "wrong_back_angle", "hands_too_low", or an empty label) counts as a
 * miss. We treat the workout as "unscored" when zero samples landed so the
 * Summary screen can show a dash instead of inventing 0%.
 */
class FormScoreTracker {

    private var correct = 0
    private var incorrect = 0

    /** Reset on Start / Restart of a workout. */
    fun reset() {
        correct = 0
        incorrect = 0
    }

    /** Feed one frame's AI verdict in. Safe to call from a non-UI thread. */
    fun sample(label: String?) {
        if (label == "correct") {
            correct++
        } else {
            incorrect++
        }
    }

    /**
     * Returns the workout's form quality as a 0..100 percentage, or -1f if
     * the AI never produced a verdict (frame never made it to the analyzer,
     * user finished before standing in frame, etc).
     */
    fun percent(): Float {
        val total = correct + incorrect
        if (total <= 0) return -1f
        return (correct * 100f / total).coerceIn(0f, 100f)
    }

    val samples: Int get() = correct + incorrect
}
