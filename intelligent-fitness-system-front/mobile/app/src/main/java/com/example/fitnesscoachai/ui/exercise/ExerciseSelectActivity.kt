package com.example.fitnesscoachai.ui.exercise

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.repo.ExerciseRepositoryLocal
import com.example.fitnesscoachai.domain.model.Exercise
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.ui.workout.plank.PlankActivity
import com.example.fitnesscoachai.ui.workout.squat.SquatActivity
import com.example.fitnesscoachai.ui.exercise.ExerciseRouter
import kotlinx.coroutines.launch

class ExerciseSelectActivity : AppCompatActivity() {

    private var allExercises: List<Exercise> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_select)

        val rv = findViewById<RecyclerView>(R.id.rvExerciseSelect)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        val adapter = ExerciseSelectAdapter(emptyList()) { exercise ->
            routeToWorkout(exercise)
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val repo = ExerciseRepositoryLocal()

        lifecycleScope.launch {
            val all = repo.getExercisesByMainCategory(MainCategory.BACK) +
                    repo.getExercisesByMainCategory(MainCategory.CHEST) +
                    repo.getExercisesByMainCategory(MainCategory.LEGS) +
                    repo.getExercisesByMainCategory(MainCategory.ARMS) +
                    repo.getExercisesByMainCategory(MainCategory.ABS) +
                    repo.getExercisesByMainCategory(MainCategory.CARDIO)

            allExercises = all
            adapter.submitList(allExercises)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().trim()

                val filtered = if (query.isEmpty()) {
                    allExercises
                } else {
                    allExercises.filter { exercise ->
                        exercise.titleEn.contains(query, ignoreCase = true)
                    }
                }

                adapter.submitList(filtered)
            }
        })
    }

    private fun routeToWorkout(exercise: Exercise) {
        android.util.Log.d(
            "ExerciseSelect",
            "clicked id=${exercise.id}, title=${exercise.titleEn}, slug=${buildExerciseSlug(exercise.titleEn)}"
        )

        val intent = ExerciseRouter.createIntent(
            context = this,
            exerciseId = exercise.id,
            exerciseSlug = buildExerciseSlug(exercise.titleEn),
            exerciseName = exercise.titleEn
        )
        startActivity(intent)
    }

    private fun buildExerciseSlug(title: String): String {
        return title
            .lowercase()
            .replace("(", "")
            .replace(")", "")
            .replace("’", "")
            .replace("'", "")
            .replace(" ", "-")
    }
}