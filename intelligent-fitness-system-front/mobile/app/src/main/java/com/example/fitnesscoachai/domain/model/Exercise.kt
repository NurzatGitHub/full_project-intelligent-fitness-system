package com.example.fitnesscoachai.domain.model

/**
 * Упражнение (экран 3). Источник — репозиторий (локальный JSON / API позже).
 */
data class Exercise(
    val id: String,
    val titleEn: String,
    val description: String,
    val steps: List<String>,
    val tips: List<String>,
    val main: MainCategory,
    val sub: SubCategory,
    val equipment: String?,
    val difficulty: String?,
    val media: ExerciseMedia?,
    val videoPath: String?
)

/**
 * Медиа упражнения: локальный ассет или URL (на будущее).
 */
sealed class ExerciseMedia {
    data class LocalAsset(val path: String) : ExerciseMedia()
    data class RemoteUrl(val url: String) : ExerciseMedia()
}
