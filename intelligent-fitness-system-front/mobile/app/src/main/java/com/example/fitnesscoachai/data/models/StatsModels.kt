package com.example.fitnesscoachai.data.models

/**
 * GET /api/workouts/stats/
 *
 * Mirrors what the backend `workout_stats` endpoint returns: a snapshot of
 * per-user totals plus the series we need to render the form-score bar
 * chart and the time-in-zone distribution.
 *
 * All chart fields are optional / nullable — older builds of the backend
 * may not return them, and the UI should fall back gracefully when they
 * aren't present.
 */
data class WorkoutStatsResponse(
    val total_workouts: Int = 0,
    val total_reps: Int = 0,
    val average_form_score: Float = 0f,
    val current_streak: Int = 0,
    val best_exercise: String = "",
    val last_workout_at: String? = null,
    val updated_at: String? = null,

    val form_score_history: List<FormScorePoint> = emptyList(),
    val zone_distribution: ZoneDistribution = ZoneDistribution(),
)

/** One point on the bar chart. `date` is ISO `YYYY-MM-DD`. */
data class FormScorePoint(
    val date: String? = null,
    val title: String = "",
    val form_score: Float = 0f,
)

/** Count of completed sessions per quality bucket. */
data class ZoneDistribution(
    val excellent: Int = 0,  // >= 85
    val good: Int = 0,       // 70-84
    val average: Int = 0,    // 50-69
    val poor: Int = 0,       // < 50
)
