package com.example.fitnesscoachai.data.models

data class User(
    val id: Int,
    val email: String,
    val username: String,
    val age: Int? = null,
    val weight: Float? = null,
    val height: Float? = null,
    val fitness_level: String = "beginner",
    val goal: String = "",
    val limitations: String = "",
    val frequency: String = "",
    val workout_duration: String = "",
    val workout_place: String = "",
    val endurance_level: String = "",
    val gender: String = "",
    val profile_picture_url: String? = null,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val age: Int?,
    val height: Float?,
    val weight: Float?,
    val fitness_level: String,
    val goal: String,
    val limitations: String,
    val frequency: String,
    val workout_duration: String? = null,
    val workout_place: String? = null,
    val endurance_level: String? = null,
    val gender: String? = null
)

data class AuthResponse(
    val user: User,
    val refresh: String,
    val access: String,
    val is_new_user: Boolean = false
)

data class GoogleLoginRequest(val id_token: String)