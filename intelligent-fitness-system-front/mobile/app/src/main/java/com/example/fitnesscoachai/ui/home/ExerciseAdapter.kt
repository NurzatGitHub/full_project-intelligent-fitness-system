package com.example.fitnesscoachai.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.models.ExerciseListItemResponse

class ExerciseAdapter(
    private var exercises: List<ExerciseListItemResponse>,
    private val onExerciseClick: (ExerciseListItemResponse) -> Unit = {}
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(parent: android.view.View) : RecyclerView.ViewHolder(parent) {
        val tvExerciseName: TextView = parent.findViewById(R.id.tvExerciseName)
        val tvExerciseTarget: TextView = parent.findViewById(R.id.tvExerciseTarget)
        val ivExerciseGif: ImageView = parent.findViewById(R.id.ivExerciseGif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]

        holder.tvExerciseName.text = exercise.name
        holder.tvExerciseTarget.text = buildString {
            append(exercise.subcategory_name ?: exercise.category_name)
            if (exercise.equipment.isNotBlank()) {
                append(" | ").append(exercise.equipment)
            }
        }

        val imageName = exercise.asset_image_name.trim()
        if (imageName.isNotEmpty()) {
            val resId = holder.itemView.context.resources.getIdentifier(
                imageName,
                "drawable",
                holder.itemView.context.packageName
            )
            if (resId != 0) {
                holder.ivExerciseGif.setImageResource(resId)
            } else {
                holder.ivExerciseGif.setImageDrawable(null)
            }
        } else {
            holder.ivExerciseGif.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener { onExerciseClick(exercise) }
    }

    override fun getItemCount(): Int = exercises.size

    fun updateData(newExercises: List<ExerciseListItemResponse>) {
        exercises = newExercises
        notifyDataSetChanged()
    }
}