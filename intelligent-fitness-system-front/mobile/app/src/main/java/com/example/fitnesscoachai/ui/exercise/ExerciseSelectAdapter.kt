package com.example.fitnesscoachai.ui.exercise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.model.Exercise
import com.example.fitnesscoachai.domain.model.ExerciseMedia

class ExerciseSelectAdapter(
    private var items: List<Exercise>,
    private val onClick: (Exercise) -> Unit
) : RecyclerView.Adapter<ExerciseSelectAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = items[position]

        holder.tvName.text = exercise.titleEn
        holder.tvSubtitle.text = buildString {
            append(exercise.sub.titleEn)
            exercise.equipment?.let { append(" | ").append(it) }
        }

        holder.ivIcon.setImageDrawable(null)

        when (val media = exercise.media) {
            is ExerciseMedia.LocalAsset -> {
                if (media.path.startsWith("drawable/")) {
                    val name = media.path.removePrefix("drawable/")
                    val resId = holder.itemView.context.resources.getIdentifier(
                        name,
                        "drawable",
                        holder.itemView.context.packageName
                    )
                    if (resId != 0) {
                        holder.ivIcon.setImageResource(resId)
                    }
                }
            }
            else -> Unit
        }

        holder.itemView.setOnClickListener { onClick(exercise) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Exercise>) {
        items = newItems
        notifyDataSetChanged()
    }
}