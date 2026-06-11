package com.example.fitnesscoachai.ui.stats

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.WorkoutStatsResponse
import kotlinx.coroutines.launch

/**
 * Form-quality dashboard.
 *
 * Hits GET /api/workouts/stats/ and renders:
 *   • a hero card with the all-time average form score
 *   • a bar chart of the last 20 scored workouts
 *   • a list with how many sessions landed in each quality zone
 *   • two totals (workouts + reps)
 *
 * Backend is the source of truth — we don't compute anything here, we just
 * draw what the server sends. If the user has zero scored workouts, every
 * numeric field shows a dash and the chart switches to an empty state.
 */
class StatsActivity : AppCompatActivity() {

    private val tag = "StatsActivity"

    private lateinit var tvAvgScore: TextView
    private lateinit var tvAvgScoreUnit: TextView
    private lateinit var tvAvgSubtitle: TextView
    private lateinit var formScoreChart: FormScoreChartView

    private lateinit var tvZoneExcellent: TextView
    private lateinit var tvZoneGood: TextView
    private lateinit var tvZoneAverage: TextView
    private lateinit var tvZonePoor: TextView

    private lateinit var tvTotalWorkoutsStat: TextView
    private lateinit var tvTotalRepsStat: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        bindViews()
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Render an empty placeholder first so the screen feels instant,
        // then upgrade with the real data when the network responds.
        renderEmpty()
        loadStats()
    }

    private fun bindViews() {
        tvAvgScore = findViewById(R.id.tvAvgScore)
        tvAvgScoreUnit = findViewById(R.id.tvAvgScoreUnit)
        tvAvgSubtitle = findViewById(R.id.tvAvgSubtitle)
        formScoreChart = findViewById(R.id.formScoreChart)

        tvZoneExcellent = findViewById(R.id.tvZoneExcellent)
        tvZoneGood = findViewById(R.id.tvZoneGood)
        tvZoneAverage = findViewById(R.id.tvZoneAverage)
        tvZonePoor = findViewById(R.id.tvZonePoor)

        tvTotalWorkoutsStat = findViewById(R.id.tvTotalWorkoutsStat)
        tvTotalRepsStat = findViewById(R.id.tvTotalRepsStat)
    }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getWorkoutStats()
                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
                    render(body)
                } else {
                    Log.w(tag, "GET /workouts/stats failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w(tag, "GET /workouts/stats threw", e)
            }
        }
    }

    private fun renderEmpty() {
        tvAvgScore.text = "—"
        tvAvgScoreUnit.visibility = View.GONE
        tvAvgSubtitle.text = "Complete a workout to see your stats"

        formScoreChart.setData(emptyList(), avg = 0f)

        tvZoneExcellent.text = "0"
        tvZoneGood.text = "0"
        tvZoneAverage.text = "0"
        tvZonePoor.text = "0"

        tvTotalWorkoutsStat.text = "0"
        tvTotalRepsStat.text = "0"
    }

    private fun render(stats: WorkoutStatsResponse) {
        val avg = stats.average_form_score
        val scored = stats.form_score_history.size

        if (scored > 0 && avg > 0f) {
            tvAvgScore.text = avg.toInt().toString()
            tvAvgScoreUnit.visibility = View.VISIBLE
            tvAvgSubtitle.text = "Across $scored scored ${if (scored == 1) "workout" else "workouts"}"
        } else {
            tvAvgScore.text = "—"
            tvAvgScoreUnit.visibility = View.GONE
            tvAvgSubtitle.text = "Complete a workout to see your stats"
        }

        formScoreChart.setData(stats.form_score_history, avg)

        val z = stats.zone_distribution
        tvZoneExcellent.text = sessionsLabel(z.excellent)
        tvZoneGood.text = sessionsLabel(z.good)
        tvZoneAverage.text = sessionsLabel(z.average)
        tvZonePoor.text = sessionsLabel(z.poor)

        tvTotalWorkoutsStat.text = stats.total_workouts.toString()
        tvTotalRepsStat.text = stats.total_reps.toString()
    }

    private fun sessionsLabel(n: Int): String {
        if (n == 0) return "0"
        return "$n " + if (n == 1) "session" else "sessions"
    }
}
