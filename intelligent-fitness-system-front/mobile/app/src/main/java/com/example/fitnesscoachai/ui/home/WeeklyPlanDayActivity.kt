package com.example.fitnesscoachai.ui.home

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.models.WeeklyPlanDay
import com.google.gson.Gson

class WeeklyPlanDayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DAY_JSON = "extra_day_json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_plan_day)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val json = intent.getStringExtra(EXTRA_DAY_JSON)
        val day = json?.let {
            runCatching { Gson().fromJson(it, WeeklyPlanDay::class.java) }.getOrNull()
        }

        if (day == null) {
            finish()
            return
        }

        bindDay(day)
    }

    private fun bindDay(day: WeeklyPlanDay) {
        findViewById<TextView>(R.id.tvDayLabel).text = day.label
        findViewById<TextView>(R.id.tvDayTitle).text = day.title
        findViewById<TextView>(R.id.tvDayMeta).text =
            "${day.type.replaceFirstChar { it.uppercase() }} • ${day.duration_min} min"

        findViewById<TextView>(R.id.tvDayNote).text =
            if (day.note.isBlank()) "Stay consistent and focus on form."
            else day.note

        val emptyView = findViewById<TextView>(R.id.tvEmptyExercises)
        val recycler = findViewById<RecyclerView>(R.id.rvPlanExercises)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = WeeklyPlanExerciseAdapter(
            items = day.exercises,
            weeklyPlanDayId = day.id
        )

        emptyView.visibility = if (day.exercises.isEmpty()) TextView.VISIBLE else TextView.GONE
        recycler.visibility = if (day.exercises.isEmpty()) RecyclerView.GONE else RecyclerView.VISIBLE
    }
}