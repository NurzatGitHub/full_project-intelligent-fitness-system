package com.example.fitnesscoachai.ui.home

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView, который при layout_height="wrap_content" внутри ScrollView
 * раскладывает все элементы по высоте, чтобы были видны все категории.
 */
class ExpandableHeightRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            val expandSpec = View.MeasureSpec.makeMeasureSpec(
                Int.MAX_VALUE shr 2,
                View.MeasureSpec.AT_MOST
            )
            super.onMeasure(widthSpec, expandSpec)
        } else {
            super.onMeasure(widthSpec, heightSpec)
        }
    }
}
