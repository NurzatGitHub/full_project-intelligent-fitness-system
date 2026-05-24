package com.example.fitnesscoachai.ui.exercise

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fitnesscoachai.ui.workout.plank.PlankActivity
import com.example.fitnesscoachai.ui.workout.pushup.PushupActivity
import com.example.fitnesscoachai.ui.workout.shoulderpress.ShoulderPressActivity
import com.example.fitnesscoachai.ui.workout.squat.SquatActivity
import com.example.fitnesscoachai.ui.workout.crunch.CrunchActivity
import java.util.Locale

object ExerciseRouter {

    fun createIntent(
        context: Context,
        exerciseId: String? = null,
        exerciseSlug: String? = null,
        exerciseName: String? = null,
        weeklyPlanDayId: Int? = null
    ): Intent {
        val id = exerciseId.orEmpty().trim().lowercase(Locale.ROOT)
        val slug = exerciseSlug.orEmpty().trim().lowercase(Locale.ROOT)
        val name = exerciseName.orEmpty().trim().lowercase(Locale.ROOT)

        val resolvedName = exerciseName ?: ""
        val resolvedSlug = if (!exerciseSlug.isNullOrBlank()) {
            exerciseSlug
        } else {
            buildSlug(resolvedName)
        }

        Log.d("ExerciseRouter", "id=$id, slug=$slug, name=$name")

        val isPushup =
            id == "ex16" ||
                    slug == "push-up" ||
                    slug == "pushup" ||
                    slug.contains("push-up") ||
                    slug.contains("pushup") ||
                    name == "push up" ||
                    name == "pushup" ||
                    name.contains("push up") ||
                    name.contains("pushup")

        val isSquat =
            id == "ex6" ||
                    slug == "squat" ||
                    slug.contains("squat") ||
                    name == "squat" ||
                    name.contains("squat")

        val isPlank =
            id == "ex18" ||
                    slug == "plank" ||
                    slug.contains("plank") ||
                    name == "plank" ||
                    name.contains("plank")

        val isShoulderPress =
            id == "ex114" ||
                    slug == "shoulder-press" ||
                    slug.contains("shoulder-press") ||
                    slug.contains("shoulderpress") ||
                    name == "shoulder press" ||
                    name.contains("shoulder press")
        val isCrunch =
            id == "ex125" ||
                    slug == "crunch" ||
                    slug.contains("crunch") ||
                    name == "crunch" ||
                    name.contains("crunch")

        val intent = when {
            isPushup -> {
                Log.d("ExerciseRouter", "Opening PushupActivity")
                Intent(context, PushupActivity::class.java)
            }

            isSquat -> {
                Log.d("ExerciseRouter", "Opening SquatActivity")
                Intent(context, SquatActivity::class.java)
            }

            isPlank -> {
                Log.d("ExerciseRouter", "Opening PlankActivity")
                Intent(context, PlankActivity::class.java)
            }

            isShoulderPress -> {
                Log.d("ExerciseRouter", "Opening ShoulderPressActivity")
                Intent(context, ShoulderPressActivity::class.java)
            }

            isCrunch -> {
                Log.d("ExerciseRouter", "Opening CrunchActivity")
                Intent(context, CrunchActivity::class.java)
            }

            else -> {
                Log.d("ExerciseRouter", "Opening ExerciseInstructionActivity")
                ExerciseInstructionActivity.newIntent(
                    context = context,
                    exerciseSlug = resolvedSlug
                )
            }
        }

        intent.putExtra("exercise_name", resolvedName.ifBlank { "Exercise" })
        intent.putExtra("exercise_slug", resolvedSlug)

        if (weeklyPlanDayId != null && weeklyPlanDayId > 0) {
            intent.putExtra("weekly_plan_day_id", weeklyPlanDayId)
        }

        return intent
    }

    private fun buildSlug(title: String): String {
        return title
            .trim()
            .lowercase(Locale.ROOT)
            .replace("(", "")
            .replace(")", "")
            .replace("’", "")
            .replace("'", "")
            .replace(" ", "-")
    }
}