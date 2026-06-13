package com.example.fitnesscoachai.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorkoutHistoryItem(
    val exercise: String,
    val reps: Int,
    val duration: Int,
    val date: Long,
)

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: WorkoutHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Toolbar back navigation
        findViewById<Toolbar>(R.id.toolbar)?.setNavigationOnClickListener { finish() }

        rvHistory = findViewById(R.id.rvHistory)
        emptyState = findViewById(R.id.tvEmpty)

        adapter = WorkoutHistoryAdapter { item ->
            // Tapping a history entry is read-only for now.
            // We do NOT relaunch SummaryActivity — that was confusing and
            // also broke the back stack (history → summary → history → ...).
            val unit = if (isIsometric(item.exercise)) "sec" else "reps"
            val msg = "${item.exercise} · ${item.reps} $unit"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter

        loadWorkoutHistory()
    }

    override fun onResume() {
        super.onResume()
        // Refresh in case a new workout was just saved before navigating here.
        loadWorkoutHistory()
    }

    private fun loadWorkoutHistory() {
        val prefs = getSharedPreferences("workout_history", MODE_PRIVATE)
        val store = com.example.fitnesscoachai.data.local.WorkoutHistoryStore
        val historyCount = store.getCount(this)

        val historyItems = mutableListOf<WorkoutHistoryItem>()

        for (i in 0 until historyCount) {
            val exercise = prefs.getString(store.exerciseKey(this, i), null)
            val duration = prefs.getInt(store.durationKey(this, i), 0)
            val reps = prefs.getInt(store.repsKey(this, i), 0)
            val date = prefs.getLong(store.dateKey(this, i), 0)

            if (exercise != null && date > 0) {
                historyItems.add(WorkoutHistoryItem(exercise, reps, duration, date))
            }
        }

        historyItems.sortByDescending { it.date }

        if (historyItems.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            adapter.submitList(historyItems)
        }
    }
}

/**
 * Returns true when the given exercise name corresponds to an isometric
 * (hold-the-pose) exercise, where the "reps" field actually stores seconds.
 * Centralized here so HistoryActivity, ProfileFragment, and the adapter all
 * agree on the same rule.
 */
internal fun isIsometric(exerciseName: String): Boolean {
    val n = exerciseName.lowercase(Locale.US)
    return n.contains("plank")
}

class WorkoutHistoryAdapter(
    private val onItemClick: (WorkoutHistoryItem) -> Unit,
) : RecyclerView.Adapter<WorkoutHistoryAdapter.ViewHolder>() {

    private var items: List<WorkoutHistoryItem> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault())

    fun submitList(newItems: List<WorkoutHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, dateFormat)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvExercise: TextView = itemView.findViewById(R.id.tvExercise)
        private val tvReps: TextView = itemView.findViewById(R.id.tvReps)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)

        fun bind(item: WorkoutHistoryItem, dateFormat: SimpleDateFormat) {
            tvDate.text = dateFormat.format(Date(item.date))
            tvExercise.text = item.exercise

            // For isometric exercises (plank) the "reps" int is actually
            // seconds-held — relabel so the pill reads "30 sec" instead of
            // the misleading "30 reps".
            tvReps.text = if (isIsometric(item.exercise)) {
                "${item.reps} sec"
            } else {
                "${item.reps} reps"
            }

            val minutes = item.duration / 60
            val seconds = item.duration % 60
            tvDuration.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
