package com.example.fitnesscoachai.data.models

data class WeeklyPlanExerciseItem(
    val id: Int,
    val external_id: String? = null,
    val name: String,
    val slug: String,
    val description: String? = null,
    val target_muscle: String? = null,
    val equipment: String? = null,
    val difficulty: String? = null,
    val asset_image_name: String? = null,
    val asset_video_name: String? = null,
    val default_sets: Int? = null,
    val default_reps: Int? = null,
    val default_duration_min: Int? = null,
    val plan_sets: Int? = null,
    val plan_reps: Int? = null,
    val plan_duration_min: Int? = null,
    val plan_notes: String? = null,
    val sort_order: Int? = null,
    val is_completed: Boolean = false
)

data class WeeklyPlanDay(
    val id: Int,
    val day_key: String,
    val label: String,
    val type: String,
    val title: String,
    val description: String? = null,
    val duration_min: Int,
    val note: String,
    val sort_order: Int? = null,
    val is_completed: Boolean = false,
    val completed_exercise_count: Int = 0,
    val total_exercise_count: Int = 0,
    val exercises: List<WeeklyPlanExerciseItem> = emptyList()
)

data class WeeklyPlanResponse(
    val id: Int? = null,
    val title: String,
    val goal_summary: String,
    val days: List<WeeklyPlanDay>,
    val today_tip: String,
    val generated_at: String? = null,
    val week_start_date: String? = null,
    val is_active: Boolean? = null
)