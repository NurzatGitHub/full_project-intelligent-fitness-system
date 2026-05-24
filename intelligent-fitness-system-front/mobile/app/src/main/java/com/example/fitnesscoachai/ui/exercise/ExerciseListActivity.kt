package com.example.fitnesscoachai.ui.exercise

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.ExerciseSubcategoryResponse
import com.example.fitnesscoachai.data.repo.ExerciseMemoryCache
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.ui.home.ExerciseAdapter
import com.example.fitnesscoachai.ui.home.MainCategoryDisplay
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class ExerciseListActivity : AppCompatActivity() {

    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val mainId = intent.getStringExtra(EXTRA_MAIN_CATEGORY_ID)
        val main = MainCategory.fromId(mainId)
        if (main == null) {
            finish()
            return
        }

        supportActionBar?.title = getString(R.string.exercise_list_title)

        val rvExercises = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvExercises)
        exerciseAdapter = ExerciseAdapter(emptyList()) { exercise ->
            startActivity(ExerciseInstructionActivity.newIntent(this, exercise.slug))
        }
        rvExercises.adapter = exerciseAdapter
        rvExercises.layoutManager = LinearLayoutManager(this)

        loadSubcategories(main)
    }

    private fun getBearerToken(): String? {
        // Source of truth: encrypted TokenStore. Falls back to legacy unencrypted
        // pref only for users who haven't been migrated yet.
        val secure = com.example.fitnesscoachai.data.api.RetrofitClient.tokenStore().accessToken
        val token = secure
            ?: getSharedPreferences("auth", Context.MODE_PRIVATE).getString("access_token", null)
        return if (token.isNullOrBlank()) null else "Bearer $token"
    }

    private fun loadSubcategories(main: MainCategory) {
        val token = getBearerToken()
        if (token == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val chipGroup = findViewById<ChipGroup>(R.id.chipGroup)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        val cached = ExerciseMemoryCache.getSubcategories(main.id)
        if (!cached.isNullOrEmpty()) {
            setupChips(chipGroup, main, cached, pbLoading, tvEmpty, token)
            return
        }

        pbLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getExerciseSubcategories(
                    bearer = token,
                    categorySlug = main.id
                )

                pbLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val subcategories = response.body().orEmpty()
                    ExerciseMemoryCache.putSubcategories(main.id, subcategories)

                    if (subcategories.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "No subcategories found"
                    } else {
                        setupChips(chipGroup, main, subcategories, pbLoading, tvEmpty, token)
                    }
                } else {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Failed to load subcategories"
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Network error"
            }
        }
    }

    private fun setupChips(
        chipGroup: ChipGroup,
        main: MainCategory,
        subcategories: List<ExerciseSubcategoryResponse>,
        pbLoading: ProgressBar,
        tvEmpty: TextView,
        token: String
    ) {
        val selectedColor = getColor(MainCategoryDisplay.colorRes(main))
        val unselectedColor = getColor(android.R.color.darker_gray)
        val textSelected = getColor(android.R.color.white)
        val textUnselected = getColor(android.R.color.black)

        val bgStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(selectedColor, unselectedColor)
        )

        val textStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(textSelected, textUnselected)
        )

        chipGroup.setOnCheckedStateChangeListener(null)
        chipGroup.removeAllViews()

        var firstChipId = -1

        subcategories.sortedBy { it.sort_order }.forEachIndexed { index, sub ->
            val chip = createChip(sub, bgStates, textStates)
            if (index == 0) {
                firstChipId = View.generateViewId()
                chip.id = firstChipId
            }
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { group, _ ->
            val checkedId = group.checkedChipId
            val chip = group.findViewById<Chip>(checkedId) ?: return@setOnCheckedStateChangeListener
            val sub = chip.tag as? ExerciseSubcategoryResponse ?: return@setOnCheckedStateChangeListener
            loadExercisesForSub(main.id, sub.slug, pbLoading, tvEmpty, token)
        }

        if (firstChipId != -1) {
            chipGroup.check(firstChipId)
        }
    }

    private fun createChip(
        sub: ExerciseSubcategoryResponse,
        bgStates: ColorStateList,
        textStates: ColorStateList
    ): Chip {
        val chip = Chip(this)
        chip.text = sub.name
        chip.isCheckable = true
        chip.isClickable = true
        chip.chipCornerRadius = dpToPx(24f)
        chip.chipBackgroundColor = bgStates
        chip.setTextColor(textStates)
        chip.tag = sub

        val params = ChipGroup.LayoutParams(
            ChipGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(36f).toInt()
        )
        params.setMargins(dpToPx(4f).toInt(), 0, dpToPx(4f).toInt(), 0)
        chip.layoutParams = params

        return chip
    }

    private fun loadExercisesForSub(
        categorySlug: String,
        subcategorySlug: String,
        pbLoading: ProgressBar,
        tvEmpty: TextView,
        token: String
    ) {
        val cached = ExerciseMemoryCache.getExercises(categorySlug, subcategorySlug)
        if (!cached.isNullOrEmpty()) {
            exerciseAdapter.updateData(cached)
            pbLoading.visibility = View.GONE
            tvEmpty.visibility = if (cached.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        pbLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getExercises(
                    bearer = token,
                    categorySlug = categorySlug,
                    subcategorySlug = subcategorySlug
                )

                pbLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val exercises = response.body().orEmpty()
                    ExerciseMemoryCache.putExercises(categorySlug, subcategorySlug, exercises)
                    exerciseAdapter.updateData(exercises)
                    tvEmpty.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    exerciseAdapter.updateData(emptyList())
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Failed to load exercises"
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                exerciseAdapter.updateData(emptyList())
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Network error"
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    companion object {
        const val EXTRA_MAIN_CATEGORY_ID = "extra_main_category_id"

        fun newIntent(context: Context, mainCategoryId: String): Intent {
            return Intent(context, ExerciseListActivity::class.java).apply {
                putExtra(EXTRA_MAIN_CATEGORY_ID, mainCategoryId)
            }
        }
    }
}