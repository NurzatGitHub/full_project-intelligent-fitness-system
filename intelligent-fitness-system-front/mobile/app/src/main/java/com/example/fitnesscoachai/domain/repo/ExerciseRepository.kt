package com.example.fitnesscoachai.domain.repo

import com.example.fitnesscoachai.domain.model.Exercise
import com.example.fitnesscoachai.domain.model.MainCategory

/**
 * Репозиторий упражнений. Реализацию можно заменить (локальный JSON → API).
 */
interface ExerciseRepository {
    suspend fun getExercisesBySubCategory(subId: String): List<Exercise>
    suspend fun getExercisesByMainCategory(main: MainCategory): List<Exercise>
    suspend fun getExerciseById(id: String): Exercise?
}
