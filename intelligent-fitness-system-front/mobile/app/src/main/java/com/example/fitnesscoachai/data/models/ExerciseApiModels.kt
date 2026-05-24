package com.example.fitnesscoachai.data.models

data class ExerciseCategoryResponse(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String = "",
    val image_url: String? = null,
    val is_active: Boolean = true,
    val sort_order: Int = 0,
    val subcategories_count: Int = 0,
    val exercises_count: Int = 0
)

data class ExerciseSubcategoryResponse(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String = "",
    val is_active: Boolean = true,
    val sort_order: Int = 0,
    val category: Int,
    val category_name: String,
    val category_slug: String
)

data class ExerciseListItemResponse(
    val id: Int,
    val external_id: String,
    val name: String,
    val slug: String,
    val description: String = "",
    val target_muscle: String = "",
    val equipment: String = "",
    val difficulty: String = "beginner",
    val default_sets: Int? = null,
    val default_reps: Int? = null,
    val default_duration_min: Int? = null,
    val image_url: String? = null,
    val asset_image_name: String = "",
    val asset_video_name: String = "",
    val category: Int,
    val category_name: String,
    val category_slug: String,
    val subcategory: Int? = null,
    val subcategory_name: String? = null,
    val subcategory_slug: String? = null
)

data class ExerciseDetailResponse(
    val id: Int,
    val external_id: String,
    val name: String,
    val slug: String,
    val description: String = "",
    val target_muscle: String = "",
    val equipment: String = "",
    val difficulty: String = "beginner",
    val instructions: String = "",
    val tips: String = "",
    val steps: List<String> = emptyList(),
    val tips_list: List<String> = emptyList(),
    val video_url: String? = null,
    val image_url: String? = null,
    val asset_image_name: String = "",
    val asset_video_name: String = "",
    val default_sets: Int? = null,
    val default_reps: Int? = null,
    val default_duration_min: Int? = null,
    val category: Int,
    val category_name: String,
    val category_slug: String,
    val subcategory: Int? = null,
    val subcategory_name: String? = null,
    val subcategory_slug: String? = null,
    val is_active: Boolean = true,
    val sort_order: Int = 0
)