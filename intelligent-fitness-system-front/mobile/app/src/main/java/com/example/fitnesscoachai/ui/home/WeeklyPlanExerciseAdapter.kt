package com.example.fitnesscoachai.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.models.WeeklyPlanExerciseItem
import com.example.fitnesscoachai.ui.exercise.ExerciseInstructionActivity

class WeeklyPlanExerciseAdapter(
    private val items: List<WeeklyPlanExerciseItem>,
    private val weeklyPlanDayId: Int? = null
) : RecyclerView.Adapter<WeeklyPlanExerciseAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivExerciseImage)
        val ivCompleted: ImageView = itemView.findViewById(R.id.ivCompleted)
        val tvName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val tvMeta: TextView = itemView.findViewById(R.id.tvExerciseMeta)
        val tvNotes: TextView = itemView.findViewById(R.id.tvExerciseNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_plan_exercise, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        holder.tvName.text = item.name

        val metaParts = buildList {
            item.plan_sets?.let { add("$it sets") }
            item.plan_reps?.let { add("$it reps") }
            item.plan_duration_min?.let { add("$it min") }

            if (isEmpty()) {
                item.default_sets?.let { add("$it sets") }
                item.default_reps?.let { add("$it reps") }
                item.default_duration_min?.let { add("$it min") }
            }

            if (!item.equipment.isNullOrBlank()) add(item.equipment)
        }

        holder.tvMeta.text = metaParts.joinToString(" • ").ifBlank { "Tap to open instructions" }
        holder.tvNotes.text = if (item.is_completed) "Completed" else (item.plan_notes?.takeIf { it.isNotBlank() } ?: "Open exercise details")
        holder.ivCompleted.visibility = if (item.is_completed) View.VISIBLE else View.GONE

        bindExerciseImage(holder.ivImage, item.asset_image_name)

        holder.itemView.setOnClickListener {
            ctx.startActivity(
                ExerciseInstructionActivity.newIntent(
                    context = ctx,
                    exerciseSlug = item.slug,
                    weeklyPlanDayId = weeklyPlanDayId
                )
            )
        }
    }

    override fun getItemCount(): Int = items.size

    private fun bindExerciseImage(imageView: ImageView, assetImageName: String?) {
        val ctx = imageView.context

        val rawName = assetImageName
            ?.substringBeforeLast(".")
            ?.trim()
            ?.lowercase()
            ?.replace("-", "_")
            ?.replace(" ", "_")
            .orEmpty()

        val resId = if (rawName.isNotBlank()) {
            ctx.resources.getIdentifier(rawName, "drawable", ctx.packageName)
        } else {
            0
        }

        if (resId != 0) {
            imageView.setImageResource(resId)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
}