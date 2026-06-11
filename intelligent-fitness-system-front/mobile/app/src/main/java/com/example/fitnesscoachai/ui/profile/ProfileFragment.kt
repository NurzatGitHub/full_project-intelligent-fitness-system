package com.example.fitnesscoachai.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.UpdateProfileRequest
import com.example.fitnesscoachai.data.models.User
import com.example.fitnesscoachai.ui.auth.AuthActivity
import com.example.fitnesscoachai.ui.history.HistoryActivity
import com.example.fitnesscoachai.ui.home.HomeFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private val tag = "ProfileFragment"

    private val avatarPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.w(tag, "Не удалось персистить Uri для аватара: $uri", e)
                }

                getProfilePrefs()
                    .edit()
                    .putString(getAvatarUriKey(), uri.toString())
                    .apply()

                val ivAvatar = view?.findViewById<ImageView>(R.id.ivAvatar)
                ivAvatar?.let {
                    applyAvatarSafely(it, uri.toString())
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActions(view)
        setupSettings(view)

        loadCachedProfile(view)
        loadQuickStats(view)
        loadAchievements(view)
        loadTrainingInsights(view)
        loadRecentActivity(view)

        fetchProfileFromServer()
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            loadCachedProfile(it)
            loadQuickStats(it)
            loadAchievements(it)
            loadTrainingInsights(it)
            loadRecentActivity(it)
        }
        fetchProfileFromServer()
    }

    private fun setupActions(view: View) {
        view.findViewById<View>(R.id.btnChangeAvatar).setOnClickListener {
            avatarPicker.launch(arrayOf("image/*"))
        }

        view.findViewById<MaterialButton>(R.id.btnEditProfile).setOnClickListener {
            if (isGuestMode()) {
                // Guests have no server account, so editing prefs is pointless.
                // Send them to sign-in instead.
                Toast.makeText(
                    requireContext(),
                    "Please sign in to edit your profile",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                return@setOnClickListener
            }
            showEditProfileDialog()
        }

        // Form Stats card — opens the chart + zone-distribution screen.
        view.findViewById<View>(R.id.cardViewFormStats)?.setOnClickListener {
            if (isGuestMode()) {
                Toast.makeText(
                    requireContext(),
                    "Sign in to see your form stats",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            startActivity(
                Intent(requireContext(), com.example.fitnesscoachai.ui.stats.StatsActivity::class.java)
            )
        }
    }

    private fun getAuthPrefs() =
        requireContext().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)

    private fun getProfilePrefs() =
        requireContext().getSharedPreferences("user_profile", AppCompatActivity.MODE_PRIVATE)

    private fun getCurrentUserId(): Int {
        return getAuthPrefs().getInt("user_id", -1)
    }

    private fun getAvatarUriKey(): String {
        val userId = getCurrentUserId()
        return if (userId > 0) "avatar_uri_$userId" else "avatar_uri_guest"
    }

    private fun getAccessToken(): String? {
        // Source of truth is now the encrypted TokenStore. Falls back to the
        // legacy unencrypted pref for backward-compat during upgrade window.
        return com.example.fitnesscoachai.data.api.RetrofitClient
            .tokenStore()
            .accessToken
            ?: getAuthPrefs().getString("access_token", null)
    }

    private fun clearBrokenAvatarUri() {
        getProfilePrefs()
            .edit()
            .remove(getAvatarUriKey())
            .apply()
    }

    private fun resetAvatarView(ivAvatar: ImageView) {
        ivAvatar.setImageDrawable(null)
        ivAvatar.background = null
        ivAvatar.clearColorFilter()
        ivAvatar.imageTintList = null
        ivAvatar.invalidate()
    }

    private fun applyAvatarSafely(ivAvatar: ImageView, avatarUriString: String?) {
        if (avatarUriString.isNullOrBlank()) {
            resetAvatarView(ivAvatar)
            return
        }

        try {
            val uri = Uri.parse(avatarUriString)

            requireContext().contentResolver.openInputStream(uri)?.use {
                // validate access
            } ?: throw IllegalStateException("Avatar stream is null")

            ivAvatar.setImageURI(uri)
            ivAvatar.background = null
            ivAvatar.clearColorFilter()
            ivAvatar.imageTintList = null
            ivAvatar.invalidate()

        } catch (e: SecurityException) {
            Log.w(tag, "Нет доступа к avatar uri: $avatarUriString", e)
            clearBrokenAvatarUri()
            resetAvatarView(ivAvatar)
        } catch (e: Exception) {
            Log.w(tag, "Битый avatar uri: $avatarUriString", e)
            clearBrokenAvatarUri()
            resetAvatarView(ivAvatar)
        }
    }

    private fun isGuestMode(): Boolean {
        return getAuthPrefs().getBoolean("isGuest", false)
    }

    private fun fetchProfileFromServer() {
        // Guests have no account on the server — don't hit /me or the response
        // could carry stale data from a previous logged-in session and clobber
        // the guest placeholder.
        if (isGuestMode()) return

        val token = getAccessToken() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getMe("Bearer $token")
                }

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        cacheUser(user)
                        view?.let { updateProfileUI(it, user) }
                    } else {
                        Log.e(tag, "GET /me success but body is null")
                    }
                } else {
                    Log.e(tag, "GET /me failed: ${response.code()} ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(tag, "GET /me exception", e)
            }
        }
    }

    private fun cacheUser(user: User) {
        val authPrefs = getAuthPrefs()
        val profilePrefs = getProfilePrefs()
        val userId = getCurrentUserId()

        authPrefs.edit()
            .putString("user_name", user.username)
            .putString("user_email", user.email)
            .apply()

        profilePrefs.edit()
            .putInt("age_$userId", user.age ?: 25)
            .putFloat("weight_$userId", user.weight ?: 75f)
            .putFloat("height_$userId", user.height ?: 180f)
            .putString("fitness_level_$userId", user.fitness_level)
            .putString("training_goal_$userId", user.goal.ifBlank { "" })
            .putString("injuries_$userId", user.limitations.ifBlank { "" })
            .putString("training_frequency_$userId", user.frequency.ifBlank { "" })
            .putString("workout_duration_$userId", user.workout_duration.ifBlank { "" })
            .putString("workout_place_$userId", user.workout_place.ifBlank { "" })
            .putString("endurance_level_$userId", user.endurance_level.ifBlank { "" })
            .putString("gender_$userId", user.gender.ifBlank { "" })
            .putString("profile_picture_url_$userId", user.profile_picture_url ?: "")
            .apply()
    }

    private fun loadCachedProfile(view: View) {
        // Guests are not real accounts — render an empty placeholder profile
        // instead of reading SharedPreferences, which could still carry
        // leftover keys from a previous logged-in session.
        if (isGuestMode()) {
            updateProfileUI(view, buildGuestPlaceholder())
            return
        }

        val authPrefs = getAuthPrefs()
        val profilePrefs = getProfilePrefs()
        val userId = getCurrentUserId()

        val cachedUser = User(
            id = userId.coerceAtLeast(0),
            email = authPrefs.getString("user_email", "") ?: "",
            username = authPrefs.getString("user_name", "Athlete") ?: "Athlete",
            age = profilePrefs.getInt("age_$userId", 25),
            weight = profilePrefs.getFloat("weight_$userId", 75f),
            height = profilePrefs.getFloat("height_$userId", 180f),
            fitness_level = profilePrefs.getString("fitness_level_$userId", "beginner") ?: "beginner",
            goal = profilePrefs.getString("training_goal_$userId", "") ?: "",
            limitations = profilePrefs.getString("injuries_$userId", "") ?: "",
            frequency = profilePrefs.getString("training_frequency_$userId", "") ?: "",
            workout_duration = profilePrefs.getString("workout_duration_$userId", "") ?: "",
            workout_place = profilePrefs.getString("workout_place_$userId", "") ?: "",
            endurance_level = profilePrefs.getString("endurance_level_$userId", "") ?: "",
            gender = profilePrefs.getString("gender_$userId", "") ?: "",
            profile_picture_url = profilePrefs.getString("profile_picture_url_$userId", "")?.ifBlank { null }
        )

        updateProfileUI(view, cachedUser)
    }

    /**
     * Placeholder profile used while the user is in guest mode. All fields are
     * empty so updateProfileUI shows clean "Not specified" / "No goal set yet"
     * defaults, never another user's leaked data.
     */
    private fun buildGuestPlaceholder(): User {
        return User(
            id = 0,
            email = "",
            username = "Guest",
            age = null,
            weight = null,
            height = null,
            fitness_level = "",
            goal = "",
            limitations = "",
            frequency = "",
            workout_duration = "",
            workout_place = "",
            endurance_level = "",
            gender = "",
            profile_picture_url = null,
        )
    }

    private fun updateProfileUI(view: View, user: User) {
        val settingsPrefs =
            requireContext().getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val units = settingsPrefs.getString("units", "kg / cm") ?: "kg / cm"

        val guest = isGuestMode()

        val userName = user.username.ifBlank { if (guest) "Guest" else "User" }
        val levelText = formatFitnessLevel(user.fitness_level)
        val goalText = user.goal.ifBlank { "No goal set yet" }
        val subtitle = when {
            guest -> "Sign in to personalize"
            levelText.isNotBlank() -> "Fitness Level: $levelText"
            else -> "Fitness Profile"
        }

        view.findViewById<TextView>(R.id.tvUserName).text = userName
        view.findViewById<TextView>(R.id.tvUserSubtitle).text = subtitle
        view.findViewById<TextView>(R.id.tvProfileGoal).text =
            if (guest) "Goal: Sign in to set" else "Goal: $goalText"

        val ivAvatar = view.findViewById<ImageView>(R.id.ivAvatar)
        val localAvatarUri = getProfilePrefs().getString(getAvatarUriKey(), null)
        applyAvatarSafely(ivAvatar, localAvatarUri)

        view.findViewById<TextView>(R.id.tvAge).text =
            user.age?.toString() ?: if (guest) "—" else "25"

        view.findViewById<TextView>(R.id.tvGender).text =
            user.gender.ifBlank { if (guest) "—" else "Not specified" }

        val weightKg = user.weight
        val heightCm = user.height

        if (weightKg == null || heightCm == null) {
            view.findViewById<TextView>(R.id.tvWeight).text = if (guest) "—" else "Not specified"
            view.findViewById<TextView>(R.id.tvHeight).text = if (guest) "—" else "Not specified"
        } else if (units == "lb / ft") {
            val weightLb = weightKg * 2.20462f
            val totalInches = heightCm / 2.54f
            val feet = (totalInches / 12f).toInt()
            val inches = (totalInches % 12f).toInt()

            view.findViewById<TextView>(R.id.tvWeight).text = "${weightLb.toInt()} lb"
            view.findViewById<TextView>(R.id.tvHeight).text = "${feet} ft ${inches} in"
        } else {
            view.findViewById<TextView>(R.id.tvWeight).text = "${weightKg.toInt()} kg"
            view.findViewById<TextView>(R.id.tvHeight).text = "${heightCm.toInt()} cm"
        }

        view.findViewById<TextView>(R.id.tvTrainingFrequency).text =
            user.frequency.ifBlank { if (guest) "—" else "Not specified" }

        view.findViewById<TextView>(R.id.tvWorkoutDuration).text =
            user.workout_duration.ifBlank { if (guest) "—" else "Not specified" }

        view.findViewById<TextView>(R.id.tvLimitations).text =
            user.limitations.ifBlank { if (guest) "—" else "No Limitations" }
    }

    private fun formatFitnessLevel(level: String?): String {
        return when (level?.lowercase(Locale.getDefault())) {
            "beginner" -> "Beginner"
            "intermediate" -> "Intermediate"
            "advanced" -> "Advanced"
            else -> level?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: ""
        }
    }

    private fun loadQuickStats(view: View) {
        val ctx = requireContext()
        val workoutPrefs = ctx.getSharedPreferences("workout_history", AppCompatActivity.MODE_PRIVATE)
        val store = com.example.fitnesscoachai.data.local.WorkoutHistoryStore
        val historyCount = store.getCount(ctx)

        view.findViewById<TextView>(R.id.tvTotalWorkouts).text = historyCount.toString()
        view.findViewById<TextView>(R.id.tvStatWorkoutsValue).text = historyCount.toString()

        var totalReps = 0
        for (i in 0 until historyCount) {
            totalReps += workoutPrefs.getInt(store.repsKey(ctx, i), 0)
        }
        view.findViewById<TextView>(R.id.tvTotalReps).text = totalReps.toString()

        // Initial state: dash until the network call below replaces it. We
        // ask the server because form score is computed across all devices,
        // not from the on-device history slice.
        view.findViewById<TextView>(R.id.tvAvgFormScore).text = "—"
        view.findViewById<TextView>(R.id.tvStatFormValue).text = "—"

        val streak = calculateStreak(workoutPrefs, historyCount)
        view.findViewById<TextView>(R.id.tvStatStreakValue).text = streak.toString()

        // Fetch the server-side avg_form_score. Guests don't have an account,
        // so skip the request and leave the dash showing.
        if (!isGuestMode()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = com.example.fitnesscoachai.data.api.RetrofitClient
                        .apiService.getWorkoutStats()
                    if (response.isSuccessful) {
                        val body = response.body() ?: return@launch
                        val avg = body.average_form_score
                        val scored = body.form_score_history.size
                        if (scored > 0 && avg > 0f) {
                            view.findViewById<TextView>(R.id.tvAvgFormScore)
                                ?.text = "${avg.toInt()}%"
                            view.findViewById<TextView>(R.id.tvStatFormValue)
                                ?.text = "${avg.toInt()}%"
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "stats fetch failed in Profile", e)
                }
            }
        }
    }

    private fun loadAchievements(view: View) {
        val llAchievements = view.findViewById<LinearLayout>(R.id.llAchievements)
        llAchievements.removeAllViews()

        val ctx = requireContext()
        val workoutPrefs = ctx.getSharedPreferences("workout_history", AppCompatActivity.MODE_PRIVATE)
        val historyCount = com.example.fitnesscoachai.data.local.WorkoutHistoryStore.getCount(ctx)

        val achievements = mutableListOf<String>()
        if (historyCount >= 1) achievements.add("First workout completed")
        if (historyCount >= 5) achievements.add("5 workouts done")

        val streak = calculateStreak(workoutPrefs, historyCount)
        if (streak > 0) achievements.add("Consistency streak: $streak days")

        if (achievements.isEmpty()) {
            llAchievements.addView(createSecondaryText("Complete your first workout to unlock achievements!"))
        } else {
            achievements.forEach { text ->
                llAchievements.addView(createPrimaryListText("✓ $text"))
            }
        }
    }

    private fun calculateStreak(prefs: android.content.SharedPreferences, historyCount: Int): Int {
        if (historyCount == 0) return 0
        val ctx = requireContext()
        val store = com.example.fitnesscoachai.data.local.WorkoutHistoryStore

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dates = mutableListOf<Long>()
        for (i in 0 until historyCount) {
            val date = prefs.getLong(store.dateKey(ctx, i), 0)
            if (date > 0) dates.add(date)
        }

        dates.sortDescending()

        var streak = 0
        var currentDate = today
        val oneDay = 24 * 60 * 60 * 1000L

        dates.forEach { date ->
            val normalized = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (normalized == currentDate || normalized == currentDate - oneDay) {
                if (normalized == currentDate - oneDay) currentDate = normalized
                streak++
            } else {
                return streak
            }
        }

        return streak
    }

    private fun loadTrainingInsights(view: View) {
        val ctx = requireContext()
        val workoutPrefs = ctx.getSharedPreferences("workout_history", AppCompatActivity.MODE_PRIVATE)
        val store = com.example.fitnesscoachai.data.local.WorkoutHistoryStore
        val historyCount = store.getCount(ctx)

        // "Most common mistake" needs per-rep AI feedback aggregation, which
        // we don't store yet. Keep honest: dash if no data, generic prompt
        // otherwise. Hard-coded "Knees moving inward" was misleading.
        view.findViewById<TextView>(R.id.tvCommonMistake).text =
            if (historyCount > 0) "More data needed" else "No data yet"

        val exerciseCounts = mutableMapOf<String, Int>()
        for (i in 0 until historyCount) {
            val exercise = workoutPrefs.getString(store.exerciseKey(ctx, i), null)
            if (exercise != null) {
                exerciseCounts[exercise] = exerciseCounts.getOrDefault(exercise, 0) + 1
            }
        }

        view.findViewById<TextView>(R.id.tvBestExercise).text =
            exerciseCounts.maxByOrNull { it.value }?.key ?: "No data yet"

        // AI accuracy needs a separate evaluation pipeline (offline tests
        // against labeled poses). Showing "92%" without that pipeline was
        // fabricated. Dash until we plug real numbers in.
        view.findViewById<TextView>(R.id.tvAIAccuracy).text = "—"
    }

    private fun loadRecentActivity(view: View) {
        val llRecentWorkouts = view.findViewById<LinearLayout>(R.id.llRecentWorkouts)
        llRecentWorkouts.removeAllViews()

        val ctx = requireContext()
        val workoutPrefs = ctx.getSharedPreferences("workout_history", AppCompatActivity.MODE_PRIVATE)
        val store = com.example.fitnesscoachai.data.local.WorkoutHistoryStore
        val historyCount = store.getCount(ctx)

        if (historyCount == 0) {
            llRecentWorkouts.addView(createSecondaryText("No workouts yet"))
            return
        }

        val workouts = mutableListOf<Triple<String, Int, Long>>()
        for (i in 0 until historyCount) {
            val exercise = workoutPrefs.getString(store.exerciseKey(ctx, i), null)
            val reps = workoutPrefs.getInt(store.repsKey(ctx, i), 0)
            val date = workoutPrefs.getLong(store.dateKey(ctx, i), 0)
            if (exercise != null && date > 0) workouts.add(Triple(exercise, reps, date))
        }

        workouts.sortByDescending { it.third }
        val recentWorkouts = workouts.take(3)

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        recentWorkouts.forEach { (exercise, reps, date) ->
            val workoutDate = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dateText = when {
                workoutDate == today -> "Today"
                workoutDate == today - 24 * 60 * 60 * 1000L -> "Yesterday"
                else -> dateFormat.format(Date(date))
            }

            llRecentWorkouts.addView(createPrimaryListText("$exercise · $reps reps · $dateText"))
        }

        view.findViewById<MaterialButton>(R.id.btnViewFullHistory).setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }
    }

    private fun setupSettings(view: View) {
        val prefs = requireContext().getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val currentTheme = prefs.getString("theme_mode", "system") ?: "system"

        view.findViewById<TextView>(R.id.tvTheme).text = when (currentTheme) {
            "light" -> "Light"
            "dark" -> "Dark"
            else -> "System"
        }

        view.findViewById<TextView>(R.id.tvUnits).text =
            prefs.getString("units", "kg / cm") ?: "kg / cm"

        view.findViewById<View>(R.id.llTheme).setOnClickListener {
            showThemeDialog()
        }

        view.findViewById<View>(R.id.llUnits).setOnClickListener {
            showUnitsDialog()
        }

        view.findViewById<View>(R.id.llLogout).setOnClickListener {
            HomeFragment.clearCache()

            // Wipe encrypted tokens AND legacy session flags.
            com.example.fitnesscoachai.data.api.RetrofitClient.tokenStore().clear()
            requireActivity()
                .getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            // NOTE: we intentionally do NOT clear `user_profile` or
            // `workout_history` prefs here. Those are already user-scoped
            // (avatar_uri_<userId>, age_<userId>, history_count_<userId>),
            // so a different user logging in reads their own data, and the
            // same user logging back in keeps their avatar + workout history.
            // Wiping them was deleting the avatar on every re-login.

            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun showThemeDialog() {
        val items = arrayOf("System", "Light", "Dark")
        val values = arrayOf("system", "light", "dark")
        val prefs = requireContext().getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val current = prefs.getString("theme_mode", "system") ?: "system"
        val checked = values.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(items, checked) { dialog, which ->
                val value = values[which]
                prefs.edit().putString("theme_mode", value).apply()

                when (value) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }

                dialog.dismiss()
            }
            .show()
    }

    private fun showUnitsDialog() {
        val items = arrayOf("kg / cm", "lb / ft")
        val prefs = requireContext().getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val current = prefs.getString("units", "kg / cm") ?: "kg / cm"
        val checked = items.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Units")
            .setSingleChoiceItems(items, checked) { dialog, which ->
                prefs.edit().putString("units", items[which]).apply()
                view?.findViewById<TextView>(R.id.tvUnits)?.text = items[which]
                view?.let { currentView ->
                    loadCachedProfile(currentView)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showEditProfileDialog() {
        val authPrefs = getAuthPrefs()
        val prefs = getProfilePrefs()
        val settingsPrefs = requireContext().getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val userId = getCurrentUserId()

        val units = settingsPrefs.getString("units", "kg / cm") ?: "kg / cm"
        val isImperial = units == "lb / ft"
        val ctx = requireContext()

        val fieldParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 16
        }

        val weightKg = prefs.getFloat("weight_$userId", 75f)
        val heightCm = prefs.getFloat("height_$userId", 180f)

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val tilName = TextInputLayout(ctx).apply {
            hint = "Name"
            layoutParams = fieldParams
            isErrorEnabled = true
            addView(TextInputEditText(ctx).apply {
                setText(authPrefs.getString("user_name", "") ?: "")
            })
        }
        val etName = tilName.editText!!

        val tilAge = TextInputLayout(ctx).apply {
            hint = "Age"
            layoutParams = fieldParams
            isErrorEnabled = true
            addView(TextInputEditText(ctx).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(prefs.getInt("age_$userId", 25).toString())
            })
        }
        val etAge = tilAge.editText!!

        val tilWeight = TextInputLayout(ctx).apply {
            hint = "Weight"
            suffixText = if (isImperial) " lb" else " kg"
            layoutParams = fieldParams
            isErrorEnabled = true
            addView(TextInputEditText(ctx).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(
                    if (isImperial) (weightKg * 2.20462f).toString()
                    else weightKg.toString()
                )
            })
        }
        val etWeight = tilWeight.editText!!

        val tilHeight = TextInputLayout(ctx).apply {
            hint = "Height"
            suffixText = if (isImperial) " in" else " cm"
            layoutParams = fieldParams
            isErrorEnabled = true
            addView(TextInputEditText(ctx).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(
                    if (isImperial) (heightCm / 2.54f).toString()
                    else heightCm.toInt().toString()
                )
            })
        }
        val etHeight = tilHeight.editText!!

        container.addView(tilName)
        container.addView(tilAge)
        container.addView(tilWeight)
        container.addView(tilHeight)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Edit Personal Data")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        tilName.error = null
                        tilAge.error = null
                        tilWeight.error = null
                        tilHeight.error = null

                        val name = etName.text?.toString()?.trim().orEmpty()
                        val ageStr = etAge.text?.toString()?.trim().orEmpty()
                        val weightStr = etWeight.text?.toString()?.trim().orEmpty()
                        val heightStr = etHeight.text?.toString()?.trim().orEmpty()

                        var hasError = false

                        if (name.isBlank()) {
                            tilName.error = "Enter your name"
                            hasError = true
                        }

                        val age = ageStr.toIntOrNull()
                        if (age == null || age !in 10..120) {
                            tilAge.error = "Age must be between 10 and 120"
                            hasError = true
                        }

                        val weightVal: Float
                        val heightVal: Float

                        if (isImperial) {
                            val weightLb = weightStr.toFloatOrNull()
                            if (weightLb == null || weightLb !in 44f..660f) {
                                tilWeight.error = "Weight must be between 44 and 660 lb"
                                hasError = true
                            }

                            val heightIn = heightStr.toFloatOrNull()
                            if (heightIn == null || heightIn !in 39f..98f) {
                                tilHeight.error = "Height must be between 39 and 98 in"
                                hasError = true
                            }

                            weightVal = (weightLb ?: 165f) / 2.20462f
                            heightVal = (heightIn ?: 70f) * 2.54f
                        } else {
                            val weight = weightStr.toFloatOrNull()
                            if (weight == null || weight !in 20f..300f) {
                                tilWeight.error = "Weight must be between 20 and 300 kg"
                                hasError = true
                            }

                            val height = heightStr.toFloatOrNull()
                            if (height == null || height !in 100f..250f) {
                                tilHeight.error = "Height must be between 100 and 250 cm"
                                hasError = true
                            }

                            weightVal = weight ?: 75f
                            heightVal = height ?: 180f
                        }

                        if (hasError) return@setOnClickListener

                        updateProfileOnServer(
                            dialog = dialog,
                            username = name,
                            age = age ?: 25,
                            weight = weightVal,
                            height = heightVal
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun updateProfileOnServer(
        dialog: androidx.appcompat.app.AlertDialog,
        username: String,
        age: Int,
        weight: Float,
        height: Float
    ) {
        val token = getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Authorization token not found", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getProfilePrefs()
        val userId = getCurrentUserId()

        val request = UpdateProfileRequest(
            username = username,
            age = age,
            weight = weight,
            height = height,
            fitness_level = prefs.getString("fitness_level_$userId", null),
            goal = prefs.getString("training_goal_$userId", null),
            limitations = prefs.getString("injuries_$userId", null),
            frequency = prefs.getString("training_frequency_$userId", null),
            workout_duration = prefs.getString("workout_duration_$userId", null),
            workout_place = prefs.getString("workout_place_$userId", null),
            endurance_level = prefs.getString("endurance_level_$userId", null),
            gender = prefs.getString("gender_$userId", null)
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.updateMe("Bearer $token", request)
                }

                if (response.isSuccessful) {
                    val updatedUser = response.body()
                    if (updatedUser != null) {
                        cacheUser(updatedUser)
                        view?.let { updateProfileUI(it, updatedUser) }
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Empty server response", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(tag, "PATCH /me failed: ${response.code()} $errorBody")
                    Toast.makeText(
                        requireContext(),
                        "Update failed: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(tag, "PATCH /me exception", e)
                Toast.makeText(
                    requireContext(),
                    "Network error while updating profile",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createPrimaryListText(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
    }

    private fun createSecondaryText(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }
    }
}