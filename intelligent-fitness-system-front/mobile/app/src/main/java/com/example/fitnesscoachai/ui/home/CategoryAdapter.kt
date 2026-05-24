package com.example.fitnesscoachai.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.catalog.CatalogTree
import com.example.fitnesscoachai.domain.model.MainCategory

class CategoryAdapter(
    private var categories: List<MainCategory>,
    private val onCategoryClick: (MainCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCategoryImage: ImageView = view.findViewById(R.id.ivCategoryImage)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvSubcategoryCount: TextView = view.findViewById(R.id.tvSubcategoryCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_card, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val main = categories[position]
        holder.tvCategoryName.text = holder.itemView.context.getString(MainCategoryDisplay.titleRes(main))
        holder.ivCategoryImage.setImageResource(MainCategoryDisplay.backgroundRes(main))
        holder.ivCategoryImage.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, MainCategoryDisplay.colorRes(main))
        )
        val count = CatalogTree.subFor(main).size
        holder.tvSubcategoryCount.text = holder.itemView.context.resources.getQuantityString(
            R.plurals.subcategory_count, count, count
        )
        holder.itemView.setOnClickListener { onCategoryClick(main) }
    }

    override fun getItemCount() = categories.size

    fun setCategories(newCategories: List<MainCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
