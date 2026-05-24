package com.example.fitnesscoachai.data.repo

import com.example.fitnesscoachai.data.models.ExerciseListItemResponse
import com.example.fitnesscoachai.data.models.ExerciseSubcategoryResponse

object ExerciseMemoryCache {
    private val subcategoriesByCategory = mutableMapOf<String, List<ExerciseSubcategoryResponse>>()
    private val exercisesByCategoryAndSub = mutableMapOf<String, List<ExerciseListItemResponse>>()

    fun getSubcategories(categorySlug: String): List<ExerciseSubcategoryResponse>? {
        return subcategoriesByCategory[categorySlug]
    }

    fun putSubcategories(categorySlug: String, items: List<ExerciseSubcategoryResponse>) {
        subcategoriesByCategory[categorySlug] = items
    }

    fun getExercises(categorySlug: String, subcategorySlug: String): List<ExerciseListItemResponse>? {
        return exercisesByCategoryAndSub["$categorySlug::$subcategorySlug"]
    }

    fun putExercises(
        categorySlug: String,
        subcategorySlug: String,
        items: List<ExerciseListItemResponse>
    ) {
        exercisesByCategoryAndSub["$categorySlug::$subcategorySlug"] = items
    }

    fun clearAll() {
        subcategoriesByCategory.clear()
        exercisesByCategoryAndSub.clear()
    }
}