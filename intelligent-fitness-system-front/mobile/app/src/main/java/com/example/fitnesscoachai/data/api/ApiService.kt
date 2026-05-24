package com.example.fitnesscoachai.data.api

import com.example.fitnesscoachai.data.models.AuthResponse
import com.example.fitnesscoachai.data.models.ChatHistoryResponse
import com.example.fitnesscoachai.data.models.ChatRequest
import com.example.fitnesscoachai.data.models.ChatResponse
import com.example.fitnesscoachai.data.models.ExerciseCategoryResponse
import com.example.fitnesscoachai.data.models.ExerciseDetailResponse
import com.example.fitnesscoachai.data.models.ExerciseListItemResponse
import com.example.fitnesscoachai.data.models.ExerciseSubcategoryResponse
import com.example.fitnesscoachai.data.models.GoogleLoginRequest
import com.example.fitnesscoachai.data.models.LoginRequest
import com.example.fitnesscoachai.data.models.RegisterRequest
import com.example.fitnesscoachai.data.models.UpdateProfileRequest
import com.example.fitnesscoachai.data.models.User
import com.example.fitnesscoachai.data.models.WeeklyPlanResponse
import com.example.fitnesscoachai.data.models.WorkoutHistoryPageResponse
import com.example.fitnesscoachai.data.models.WorkoutSessionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * NOTE: Authorization headers are injected automatically by AuthInterceptor
 * (see RetrofitClient). Call sites should NOT pass a bearer manually.
 *
 * A few legacy overloads still accept `bearer:` for backwards-compatibility
 * with screens we haven't migrated yet — they take priority over the
 * interceptor if set.
 */
interface ApiService {

    // ===================== Auth =====================

    @POST("api/users/login/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/users/register/")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/users/google/")
    suspend fun google(@Body request: GoogleLoginRequest): Response<AuthResponse>

    // ===================== Profile =====================

    @GET("api/users/me/")
    suspend fun getMe(): Response<User>

    // Legacy: still accepts bearer for older callers.
    @GET("api/users/me/")
    suspend fun getMe(@Header("Authorization") bearer: String): Response<User>

    @PATCH("api/users/me/")
    suspend fun updateMe(@Body body: UpdateProfileRequest): Response<User>

    @PATCH("api/users/me/")
    suspend fun updateMe(
        @Header("Authorization") bearer: String,
        @Body body: UpdateProfileRequest,
    ): Response<User>

    // ===================== Assistant =====================

    @POST("api/assistant/chat/")
    suspend fun assistantChat(@Body body: ChatRequest): Response<ChatResponse>

    @POST("api/assistant/chat/")
    suspend fun assistantChat(
        @Header("Authorization") bearer: String,
        @Body body: ChatRequest,
    ): Response<ChatResponse>

    @GET("api/assistant/chat/messages/")
    suspend fun assistantChatHistory(
        @Query("limit") limit: Int = 200,
    ): Response<ChatHistoryResponse>

    @DELETE("api/assistant/chat/messages/clear/")
    suspend fun assistantChatClear(): Response<Unit>

    @GET("api/assistant/weekly-plan/")
    suspend fun getWeeklyPlan(): Response<WeeklyPlanResponse>

    @GET("api/assistant/weekly-plan/")
    suspend fun getWeeklyPlan(@Header("Authorization") bearer: String): Response<WeeklyPlanResponse>

    @POST("api/assistant/weekly-plan/regenerate/")
    suspend fun regenerateWeeklyPlan(): Response<WeeklyPlanResponse>

    @POST("api/assistant/weekly-plan/regenerate/")
    suspend fun regenerateWeeklyPlan(@Header("Authorization") bearer: String): Response<WeeklyPlanResponse>

    // ===================== Exercise catalog =====================

    @GET("api/exercises/categories/")
    suspend fun getExerciseCategories(): Response<List<ExerciseCategoryResponse>>

    @GET("api/exercises/categories/")
    suspend fun getExerciseCategories(
        @Header("Authorization") bearer: String,
    ): Response<List<ExerciseCategoryResponse>>

    @GET("api/exercises/subcategories/")
    suspend fun getExerciseSubcategories(
        @Query("category") categorySlug: String,
    ): Response<List<ExerciseSubcategoryResponse>>

    @GET("api/exercises/subcategories/")
    suspend fun getExerciseSubcategories(
        @Header("Authorization") bearer: String,
        @Query("category") categorySlug: String,
    ): Response<List<ExerciseSubcategoryResponse>>

    @GET("api/exercises/")
    suspend fun getExercises(
        @Query("category") categorySlug: String? = null,
        @Query("subcategory") subcategorySlug: String? = null,
        @Query("search") search: String? = null,
    ): Response<List<ExerciseListItemResponse>>

    @GET("api/exercises/")
    suspend fun getExercises(
        @Header("Authorization") bearer: String,
        @Query("category") categorySlug: String? = null,
        @Query("subcategory") subcategorySlug: String? = null,
        @Query("search") search: String? = null,
    ): Response<List<ExerciseListItemResponse>>

    @GET("api/exercises/{slug}/")
    suspend fun getExerciseDetail(@Path("slug") slug: String): Response<ExerciseDetailResponse>

    @GET("api/exercises/{slug}/")
    suspend fun getExerciseDetail(
        @Header("Authorization") bearer: String,
        @Path("slug") slug: String,
    ): Response<ExerciseDetailResponse>

    // ===================== Workouts =====================

    @POST("api/workouts/sessions/")
    suspend fun createWorkoutSession(@Body body: WorkoutSessionRequest): Response<Unit>

    @POST("api/workouts/sessions/")
    suspend fun createWorkoutSession(
        @Header("Authorization") bearer: String,
        @Body body: WorkoutSessionRequest,
    ): Response<Unit>

    @GET("api/workouts/history/")
    suspend fun getWorkoutHistory(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): Response<WorkoutHistoryPageResponse>
}
