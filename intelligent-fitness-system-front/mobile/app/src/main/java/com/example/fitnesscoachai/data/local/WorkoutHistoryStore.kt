package com.example.fitnesscoachai.data.local

import android.content.Context

/**
 * Tiny helper around the legacy `workout_history` SharedPreferences file.
 *
 * Why it exists: the original code used a SINGLE global `history_count` key,
 * which leaked between users — when a logged-in athlete finished 5 workouts,
 * then signed out and someone tapped "Continue as Guest", the guest profile
 * STILL showed 5 workouts because the counter wasn't user-scoped.
 *
 * Every read/write now goes through here, so we have one place that decides
 * the suffix and one place to fix if it changes.
 */
object WorkoutHistoryStore {

    private const val PREFS_NAME = "workout_history"

    /** Encodes the current user identity into a key suffix. */
    private fun suffix(context: Context): String {
        val auth = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        if (auth.getBoolean("isGuest", false)) return "guest"
        val userId = auth.getInt("user_id", -1)
        return if (userId > 0) userId.toString() else "anonymous"
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** "history_count_42" for user 42, "history_count_guest" for guest sessions. */
    fun countKey(context: Context): String = "history_count_${suffix(context)}"

    /** Per-entry keys, also user-scoped. */
    fun exerciseKey(context: Context, index: Int) = "exercise_${suffix(context)}_$index"
    fun durationKey(context: Context, index: Int) = "duration_${suffix(context)}_$index"
    fun repsKey(context: Context, index: Int) = "reps_${suffix(context)}_$index"
    fun dateKey(context: Context, index: Int) = "date_${suffix(context)}_$index"

    fun getCount(context: Context): Int {
        return prefs(context).getInt(countKey(context), 0)
    }

    fun appendSession(
        context: Context,
        exerciseName: String,
        durationSec: Int,
        reps: Int,
    ) {
        val p = prefs(context)
        val key = countKey(context)
        val nextIndex = p.getInt(key, 0)
        p.edit()
            .putString(exerciseKey(context, nextIndex), exerciseName)
            .putInt(durationKey(context, nextIndex), durationSec)
            .putInt(repsKey(context, nextIndex), reps)
            .putLong(dateKey(context, nextIndex), System.currentTimeMillis())
            .putInt(key, nextIndex + 1)
            .apply()
    }

    /** Wipe ALL workout-history prefs. Used on guest sign-in / logout. */
    fun wipeAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
