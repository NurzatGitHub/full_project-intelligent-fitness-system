package com.example.fitnesscoachai.data.models

/**
 * Response shape of GET /api/assistant/chat/messages/
 */
data class ChatHistoryResponse(
    val messages: List<ChatHistoryItem> = emptyList(),
)

data class ChatHistoryItem(
    val id: Long,
    val role: String,        // "user" or "assistant"
    val content: String,
    val created_at: String,  // ISO-8601 timestamp
)

/**
 * Response shape of GET /api/workouts/history/ (DRF PageNumberPagination).
 */
data class WorkoutHistoryPageResponse(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<WorkoutHistorySession> = emptyList(),
)

data class WorkoutHistorySession(
    val id: Int,
    val title: String = "",
    val started_at: String? = null,
    val finished_at: String? = null,
    val total_duration_sec: Int = 0,
    val total_reps: Int = 0,
    val avg_form_score: Float? = null,
    val calories_burned: Int? = null,
    val status: String = "completed",
    val notes: String = "",
    val created_at: String? = null,
)
