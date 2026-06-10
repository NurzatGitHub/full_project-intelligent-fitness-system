package com.example.fitnesscoachai.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.WeeklyPlanDay
import com.example.fitnesscoachai.data.models.WeeklyPlanResponse
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.ui.exercise.ExerciseListActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var categoryAdapter: CategoryAdapter
    private val tag = "HomeFragment"

    companion object {
        private var weeklyPlanCache: WeeklyPlanResponse? = null
        private var cachedUserId: Int? = null

        fun clearCache() {
            weeklyPlanCache = null
            cachedUserId = null
        }
    }

    data class DayCardBinding(
        val card: MaterialCardView,
        val label: TextView,
        val type: TextView,
        val title: TextView
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindUserHeader(view)
        setupProfileHeaderNavigation(view)
        setupSwipeRefresh(view)

        setupCategoryRecyclerView(view)
        categoryAdapter.setCategories(MainCategory.entries)

        bindFromCacheOrLoad(view)
        loadOverallStatus(view)
    }

    private fun setupSwipeRefresh(view: View) {
        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshHome) ?: return
        // Brand colors so the spinner matches the design system.
        swipe.setColorSchemeResources(
            R.color.brand_primary_500,
            R.color.brand_secondary_500,
        )
        swipe.setOnRefreshListener {
            // Hard refresh: drop the cache so we definitely re-hit the backend
            // even if the weekly plan was already cached.
            clearCache()
            loadWeeklyPlan(view)
            loadOverallStatus(view)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            view?.let {
                bindUserHeader(it)
                bindFromCacheOrLoad(it)
                loadOverallStatus(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            bindUserHeader(it)
            bindFromCacheOrLoad(it)
            loadOverallStatus(it)
        }
    }

    private fun getAuthPrefs() =
        requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)

    private fun getProfilePrefs() =
        requireContext().getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    private fun getCurrentUserId(): Int {
        return getAuthPrefs().getInt("user_id", -1)
    }

    private fun getAvatarUriKey(): String {
        val userId = getCurrentUserId()
        return if (userId > 0) "avatar_uri_$userId" else "avatar_uri_guest"
    }

    private fun clearBrokenAvatarUri() {
        getProfilePrefs()
            .edit()
            .remove(getAvatarUriKey())
            .apply()
    }

    private fun resetHeaderAvatar(avatar: ImageView) {
        avatar.setImageDrawable(null)
        avatar.background = null
        avatar.setPadding(0, 0, 0, 0)
        avatar.clearColorFilter()
        avatar.imageTintList = null
        avatar.invalidate()
    }

    private fun applyHeaderAvatarSafely(avatar: ImageView, avatarUriString: String?) {
        if (avatarUriString.isNullOrBlank()) {
            resetHeaderAvatar(avatar)
            return
        }

        try {
            val uri = Uri.parse(avatarUriString)

            requireContext().contentResolver.openInputStream(uri)?.use {
                // validate access
            } ?: throw IllegalStateException("Avatar stream is null")

            avatar.setImageURI(uri)
            avatar.background = null
            avatar.setPadding(0, 0, 0, 0)
            avatar.clearColorFilter()
            avatar.imageTintList = null
            avatar.invalidate()

        } catch (e: SecurityException) {
            Log.w(tag, "No access to avatar uri in Home: $avatarUriString", e)
            clearBrokenAvatarUri()
            resetHeaderAvatar(avatar)
        } catch (e: Exception) {
            Log.w(tag, "Broken avatar uri in Home: $avatarUriString", e)
            clearBrokenAvatarUri()
            resetHeaderAvatar(avatar)
        }
    }

    private fun bindFromCacheOrLoad(view: View) {
        val currentUserId = getCurrentUserId()
        val cached = weeklyPlanCache

        if (cached != null && cachedUserId != null && cachedUserId == currentUserId) {
            bindWeeklyPlan(view, cached)
        } else {
            loadWeeklyPlan(view)
        }
    }

    private fun bindUserHeader(view: View) {
        val authPrefs = getAuthPrefs()
        val userName = authPrefs.getString("user_name", "beginner") ?: "beginner"
        view.findViewById<TextView>(R.id.tvUserName)?.text = "$userName 👋"

        val avatarUri = getProfilePrefs().getString(getAvatarUriKey(), null)

        view.findViewById<ImageView>(R.id.ivAvatar)?.let { avatar ->
            applyHeaderAvatarSafely(avatar, avatarUri)
        }
    }

    private fun setupProfileHeaderNavigation(view: View) {
        view.findViewById<View>(R.id.homeProfileHeader)?.setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNavigation)
                ?.selectedItemId = R.id.nav_profile
        }
    }

    private fun setupCategoryRecyclerView(view: View) {
        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategories)
        categoryAdapter = CategoryAdapter(emptyList()) { main ->
            startActivity(ExerciseListActivity.newIntent(requireContext(), main.id))
        }
        rvCategories.adapter = categoryAdapter
        rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun loadWeeklyPlan(view: View) {
        val prefs = getAuthPrefs()
        val isGuest = prefs.getBoolean("isGuest", false)
        // Source of truth: encrypted TokenStore; fall back to legacy pref.
        val token = com.example.fitnesscoachai.data.api.RetrofitClient.tokenStore().accessToken
            ?: prefs.getString("access_token", null)
        val currentUserId = prefs.getInt("user_id", -1)

        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshHome)

        if (isGuest || token.isNullOrBlank()) {
            clearCache()
            showGuestPlan(view)
            swipe?.isRefreshing = false
            return
        }

        setLoadingState(view)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getWeeklyPlan("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val plan = response.body()!!
                    weeklyPlanCache = plan
                    cachedUserId = currentUserId
                    bindWeeklyPlan(view, plan)
                } else {
                    Log.w(tag, "weekly-plan HTTP ${response.code()} ${response.message()}")
                    showPlanError(view)
                }
            } catch (e: Exception) {
                Log.e(tag, "weekly-plan failed", e)
                showPlanError(view)
            } finally {
                swipe?.isRefreshing = false
            }
        }
    }

    private fun setLoadingState(view: View) {
        view.findViewById<TextView>(R.id.tvAiPlanTitle)?.text = "AI Weekly Plan"
        view.findViewById<TextView>(R.id.tvAiPlanSummary)?.text = "Loading your weekly plan..."
        view.findViewById<TextView>(R.id.tvTodayPlan)?.text = "Today"
        view.findViewById<TextView>(R.id.tvTodayMeta)?.text = "Please wait..."

        dayBindings(view).forEachIndexed { index, binding ->
            binding.label.text = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]
            binding.type.text = "..."
            binding.title.text = "..."
            binding.card.setOnClickListener(null)
        }
    }

    private fun showGuestPlan(view: View) {
        view.findViewById<TextView>(R.id.tvAiPlanTitle)?.text = "AI Weekly Plan"
        view.findViewById<TextView>(R.id.tvAiPlanSummary)?.text = "Login to get a personalized weekly plan"
        view.findViewById<TextView>(R.id.tvTodayPlan)?.text = buildTodayHeader()
        view.findViewById<TextView>(R.id.tvTodayMeta)?.text = "Sign in and complete onboarding"

        dayBindings(view).forEachIndexed { index, binding ->
            binding.label.text = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]
            binding.type.text = "-"
            binding.title.text = "Login"
            binding.card.setOnClickListener(null)
        }
    }

    private fun showPlanError(view: View) {
        view.findViewById<TextView>(R.id.tvAiPlanTitle)?.text = "AI Weekly Plan"
        view.findViewById<TextView>(R.id.tvAiPlanSummary)?.text =
            "Couldn't reach the coach. Swipe down to retry."
        view.findViewById<TextView>(R.id.tvTodayPlan)?.text = buildTodayHeader()
        view.findViewById<TextView>(R.id.tvTodayMeta)?.text = "Pull to refresh"

        dayBindings(view).forEachIndexed { index, binding ->
            binding.label.text = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]
            binding.type.text = "-"
            binding.title.text = "—"
            binding.card.setOnClickListener {
                // Tapping a card while in error state also retries.
                clearCache()
                loadWeeklyPlan(view)
            }
        }
    }

    private fun bindWeeklyPlan(view: View, plan: WeeklyPlanResponse) {
        view.findViewById<TextView>(R.id.tvAiPlanTitle)?.text =
            if (plan.title.isBlank()) "AI Weekly Plan" else plan.title

        view.findViewById<TextView>(R.id.tvAiPlanSummary)?.text =
            if (plan.goal_summary.isBlank()) "Personalized weekly training plan"
            else plan.goal_summary

        val todayIndex = getTodayIndex()
        val bindings = dayBindings(view)

        bindings.forEachIndexed { index, binding ->
            val day = plan.days.getOrNull(index)
            if (day != null) {
                bindDay(binding, day, index == todayIndex)
            } else {
                binding.label.text = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]
                binding.type.text = "-"
                binding.title.text = "-"
                binding.card.setOnClickListener(null)
            }
        }

        val today = plan.days.getOrNull(todayIndex) ?: plan.days.firstOrNull()
        view.findViewById<TextView>(R.id.tvTodayPlan)?.text = buildTodayHeader()

        if (today != null) {
            val metaParts = buildList {
                add(today.label)
                add(today.type.replaceFirstChar { it.uppercase() })
                add("${today.duration_min} min")
                if (today.note.isNotBlank()) add(today.note)
                if (today.is_completed) add("Completed")
            }
            view.findViewById<TextView>(R.id.tvTodayMeta)?.text = metaParts.joinToString(" • ")
        } else {
            view.findViewById<TextView>(R.id.tvTodayMeta)?.text = plan.today_tip
        }

        centerTodayCard(view, todayIndex)
    }

    private fun bindDay(binding: DayCardBinding, day: WeeklyPlanDay, isToday: Boolean) {
        val ctx = requireContext()

        // Pulse design tokens — sourced from colors.xml so themes can drive them.
        val workoutBg = ContextCompat.getColor(ctx, R.color.day_card_workout_bg)
        val workoutStroke = ContextCompat.getColor(ctx, R.color.day_card_workout_stroke)
        val workoutLabel = ContextCompat.getColor(ctx, R.color.day_card_workout_label)

        val todayBg = ContextCompat.getColor(ctx, R.color.day_card_today_bg)
        val todayStroke = ContextCompat.getColor(ctx, R.color.day_card_today_stroke)
        val todayLabel = ContextCompat.getColor(ctx, R.color.day_card_today_label)

        val doneBg = ContextCompat.getColor(ctx, R.color.day_card_done_bg)
        val doneStroke = ContextCompat.getColor(ctx, R.color.day_card_done_stroke)
        val doneLabel = ContextCompat.getColor(ctx, R.color.day_card_done_label)

        when {
            day.is_completed -> {
                binding.card.setCardBackgroundColor(doneBg)
                binding.card.strokeColor = doneStroke
                binding.card.strokeWidth = dp(2)
                binding.card.alpha = 1f
                binding.card.scaleX = 1f
                binding.card.scaleY = 1f
                binding.card.cardElevation = dp(4).toFloat()

                binding.label.text = if (isToday) "✓ ${day.label} • Today" else "✓ ${day.label}"
                binding.label.setTextColor(doneLabel)
                binding.type.text = "Completed"
                binding.type.setTextColor(doneLabel)
                binding.title.text = day.title
                binding.title.setTextColor(workoutLabel)

                binding.label.setTypeface(null, Typeface.BOLD)
                binding.title.setTypeface(null, Typeface.BOLD)
            }

            isToday -> {
                // Bold "TODAY" treatment: filled with brand primary, white text,
                // thick stroke, scaled up and elevated so it pops in the strip.
                val primary = ContextCompat.getColor(ctx, R.color.brand_primary_500)
                binding.card.setCardBackgroundColor(primary)
                binding.card.strokeColor = ContextCompat.getColor(ctx, R.color.brand_primary_200)
                binding.card.strokeWidth = dp(3)
                binding.card.alpha = 1f
                binding.card.scaleX = 1.08f
                binding.card.scaleY = 1.08f
                binding.card.cardElevation = dp(10).toFloat()

                binding.label.text = "● TODAY · ${day.label}"
                binding.label.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                binding.type.text = day.type.replaceFirstChar { it.uppercase() }
                binding.type.setTextColor(ContextCompat.getColor(ctx, R.color.brand_primary_100))
                binding.title.text = day.title
                binding.title.setTextColor(ContextCompat.getColor(ctx, R.color.white))

                binding.label.setTypeface(null, Typeface.BOLD)
                binding.title.setTypeface(null, Typeface.BOLD)
            }

            else -> {
                binding.card.setCardBackgroundColor(workoutBg)
                binding.card.strokeColor = workoutStroke
                binding.card.strokeWidth = dp(1)
                binding.card.alpha = 0.92f
                binding.card.scaleX = 1f
                binding.card.scaleY = 1f
                binding.card.cardElevation = 0f

                binding.label.text = day.label
                binding.label.setTextColor(workoutLabel)
                binding.type.text = day.type.replaceFirstChar { it.uppercase() }
                binding.type.setTextColor(todayLabel)
                binding.title.text = day.title
                binding.title.setTextColor(workoutLabel)

                binding.label.setTypeface(null, Typeface.NORMAL)
                binding.title.setTypeface(null, Typeface.NORMAL)
            }
        }

        binding.card.setOnClickListener {
            openPlanDay(day)
        }
    }

    private fun openPlanDay(day: WeeklyPlanDay) {
        val json = Gson().toJson(day)
        val intent = Intent(requireContext(), WeeklyPlanDayActivity::class.java)
        intent.putExtra(WeeklyPlanDayActivity.EXTRA_DAY_JSON, json)
        startActivity(intent)
    }

    private fun centerTodayCard(view: View, todayIndex: Int) {
        val scroll = view.findViewById<HorizontalScrollView>(R.id.hsvWeekDays)
        val bindings = dayBindings(view)
        val todayCard = bindings.getOrNull(todayIndex)?.card ?: return

        scroll.post {
            val targetX = todayCard.left - (scroll.width - todayCard.width) / 2
            scroll.smoothScrollTo(targetX.coerceAtLeast(0), 0)
        }
    }

    private fun buildTodayHeader(): String {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH)
        return "Today • ${formatter.format(calendar.time)}"
    }

    private fun dayBindings(view: View): List<DayCardBinding> {
        return listOf(
            DayCardBinding(
                view.findViewById(R.id.cardDay1),
                view.findViewById(R.id.tvDay1Label),
                view.findViewById(R.id.tvDay1Type),
                view.findViewById(R.id.tvDay1Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay2),
                view.findViewById(R.id.tvDay2Label),
                view.findViewById(R.id.tvDay2Type),
                view.findViewById(R.id.tvDay2Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay3),
                view.findViewById(R.id.tvDay3Label),
                view.findViewById(R.id.tvDay3Type),
                view.findViewById(R.id.tvDay3Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay4),
                view.findViewById(R.id.tvDay4Label),
                view.findViewById(R.id.tvDay4Type),
                view.findViewById(R.id.tvDay4Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay5),
                view.findViewById(R.id.tvDay5Label),
                view.findViewById(R.id.tvDay5Type),
                view.findViewById(R.id.tvDay5Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay6),
                view.findViewById(R.id.tvDay6Label),
                view.findViewById(R.id.tvDay6Type),
                view.findViewById(R.id.tvDay6Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay7),
                view.findViewById(R.id.tvDay7Label),
                view.findViewById(R.id.tvDay7Type),
                view.findViewById(R.id.tvDay7Title),
            ),
        )
    }

    private fun getTodayIndex(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }

    private fun loadOverallStatus(view: View) {
        val isGuest = getAuthPrefs().getBoolean("isGuest", false)

        // 1) Render immediately from the on-device cache so the UI doesn't flash.
        val localCount = com.example.fitnesscoachai.data.local.WorkoutHistoryStore
            .getCount(requireContext())
        renderStats(view, localCount, isGuest)

        // 2) For logged-in users, ask the server for the authoritative count.
        // DRF PageNumberPagination ships a `count` field in every page, so we
        // request page_size=1 — cheap call, full count, no extra payload.
        if (isGuest) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getWorkoutHistory(page = 1, pageSize = 1)
                if (resp.isSuccessful) {
                    val serverCount = resp.body()?.count
                    if (serverCount != null && serverCount > localCount) {
                        // Server saw more sessions than this device (e.g. user
                        // also trained on another phone). Trust the server and
                        // re-render.
                        renderStats(view, serverCount, isGuest = false)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "workout-history count failed (using local)", e)
            }
        }
    }

    private fun renderStats(view: View, count: Int, isGuest: Boolean) {
        val tvWorkouts = view.findViewById<TextView>(R.id.tvTotalWorkouts)
        val tvScore = view.findViewById<TextView>(R.id.tvAverageFormScore)

        tvWorkouts?.text = count.toString()
        // Form-score is intentionally a dash. The backend doesn't compute a
        // per-rep AI quality score yet, so showing "78%" would be inventing
        // data. When the scoring pipeline lands we'll wire it in here.
        tvScore?.text = "—"
    }
}