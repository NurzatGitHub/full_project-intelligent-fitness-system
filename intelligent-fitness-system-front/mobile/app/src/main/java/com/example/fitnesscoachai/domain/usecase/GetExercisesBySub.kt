package com.example.fitnesscoachai.domain.usecase

import com.example.fitnesscoachai.domain.model.Exercise
import com.example.fitnesscoachai.domain.repo.ExerciseRepository

/**
 * Use case: список упражнений по подкатегории.
 */
class GetExercisesBySub(private val repo: ExerciseRepository) {
    suspend operator fun invoke(subId: String): List<Exercise> = repo.getExercisesBySubCategory(subId)
}
