package com.example.fitnesscoachai.data.models

data class UpdateProfileRequest(
    val username: String? = null,
    val age: Int? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val fitness_level: String? = null,
    val goal: String? = null,
    val limitations: String? = null,
    val frequency: String? = null,
    val workout_duration: String? = null,
    val workout_place: String? = null,
    val endurance_level: String? = null,
    val gender: String? = null
)