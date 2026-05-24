package com.example.fitnesscoachai.data.repo

import com.example.fitnesscoachai.domain.catalog.CatalogTree
import com.example.fitnesscoachai.domain.model.Exercise
import com.example.fitnesscoachai.domain.model.ExerciseMedia
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.domain.model.SubCategory
import com.example.fitnesscoachai.domain.repo.ExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Локальная реализация репозитория.
 * Сейчас — заглушка из памяти. Позже: загрузка из assets/exercises.json или Room.
 */
class ExerciseRepositoryLocal : ExerciseRepository {

    private val exercises = mutableListOf<Exercise>().apply {
        addAll(seedExercises())
    }

    fun countBySubId(subId: String): Int =
        exercises.count { it.sub.id == subId }

    override suspend fun getExercisesBySubCategory(subId: String): List<Exercise> =
        withContext(Dispatchers.Default) {
            exercises.filter { it.sub.id == subId }
        }

    override suspend fun getExercisesByMainCategory(main: MainCategory): List<Exercise> =
        withContext(Dispatchers.Default) {
            exercises.filter { it.main == main }
        }

    override suspend fun getExerciseById(id: String): Exercise? =
        withContext(Dispatchers.Default) {
            exercises.find { it.id == id }
        }

    private fun seedExercises(): List<Exercise> {
        val subs = CatalogTree.subCategories
        return listOf(
            // =====================
            // ARMS — BICEPS
            // =====================
            sub(subs, "biceps", "ex1", "Barbell Curl",
                "Standing barbell curl to build overall biceps mass.",
                listOf(
                    "Stand tall with a barbell held in an underhand grip.",
                    "Keep elbows close to your sides.",
                    "Curl the bar toward your shoulders.",
                    "Lower slowly to the starting position."
                ),
                listOf("Do not swing your body.", "Keep upper arms fixed."),
                "barbell", "beginner", null, videoFile = "barbell_curl", imageFile = "barbell_curl"),
            sub(subs, "biceps", "ex2", "Hammer Curl",
                "Neutral-grip dumbbell curl targeting biceps and brachialis.",
                listOf(
                    "Hold dumbbells at your sides with palms facing each other.",
                    "Curl both dumbbells toward your shoulders.",
                    "Pause briefly at the top.",
                    "Lower the dumbbells under control."
                ),
                listOf("Avoid using momentum.", "Keep wrists neutral."),
                "dumbbell", "beginner", null, videoFile = "hammer_curl", imageFile = "hammer_curl"),
            sub(subs, "biceps", "ex62", "Dumbbell Curl",
                "Classic alternating or simultaneous dumbbell curl for biceps.",
                listOf(
                    "Stand with a dumbbell in each hand, palms facing forward.",
                    "Curl one or both dumbbells toward your shoulders.",
                    "Keep elbows at your sides.",
                    "Lower the weight slowly."
                ),
                listOf("Do not let shoulders roll forward.", "Use full but controlled range."),
                "dumbbell", "beginner", null, videoFile = "dumbbell_curl", imageFile = "dumbbell_curl"),
            sub(subs, "biceps", "ex63", "Incline Dumbbell Curl",
                "Seated incline curl emphasizing the long head of the biceps.",
                listOf(
                    "Set an incline bench to 45–60° and sit back.",
                    "Let arms hang straight down with dumbbells in hand.",
                    "Curl the weights without letting shoulders roll forward.",
                    "Lower slowly to a full stretch."
                ),
                listOf("Keep upper arms vertical.", "Do not swing the weights."),
                "dumbbell", "intermediate", null, videoFile = "incline_dumbbell_curl", imageFile = "incline_dumbbell_curl"),
            sub(subs, "biceps", "ex64", "Concentration Curl",
                "Single-arm curl with elbow braced on inner thigh for strict form.",
                listOf(
                    "Sit on a bench and place elbow of working arm on inner thigh.",
                    "Hold a dumbbell with arm extended toward the floor.",
                    "Curl the dumbbell toward your shoulder.",
                    "Squeeze at the top and lower under control."
                ),
                listOf("Avoid moving your upper arm.", "Focus on a strong peak contraction."),
                "dumbbell", "beginner", null, videoFile = "concentration_curl", imageFile = "concentration_curl"),
            sub(subs, "biceps", "ex65", "Cable Curl",
                "Standing curl using a low cable for constant tension.",
                listOf(
                    "Attach a straight bar or EZ handle to a low pulley.",
                    "Stand facing the machine, elbows close to sides.",
                    "Curl the bar toward your shoulders.",
                    "Lower slowly until arms are almost straight."
                ),
                listOf("Keep torso upright.", "Control both up and down phases."),
                "cable", "beginner", null, videoFile = "cable_curl", imageFile = "cable_curl"),
            sub(subs, "biceps", "ex66", "Preacher Curl",
                "Curl performed on a preacher bench to reduce cheating.",
                listOf(
                    "Sit at the preacher bench with upper arms resting on the pad.",
                    "Hold a barbell or EZ bar with underhand grip.",
                    "Curl the weight toward your shoulders.",
                    "Lower until arms are almost straight without locking elbows."
                ),
                listOf("Do not bounce out of the bottom position.", "Use a weight that allows control."),
                "barbell / EZ bar", "intermediate", null, videoFile = "preacher_curl", imageFile = "preacher_curl"),
            sub(subs, "biceps", "ex67", "Chin-up (Biceps Emphasis)",
                "Underhand-grip chin-up emphasizing biceps as well as back.",
                listOf(
                    "Grab the bar with an underhand, shoulder-width grip.",
                    "Hang with arms extended and core tight.",
                    "Pull yourself up until chin clears the bar.",
                    "Lower slowly to full extension."
                ),
                listOf("Think about pulling with elbows and biceps.", "Avoid swinging or kipping."),
                "body weight", "intermediate", null, videoFile = "chin_up_biceps_emphasis", imageFile = "chin_up_biceps_emphasis"),

            // =====================
            // ARMS — TRICEPS
            // =====================
            sub(subs, "triceps", "ex3", "Tricep Pushdown",
                "Cable pushdown focusing on the triceps with elbows fixed at the sides.",
                listOf(
                    "Attach a straight bar or rope to a high pulley.",
                    "Stand tall with elbows close to your torso.",
                    "Extend your arms downward until elbows are straight.",
                    "Return the handle to about 90° elbow bend."
                ),
                listOf("Do not let elbows drift forward.", "Keep shoulders relaxed."),
                "cable", "beginner", null, videoFile = "tricep_pushdown", imageFile = "tricep_pushdown"),
            sub(subs, "triceps", "ex4", "Close-Grip Bench Press",
                "Narrow-grip bench press to load the triceps heavily.",
                listOf(
                    "Lie on the bench and grip the bar slightly narrower than shoulders.",
                    "Lower the bar toward lower chest or sternum.",
                    "Keep elbows close to your body.",
                    "Press the bar back up to full arm extension."
                ),
                listOf("Keep wrists stacked over elbows.", "Do not flare elbows out too wide."),
                "barbell", "intermediate", null, videoFile = "close_grip_bench_press", imageFile = "close_grip_bench_press"),
            sub(subs, "triceps", "ex68", "Overhead Tricep Extension",
                "Overhead extension with dumbbell or cable to train the long head of triceps.",
                listOf(
                    "Hold a dumbbell or cable handle overhead with both hands.",
                    "Keep upper arms close to your head.",
                    "Bend elbows to lower the weight behind your head.",
                    "Extend elbows to return to the starting position."
                ),
                listOf("Avoid arching your lower back.", "Keep elbows pointing mostly forward."),
                "dumbbell / cable", "beginner", null, videoFile = "overhead_tricep_extension", imageFile = "overhead_tricep_extension"),
            sub(subs, "triceps", "ex69", "Skull Crushers",
                "Lying tricep extension, often with an EZ bar, targeting all three heads.",
                listOf(
                    "Lie on a bench holding an EZ bar with arms extended above chest.",
                    "Bend elbows to lower the bar toward your forehead or slightly behind.",
                    "Keep upper arms mostly vertical.",
                    "Extend elbows back to the starting position."
                ),
                listOf("Use a spotter with heavy loads.", "Move only at the elbows, not shoulders."),
                "EZ bar / dumbbell", "intermediate", null, videoFile = "skull_crushers", imageFile = "skull_crushers"),
            sub(subs, "triceps", "ex70", "Dips (Triceps Focus)",
                "Parallel bar or bench dips emphasizing triceps by keeping torso more upright.",
                listOf(
                    "Support yourself on parallel bars with arms straight.",
                    "Keep torso relatively upright and elbows close to your sides.",
                    "Lower until elbows reach about 90°.",
                    "Press back up to full extension."
                ),
                listOf("Avoid dropping too deep if shoulders are sensitive.", "Use assistance if needed."),
                "body weight", "intermediate", null, videoFile = "dips_triceps_focus", imageFile = "dips_triceps_focus"),
            sub(subs, "triceps", "ex71", "Rope Pushdown",
                "Cable pushdown using a rope to allow more natural wrist position.",
                listOf(
                    "Attach a rope to a high pulley and grab with neutral grip.",
                    "Start with elbows at 90° and rope near chest.",
                    "Extend elbows to push rope down and slightly apart.",
                    "Return to the start with control."
                ),
                listOf("Keep upper arms fixed.", "Do not let shoulders roll forward."),
                "cable", "beginner", null, videoFile = "rope_pushdown", imageFile = "rope_pushdown"),
            sub(subs, "triceps", "ex72", "Tricep Kickback",
                "Bent-over dumbbell kickback for triceps isolation.",
                listOf(
                    "Hinge forward with one knee and hand on a bench for support.",
                    "Hold a dumbbell with upper arm parallel to the floor.",
                    "Extend the elbow to straighten the arm behind you.",
                    "Return to the starting position with control."
                ),
                listOf("Keep upper arm fixed in place.", "Use light weight to avoid swinging."),
                "dumbbell", "beginner", null, videoFile = "tricep_kickback", imageFile = "tricep_kickback"),

            sub(subs, "triceps", "ex114", "Shoulder Press",
                "Overhead pressing movement that builds shoulder and upper-body strength.",
                listOf(
                    "Stand or sit tall with weights at shoulder height.",
                    "Brace your core and keep wrists stacked over elbows.",
                    "Press the weights overhead until arms are extended.",
                    "Lower the weights back to shoulder height with control."
                ),
                listOf(
                    "Do not over-arch your lower back.",
                    "Keep elbows slightly in front of the body.",
                    "Control both the press and the lowering phase."
                ),
                "dumbbell / barbell", "intermediate", null,
                videoFile = "shoulder_press",
                imageFile = "shoulder_press"
            ),
            // =====================
            // ARMS — FOREARMS
            // =====================
            sub(subs, "forearms", "ex5", "Wrist Curl",
                "Seated wrist curl targeting the wrist flexors.",
                listOf(
                    "Sit on a bench with forearms resting on your thighs, palms up.",
                    "Let wrists extend so the bar rolls toward your fingers.",
                    "Curl wrists up to bring the bar toward your forearms.",
                    "Lower slowly back to the stretch position."
                ),
                listOf("Use a controlled range of motion.", "Do not bounce the bar."),
                "barbell", "beginner", null, videoFile = "wrist_curl", imageFile = "wrist_curl"),
            sub(subs, "forearms", "ex73", "Reverse Wrist Curl",
                "Reverse-grip wrist curl focusing on wrist extensors.",
                listOf(
                    "Sit with forearms on your thighs, palms facing down.",
                    "Hold a light barbell.",
                    "Extend wrists to lift the back of your hands toward you.",
                    "Lower slowly to the starting position."
                ),
                listOf("Keep forearms planted on your legs.", "Use lighter weight than normal wrist curls."),
                "barbell", "beginner", null, videoFile = "reverse_wrist_curl", imageFile = "reverse_wrist_curl"),
            sub(subs, "forearms", "ex74", "Farmer’s Carry",
                "Loaded carry that challenges grip, forearms and core.",
                listOf(
                    "Pick up a heavy pair of dumbbells or farmer’s handles.",
                    "Stand tall with shoulders packed down.",
                    "Walk for a set distance or time.",
                    "Set the weights down safely."
                ),
                listOf("Do not let shoulders shrug up toward ears.", "Maintain a steady, controlled walk."),
                "dumbbell / farmer’s handles", "intermediate", null, videoFile = "farmers_carry", imageFile = "farmers_carry"),
            sub(subs, "forearms", "ex75", "Plate Pinch",
                "Static grip exercise pinching weight plates together.",
                listOf(
                    "Grab two smooth plates together with fingers and thumb.",
                    "Stand tall with arms at your sides.",
                    "Hold for time without letting the plates slip.",
                    "Set down carefully when grip fails."
                ),
                listOf("Use lighter plates with smooth sides to start.", "Keep wrist neutral."),
                "weight plates", "intermediate", null, videoFile = "plate_pinch", imageFile = "plate_pinch"),
            sub(subs, "forearms", "ex76", "Dead Hang",
                "Hanging from a pull-up bar to build grip endurance.",
                listOf(
                    "Grab a pull-up bar with shoulder-width grip.",
                    "Hang with arms extended and shoulders engaged.",
                    "Hold for as long as possible with good form.",
                    "Drop down safely when grip gives out."
                ),
                listOf("Do not let shoulders shrug up uncontrolled.", "Avoid swinging."),
                "body weight", "beginner", null, videoFile = "dead_hang", imageFile = "dead_hang"),
            sub(subs, "forearms", "ex77", "Reverse Curl",
                "Curl with overhand grip emphasizing forearms and brachialis.",
                listOf(
                    "Stand holding a barbell or EZ bar with overhand grip.",
                    "Curl the bar toward your shoulders.",
                    "Keep elbows close to your body.",
                    "Lower slowly back to start."
                ),
                listOf("Use moderate weight to avoid wrist strain.", "Keep wrists straight, not bent."),
                "barbell / EZ bar", "beginner", null, videoFile = "reverse_curl", imageFile = "reverse_curl"),
            // =====================
            // LEGS — QUADS
            // =====================
            sub(subs, "quads", "ex6", "Squat",
                "Classic compound squat primarily targeting quads but also glutes and hips.",
                listOf(
                    "Place the bar on your upper back.",
                    "Stand with feet about shoulder-width apart.",
                    "Lower by bending knees and hips until thighs are at least parallel to the floor.",
                    "Drive through your feet to return to standing."
                ),
                listOf("Keep your chest up and back neutral.", "Do not let knees cave inward."),
                "barbell", "beginner", null, videoFile = "squat", imageFile = "squat"),
            sub(subs, "quads", "ex7", "Leg Extension",
                "Extend your legs at the knee in the machine.",
                listOf("Sit in the machine, shins under the pad.", "Extend your legs at the knees.", "Hold the top briefly.", "Lower the weight."),
                listOf("Do not lift your hips off the seat.", "Move smoothly."),
                "machine", "beginner", null, videoFile = "leg_extention", imageFile = "leg_extention"),
            sub(subs, "quads", "ex45", "Front Squat",
                "Front-loaded squat variation emphasizing the quads and upper back.",
                listOf(
                    "Rack the barbell across the front of your shoulders.",
                    "Keep elbows high and chest up.",
                    "Sit down between your hips, keeping torso upright.",
                    "Stand back up by driving through mid‑foot."
                ),
                listOf("Do not let elbows drop.", "Use lighter weight than the back squat at first."),
                "barbell", "intermediate", null, videoFile = "front_squat", imageFile = "front_squat"),
            sub(subs, "quads", "ex46", "Leg Press",
                "Machine leg press focusing on quad strength with back support.",
                listOf(
                    "Sit in the leg press machine and place feet shoulder-width on the platform.",
                    "Unlock the safety stops.",
                    "Lower the platform by bending knees to about 90°.",
                    "Press the platform back up without locking knees hard."
                ),
                listOf("Keep lower back against the pad.", "Do not let knees collapse inward."),
                "machine", "beginner", null, videoFile = "leg_press", imageFile = "leg_press"),
            sub(subs, "quads", "ex47", "Bulgarian Split Squat",
                "Single-leg squat variation with rear foot elevated, strongly loading the quads.",
                listOf(
                    "Stand in front of a bench and place back foot on it.",
                    "Step forward with the working leg.",
                    "Lower hips straight down, bending the front knee.",
                    "Press through the front foot to stand back up."
                ),
                listOf("Keep torso slightly forward but controlled.", "Use bodyweight first, then add dumbbells."),
                "body weight / dumbbell", "intermediate", null, videoFile = "bulgarian_split_squat", imageFile = "bulgarian_split_squat"),
            sub(subs, "quads", "ex48", "Step-up",
                "Stepping onto a box or bench to target quads and glutes.",
                listOf(
                    "Stand facing a box or bench.",
                    "Place one foot fully on the surface.",
                    "Drive through the front leg to stand on the box.",
                    "Step down under control and alternate legs."
                ),
                listOf("Do not push off too much with the back leg.", "Choose a box height you can control."),
                "body weight / dumbbell", "beginner", null, videoFile = "step_up", imageFile = "step_up"),
            sub(subs, "quads", "ex49", "Goblet Squat",
                "Squat holding a kettlebell or dumbbell at chest height.",
                listOf(
                    "Hold a kettlebell or dumbbell close to your chest.",
                    "Stand with feet slightly wider than shoulder width.",
                    "Sit between your hips, keeping elbows inside knees.",
                    "Drive back up to standing."
                ),
                listOf("Keep heels on the floor.", "Use this as a technique builder for deeper squats."),
                "kettlebell / dumbbell", "beginner", null, videoFile = "goblet_squat", imageFile = "goblet_squat"),
            sub(subs, "quads", "ex50", "Hack Squat",
                "Machine or barbell hack squat emphasizing the front of the thighs.",
                listOf(
                    "Position yourself in the hack squat machine with shoulders under pads.",
                    "Place feet slightly forward on the platform.",
                    "Lower by bending knees and hips.",
                    "Press back up, focusing on driving through quads."
                ),
                listOf("Keep knees tracking over toes.", "Control the movement at the bottom."),
                "machine", "intermediate", null, videoFile = "hack_squat", imageFile = "hack_squat"),

            // =====================
            // LEGS — HAMSTRINGS
            // =====================
            sub(subs, "hamstrings", "ex8", "Romanian Deadlift",
                "Hinge forward with a straight back. Stretches and loads the hamstrings.",
                listOf("Stand holding the bar.", "Hinge forward, pushing hips back.", "Lower the bar along your legs.", "Return to upright by driving through the hips."),
                listOf("Keep your back straight; do not round your lower back.", "Feel the stretch in the back of your legs."),
                "barbell", "intermediate", null, videoFile = "romanian_deadlift", imageFile = "romanian_deadlift"),
            sub(subs, "hamstrings", "ex51", "Lying Leg Curl",
                "Machine leg curl performed lying face down to isolate hamstrings.",
                listOf(
                    "Lie face down on the leg curl machine with ankles under the pad.",
                    "Brace your core and grip the handles.",
                    "Curl your heels toward your glutes.",
                    "Lower slowly back to the start."
                ),
                listOf("Do not lift hips off the pad.", "Control the lowering phase."),
                "machine", "beginner", null, videoFile = "lying_leg_curl", imageFile = "lying_leg_curl"),
            sub(subs, "hamstrings", "ex52", "Seated Leg Curl",
                "Seated machine curl that targets the hamstrings with hip flexed.",
                listOf(
                    "Sit in the machine and place lower legs behind the pad.",
                    "Adjust the backrest so your knees line up with the pivot.",
                    "Curl heels toward the seat.",
                    "Return slowly to full knee extension."
                ),
                listOf("Keep torso still against the back pad.", "Avoid using momentum."),
                "machine", "beginner", null, videoFile = "seated_leg_curl", imageFile = "seated_leg_curl"),
            sub(subs, "hamstrings", "ex53", "Glute Ham Raise",
                "Bodyweight or assisted raise emphasizing hamstrings and glutes.",
                listOf(
                    "Set up on a glute-ham developer with feet secured.",
                    "Start with torso upright and knees bent.",
                    "Lower torso forward by extending knees and hips.",
                    "Contract hamstrings and glutes to return to the top."
                ),
                listOf("Use assistance or partial range if needed.", "Maintain a neutral spine."),
                "body weight / machine", "advanced", null, videoFile = "glute_ham_raise", imageFile = "glute_ham_raise"),
            sub(subs, "hamstrings", "ex54", "Nordic Curl",
                "Partner or anchored hamstring curl from a tall-kneeling position.",
                listOf(
                    "Kneel on a pad with ankles secured under a heavy object or held by a partner.",
                    "Cross arms over chest or keep them by your sides.",
                    "Slowly lower your body toward the floor, resisting with hamstrings.",
                    "Use hands to catch yourself and push lightly to return if needed."
                ),
                listOf("Focus on slow lowering at first.", "Do not drop suddenly; protect your knees."),
                "body weight", "advanced", null, videoFile = "nordic_curl", imageFile = "nordic_curl"),

            // =====================
            // LEGS — GLUTES
            // =====================
            sub(subs, "glutes", "ex9", "Hip Thrust",
                "Drive your hips up from a seated position, upper back on a bench.",
                listOf("Sit on the floor with your back to the bench, bar on your hips.", "Press your upper back into the bench.", "Drive your hips up.", "Squeeze glutes at the top and lower."),
                listOf("Tuck your chin slightly.", "Do not over-arch your lower back."),
                "barbell", "beginner", null, videoFile = "hip_thrust", imageFile = "hip_thrust"),
            sub(subs, "glutes", "ex55", "Glute Bridge",
                "Floor bridge focusing on glutes with less load on the spine.",
                listOf(
                    "Lie on your back with knees bent and feet flat on the floor.",
                    "Brace your core and squeeze glutes to lift hips.",
                    "Hold briefly at the top.",
                    "Lower hips back down with control."
                ),
                listOf("Do not over-arch lower back.", "Press through heels, not toes."),
                "body weight", "beginner", null, videoFile = "glute_bridge", imageFile = "glute_bridge"),
            sub(subs, "glutes", "ex56", "Cable Kickback",
                "Standing hip extension with cable to isolate the glutes.",
                listOf(
                    "Attach an ankle cuff to a low cable.",
                    "Stand facing the machine, holding on for balance.",
                    "Extend the working leg back in a slight arc.",
                    "Pause and squeeze glute at the end range, then return."
                ),
                listOf("Avoid swinging your torso.", "Use light to moderate weight for control."),
                "cable", "beginner", null, videoFile = "cable_kickback", imageFile = "cable_kickback"),
            sub(subs, "glutes", "ex57", "Bulgarian Split Squat (Glutes)",
                "Rear-foot-elevated split squat variation with more emphasis on glutes.",
                listOf(
                    "Take a slightly longer step forward than in the quad-focused version.",
                    "Lean torso slightly forward while keeping spine neutral.",
                    "Lower until front thigh is near parallel.",
                    "Drive through front heel to stand back up."
                ),
                listOf("Think about pushing the floor away with your front heel.", "Choose load that allows stable control."),
                "body weight / dumbbell", "intermediate", null, videoFile = "bulgarian_split_squat_glutes", imageFile = "bulgarian_split_squat_glutes"),
            sub(subs, "glutes", "ex58", "Step-up (Glutes)",
                "Step-up variation focusing on driving through the heel to target glutes.",
                listOf(
                    "Use a slightly higher box or bench if mobility allows.",
                    "Place entire foot on the surface.",
                    "Drive through the heel and squeeze glute at the top.",
                    "Lower back down under control."
                ),
                listOf("Avoid pushing off with the trailing leg.", "Keep knee tracking over toes."),
                "body weight / dumbbell", "intermediate", null, videoFile = "step_up_glutes", imageFile = "step_up_glutes"),

            // =====================
            // LEGS — CALVES
            // =====================
            sub(subs, "calves", "ex10", "Standing Calf Raise",
                "Standing calf raise. Rise as high as you can on your toes.",
                listOf("Stand on a platform with balls of feet on the edge.", "Lower your heels below the platform.", "Rise up on your toes as high as possible.", "Lower back down."),
                listOf("Use full range at the bottom for a stretch.", "Pause at the top."),
                "body weight", "beginner", null, videoFile = "standing_calf_raise", imageFile = "standing_calf_raise"),
            sub(subs, "calves", "ex59", "Seated Calf Raise",
                "Seated calf raise focusing more on soleus muscle.",
                listOf(
                    "Sit in the seated calf raise machine with thighs under the pad.",
                    "Place balls of feet on the platform.",
                    "Lower heels toward the floor.",
                    "Raise heels as high as possible, then repeat."
                ),
                listOf("Do not bounce at the bottom.", "Control the top squeeze."),
                "machine", "beginner", null, videoFile = "seated_calf_raise", imageFile = "seated_calf_raise"),
            sub(subs, "calves", "ex60", "Donkey Calf Raise",
                "Hip‑hinged calf raise variation to increase calf stretch.",
                listOf(
                    "Hinge forward and support upper body on a bench or machine.",
                    "Place balls of feet on a platform.",
                    "Lower heels to feel a stretch.",
                    "Raise heels as high as you can."
                ),
                listOf("Keep hips fixed; movement comes from ankles.", "Use bodyweight or added load as appropriate."),
                "body weight / machine", "intermediate", null, videoFile = "donkey_calf_raise", imageFile = "donkey_calf_raise"),
            sub(subs, "calves", "ex61", "Single-Leg Calf Raise",
                "Unilateral standing calf raise to fix imbalances.",
                listOf(
                    "Stand on one foot on a step or platform.",
                    "Hold onto a support for balance.",
                    "Lower heel below the platform.",
                    "Press up onto the ball of the foot and repeat."
                ),
                listOf("Perform the same reps on each leg.", "Focus on full range of motion."),
                "body weight / dumbbell", "intermediate", null, videoFile = "single_leg_calf_raise", imageFile = "single_leg_calf_raise"),
            sub(subs, "lats", "ex11", "Pull-up",
                "Hang from the bar and pull until your chin is over the bar.",
                listOf("Grip the bar at shoulder width.", "Hang with arms fully extended.", "Pull yourself up, squeezing your shoulder blades.", "Lower yourself under control."),
                listOf("Do not swing.", "Engage your core."),
                "body weight", "intermediate", null, videoFile = "pullup", imageFile = "pullup"),
            sub(subs, "lats", "ex12", "Lat Pulldown",
                "Pull the bar down to your chest, squeezing your shoulder blades.",
                listOf("Sit down and secure your thighs.", "Grasp the bar with a wide grip.", "Pull to the upper chest.", "Return the bar upward."),
                listOf("Do not lean back too far.", "Elbows drive down and back."),
                "cable", "beginner", null, videoFile = "lat_pulldown", imageFile = "lat_pulldown"),
            sub(subs, "lower_back", "ex13", "Deadlift",
                "Lift the bar from the floor by extending your hips and knees.",
                listOf("Stand in front of the bar, feet hip-width apart.", "Bend down and grip the bar just outside your legs.", "Keep your back straight.", "Stand up by extending legs and hips."),
                listOf("Do not round your back.", "Keep the bar close to your legs."),
                "barbell", "intermediate", null, videoFile = "deadlift", imageFile = "deadlift"),
            sub(subs, "upper_back", "ex14", "Barbell Row",
                "Row the bar to your waist in a bent-over position. Drive elbows along your body.",
                listOf("Bend forward holding the bar.", "Pull the bar to your waist.", "Squeeze shoulder blades at the top.", "Lower the bar."),
                listOf("Back parallel or at an angle to the floor.", "Do not jerk your torso up."),
                "barbell", "beginner", null, videoFile = "barbell_row", imageFile = "barbell_row"),
            // =====================
            // CHEST — MID CHEST (основные базовые)
            // =====================
            sub(subs, "mid_chest", "ex15", "Bench Press",
                "Classic flat barbell bench press focusing on overall chest development.",
                listOf(
                    "Lie on a flat bench with your eyes under the bar.",
                    "Grip the bar slightly wider than shoulder width.",
                    "Unrack and lower the bar toward mid‑chest.",
                    "Press the bar back up until elbows are extended."
                ),
                listOf("Keep shoulder blades retracted and feet planted.", "Do not bounce the bar off your chest."),
                "barbell", "beginner", null, videoFile = "bench_press", imageFile = "bench_press"),
            sub(subs, "mid_chest", "ex16", "Push-up",
                "Bodyweight push-up emphasizing mid-chest on a flat body line.",
                listOf(
                    "Start in a high plank, hands under or slightly wider than shoulders.",
                    "Keep body in a straight line from head to heels.",
                    "Lower chest toward the floor, elbows at ~45°.",
                    "Push back up to the starting position."
                ),
                listOf("Do not let hips sag.", "Brace your core and keep neck neutral."),
                "body weight", "beginner", null, videoFile = "push_up", imageFile = "push_up"),
            // =====================
            // ABS — UPPER ABS
            // =====================
            sub(subs, "upper_abs", "ex17", "Crunch",
                "Classic floor crunch emphasizing the upper portion of the abs.",
                listOf(
                    "Lie on your back with knees bent and feet flat on the floor.",
                    "Place hands lightly behind your head or across your chest.",
                    "Lift your shoulder blades off the floor by curling your ribcage toward your pelvis.",
                    "Lower back down without fully relaxing."
                ),
                listOf("Do not pull on your neck.", "Exhale as you crunch up."),
                "body weight", "beginner", null, videoFile = "crunch", imageFile = "crunch"),
            sub(subs, "upper_abs", "ex78", "Cable Crunch",
                "Kneeling cable crunch to load the upper abs with external resistance.",
                listOf(
                    "Attach a rope handle to a high pulley.",
                    "Kneel facing the machine, holding the rope near your head.",
                    "Crunch your ribcage down toward your pelvis.",
                    "Return slowly to the starting position."
                ),
                listOf("Move through the spine, not the hips.", "Do not pull with your arms."),
                "cable", "intermediate", null, videoFile = "cable_crunch", imageFile = "cable_crunch"),
            sub(subs, "upper_abs", "ex79", "Sit-up",
                "Traditional sit-up moving through a larger range of motion for the upper abs.",
                listOf(
                    "Lie on your back with knees bent and feet anchored or flat.",
                    "Cross arms over your chest or place fingertips by your temples.",
                    "Sit up by curling your torso toward your thighs.",
                    "Lower back down with control."
                ),
                listOf("Avoid jerking or pulling the neck.", "Control the descent as much as the ascent."),
                "body weight", "beginner", null, videoFile = "sit_up", imageFile = "sit_up"),
            sub(subs, "upper_abs", "ex80", "Machine Crunch",
                "Crunch performed on an ab machine with adjustable resistance.",
                listOf(
                    "Sit in the ab crunch machine and set the pad comfortably on your chest or shoulders.",
                    "Brace your core and grip the handles if provided.",
                    "Crunch forward, bringing ribs toward pelvis.",
                    "Return slowly to the start without letting the weight stack slam."
                ),
                listOf("Adjust the seat so pivot lines up with your core.", "Use moderate weight and full control."),
                "machine", "beginner", null, videoFile = "machine_crunch", imageFile = "machine_crunch"),

            // =====================
            // ABS — LOWER ABS
            // =====================
            sub(subs, "lower_abs", "ex81", "Leg Raise",
                "Supine leg raise emphasizing the lower abs and hip flexors.",
                listOf(
                    "Lie on your back with legs straight and hands by your sides or under hips.",
                    "Lift legs toward the ceiling while keeping them mostly straight.",
                    "Pause briefly when legs are vertical.",
                    "Lower them toward the floor without letting your lower back arch excessively."
                ),
                listOf("Press your lower back gently into the floor.", "Use a comfortable range if hamstrings are tight."),
                "body weight", "intermediate", null, videoFile = "leg_raise", imageFile = "leg_raise"),
            sub(subs, "lower_abs", "ex82", "Hanging Knee Raise",
                "Hanging knee raise targeting lower abs from a pull-up bar.",
                listOf(
                    "Hang from a pull-up bar with a shoulder-width grip.",
                    "Brace your core and bring knees toward your chest.",
                    "Pause briefly at the top.",
                    "Lower legs back to the starting position with control."
                ),
                listOf("Avoid swinging; keep the movement strict.", "Use straps if grip is a limiting factor."),
                "body weight", "intermediate", null, videoFile = "hanging_knee_raise", imageFile = "hanging_knee_raise"),
            sub(subs, "lower_abs", "ex83", "Reverse Crunch",
                "Reverse crunch focusing on curling the pelvis toward the ribcage.",
                listOf(
                    "Lie on your back with hips and knees bent at 90°.",
                    "Arms can be by your sides or holding onto a stable object.",
                    "Curl your pelvis up to lift hips slightly off the floor.",
                    "Lower slowly back to the starting position."
                ),
                listOf("Do not swing your legs.", "Think about rolling the spine off the mat segment by segment."),
                "body weight", "beginner", null, videoFile = "reverse_crunch", imageFile = "reverse_crunch"),
            sub(subs, "lower_abs", "ex84", "Flutter Kicks",
                "Alternating leg kicks performed just above the floor to challenge lower abs.",
                listOf(
                    "Lie on your back with legs extended.",
                    "Lift both heels a few inches off the floor.",
                    "Quickly alternate small up-and-down kicks with each leg.",
                    "Maintain a steady breathing pattern throughout."
                ),
                listOf("Keep lower back from arching excessively.", "Use shorter sets to maintain quality."),
                "body weight", "beginner", null, videoFile = "flutter_kicks", imageFile = "flutter_kicks"),

            // =====================
            // ABS — OBLIQUES
            // =====================
            sub(subs, "obliques", "ex85", "Russian Twist",
                "Seated rotational exercise targeting the obliques.",
                listOf(
                    "Sit on the floor leaning back slightly with knees bent.",
                    "Hold a weight or clasp hands together.",
                    "Rotate torso to one side, then to the other, keeping chest open.",
                    "Continue alternating sides for reps or time."
                ),
                listOf("Rotate through the torso, not just arms.", "Keep movements controlled, not jerky."),
                "body weight / weight plate", "beginner", null, videoFile = "russian_twist", imageFile = "russian_twist"),
            sub(subs, "obliques", "ex86", "Side Plank",
                "Static hold on one side to strengthen obliques and lateral core.",
                listOf(
                    "Lie on your side with forearm on the floor under your shoulder.",
                    "Stack your feet and lift hips off the floor.",
                    "Hold a straight line from head to feet.",
                    "Repeat on the other side."
                ),
                listOf("Do not let hips sag.", "Keep neck in line with the spine."),
                "body weight", "beginner", null, videoFile = "side_plank", imageFile = "side_plank"),
            sub(subs, "obliques", "ex87", "Bicycle Crunch",
                "Alternating elbow-to-knee crunch targeting rectus abdominis and obliques.",
                listOf(
                    "Lie on your back with hands lightly behind your head.",
                    "Bring knees to tabletop position.",
                    "Extend one leg while bringing opposite elbow toward bent knee.",
                    "Alternate sides in a pedaling motion."
                ),
                listOf("Keep lower back supported on the floor.", "Move in a controlled, not rushed, tempo."),
                "body weight", "beginner", null, videoFile = "bicycle_crunch", imageFile = "bicycle_crunch"),
            sub(subs, "obliques", "ex88", "Woodchoppers",
                "Diagonal cable or band chop engaging obliques and full core.",
                listOf(
                    "Attach a handle to a high or low cable (or use a band).",
                    "Stand sideways to the machine with feet shoulder-width apart.",
                    "Rotate torso and pull the handle diagonally across your body.",
                    "Control the return back to the start."
                ),
                listOf("Turn hips and shoulders together, not just arms.", "Keep core braced throughout."),
                "cable / band", "intermediate", null, videoFile = "woodchoppers", imageFile = "woodchoppers"),

            // =====================
            // ABS — CORE STABILITY
            // =====================
            sub(subs, "core_stability", "ex18", "Plank",
                "Forearm plank building full-core stability and endurance.",
                listOf(
                    "Support your body on forearms and toes.",
                    "Form a straight line from head to heels.",
                    "Hold while breathing steadily and bracing the core.",
                    "Stop when form begins to break."
                ),
                listOf("Do not let hips sag or pike up.", "Avoid holding your breath."),
                "body weight", "beginner", null, videoFile = "plank", imageFile = "plank"),
            sub(subs, "core_stability", "ex89", "Dead Bug",
                "Anti-extension core drill coordinating opposite arm and leg movement.",
                listOf(
                    "Lie on your back with arms extended toward the ceiling and hips and knees at 90°.",
                    "Lower one arm behind your head while extending the opposite leg.",
                    "Keep lower back gently pressed into the floor.",
                    "Return to the start and alternate sides."
                ),
                listOf("Move slowly and with control.", "Only extend as far as you can while keeping back flat."),
                "body weight", "beginner", null, videoFile = "dead_bug", imageFile = "dead_bug"),
            sub(subs, "core_stability", "ex90", "Bird Dog (Core)",
                "Quadruped stability drill for core and hip control.",
                listOf(
                    "Start on hands and knees with hands under shoulders and knees under hips.",
                    "Extend opposite arm and leg until they are in line with your torso.",
                    "Pause briefly while keeping hips level.",
                    "Return to start and alternate sides."
                ),
                listOf("Do not let lower back sag or rotate.", "Move slowly; prioritize balance over range."),
                "body weight", "beginner", null, videoFile = "bird_dog", imageFile = "bird_dog"),
            sub(subs, "core_stability", "ex91", "Pallof Press",
                "Anti-rotation press using a cable or band to challenge core stability.",
                listOf(
                    "Attach a handle to a cable or band at chest height.",
                    "Stand sideways to the anchor with feet shoulder-width apart.",
                    "Hold the handle at your chest, then press it straight out in front.",
                    "Hold briefly, resisting rotation, then return to your chest."
                ),
                listOf("Keep shoulders and hips squared forward.", "Use light resistance to maintain perfect control."),
                "cable / band", "intermediate", null, videoFile = "pallof_press", imageFile = "pallof_press"),

            // =====================
            // CARDIO — HIIT
            // =====================
            sub(subs, "hiit", "ex19", "Burpees",
                "Full-body explosive movement combining squat, plank and jump.",
                listOf(
                    "From standing, squat down and place hands on the floor.",
                    "Jump feet back into a plank position.",
                    "Do a push-up if desired.",
                    "Jump feet back toward your hands.",
                    "Explosively jump up with arms overhead."
                ),
                listOf("Keep a steady but powerful rhythm.", "Land softly to protect your joints."),
                "body weight", "intermediate", null, videoFile = "burpees", imageFile = "burpees"),
            sub(subs, "hiit", "ex92", "Jump Squats",
                "Squat with an explosive jump to spike heart rate.",
                listOf(
                    "Stand with feet shoulder-width apart.",
                    "Lower into a squat.",
                    "Explosively jump upward, swinging arms as needed.",
                    "Land softly and immediately go into the next rep."
                ),
                listOf("Absorb impact through hips and knees.", "Use smaller jumps if needed for control."),
                "body weight", "intermediate", null, videoFile = "jump_squats", imageFile = "jump_squats"),
            sub(subs, "hiit", "ex93", "Mountain Climbers",
                "Fast alternating knee drives from a plank position.",
                listOf(
                    "Start in a high plank with hands under shoulders.",
                    "Drive one knee toward your chest.",
                    "Quickly switch legs in a running motion.",
                    "Maintain a strong plank throughout."
                ),
                listOf("Do not let hips pike up.", "Keep shoulders stacked over wrists."),
                "body weight", "beginner", null, videoFile = "mountain_climbers", imageFile = "mountain_climbers"),
            sub(subs, "hiit", "ex94", "High Knees",
                "On-the-spot running bringing knees high to chest level.",
                listOf(
                    "Stand tall with feet hip-width apart.",
                    "Run in place, driving knees toward chest.",
                    "Pump arms in coordination.",
                    "Maintain quick, light foot contacts."
                ),
                listOf("Stay on the balls of your feet.", "Use a moderate volume if impact is high."),
                "body weight", "beginner", null, videoFile = "high_knees", imageFile = "high_knees"),
            sub(subs, "hiit", "ex95", "Jump Lunges",
                "Alternating lunge jumps for intense lower-body cardio.",
                listOf(
                    "Start in a split lunge stance.",
                    "Lower down into a lunge.",
                    "Explosively jump and switch legs mid-air.",
                    "Land in the opposite lunge and repeat."
                ),
                listOf("Keep torso upright and core tight.", "Use smaller jumps or step-back lunges if impact is too high."),
                "body weight", "intermediate", null, videoFile = "jump_lunges", imageFile = "jump_lunges"),
            sub(subs, "hiit", "ex96", "Box Jumps",
                "Explosive jump onto a box or platform.",
                listOf(
                    "Stand in front of a sturdy box or platform.",
                    "Hinge slightly and swing arms back.",
                    "Explosively jump onto the box, landing with soft knees.",
                    "Step or lightly jump back down."
                ),
                listOf("Choose a box height you can land on safely.", "Focus on quality landings, not just height."),
                "body weight", "intermediate", null, videoFile = "box_jumps", imageFile = "box_jumps"),
            sub(subs, "hiit", "ex97", "Battle Ropes",
                "Intervals with heavy ropes to combine strength and cardio.",
                listOf(
                    "Stand with feet shoulder-width, knees slightly bent.",
                    "Hold one rope end in each hand.",
                    "Create alternating or double-arm waves rapidly.",
                    "Work in short, intense intervals."
                ),
                listOf("Keep core braced and chest up.", "Do not overextend your lower back."),
                "battle ropes", "intermediate", null, videoFile = "battle_ropes", imageFile = "battle_ropes"),
            sub(subs, "hiit", "ex98", "Sprint Intervals",
                "Short sprints alternated with rest or easy movement.",
                listOf(
                    "Warm up with light jogging and mobility.",
                    "Sprint at near-max effort for a short distance or time.",
                    "Walk or jog slowly to recover.",
                    "Repeat for several rounds."
                ),
                listOf("Use flat, safe surfaces.", "Start with moderate intensity if you are new to sprints."),
                "body weight", "advanced", null, videoFile = "sprint_intervals", imageFile = "sprint_intervals"),

            // =====================
            // CARDIO — STEADY STATE
            // =====================
            sub(subs, "steady_state", "ex20", "Running",
                "Steady-pace running session in an aerobic heart-rate zone.",
                listOf(
                    "Warm up with 5–10 minutes of light jogging or brisk walking.",
                    "Run at a comfortable, conversational pace.",
                    "Maintain steady breathing and posture.",
                    "Cool down with an easy walk and light stretching."
                ),
                listOf("Increase duration gradually over weeks.", "Avoid sudden spikes in pace."),
                "body weight", "beginner", null, videoFile = "running", imageFile = "running"),
            sub(subs, "steady_state", "ex99", "Jogging",
                "Easy, low-impact continuous jog for basic endurance.",
                listOf(
                    "Start with a brisk walk and transition into a light jog.",
                    "Keep steps soft and cadence relaxed.",
                    "Maintain a pace you can sustain for the target time.",
                    "Finish with a walk to bring heart rate down."
                ),
                listOf("Keep shoulders relaxed and arms swinging naturally.", "Use cushioned shoes and safe terrain."),
                "body weight", "beginner", null, videoFile = "jogging", imageFile = "jogging"),
            sub(subs, "steady_state", "ex100", "Cycling",
                "Steady cycling session outdoors or on a bike trainer.",
                listOf(
                    "Set saddle height so knees are slightly bent at the bottom of the pedal stroke.",
                    "Begin pedaling at an easy pace.",
                    "Maintain a moderate, continuous effort.",
                    "Cool down at a lighter intensity."
                ),
                listOf("Keep cadence smooth (e.g., 80–90 RPM).", "Use appropriate resistance to avoid knee strain."),
                "bike", "beginner", null, videoFile = "cycling", imageFile = "cycling"),
            sub(subs, "steady_state", "ex101", "Treadmill Walk",
                "Incline or flat treadmill walking for low-impact cardio.",
                listOf(
                    "Start at a comfortable walking speed.",
                    "Adjust incline to increase challenge without running.",
                    "Hold posture upright and swing arms naturally.",
                    "Gradually reduce speed and incline to cool down."
                ),
                listOf("Avoid holding onto the handrails excessively.", "Use shoes with good grip."),
                "treadmill", "beginner", null, videoFile = "treadmill_walk", imageFile = "treadmill_walk"),
            sub(subs, "steady_state", "ex102", "Stair Climber",
                "Continuous stepping on a stair climber machine.",
                listOf(
                    "Stand upright on the step platform.",
                    "Begin stepping at a manageable pace.",
                    "Keep light contact with handrails if needed for balance.",
                    "Maintain consistent stepping rhythm."
                ),
                listOf("Do not lean heavily on the rails.", "Start with shorter sessions to build endurance."),
                "machine", "beginner", null, videoFile = "stair_climber", imageFile = "stair_climber"),
            sub(subs, "steady_state", "ex103", "Rowing Machine",
                "Full-body steady-state cardio on a rower.",
                listOf(
                    "Set the damper or resistance to a moderate level.",
                    "Push with legs, hinge at the hips, then pull with arms.",
                    "Reverse the sequence to return: arms, hips, legs.",
                    "Maintain a smooth, continuous stroke rate."
                ),
                listOf("Keep lower back neutral.", "Drive primarily with legs, not just arms."),
                "rowing machine", "beginner", null, videoFile = "rowing_machine", imageFile = "rowing_machine"),

            // =====================
            // CARDIO — FUNCTIONAL
            // =====================
            sub(subs, "functional_cardio", "ex104", "Kettlebell Swings",
                "Hip-dominant swing pattern combining power and cardio.",
                listOf(
                    "Stand with feet shoulder-width and kettlebell slightly in front of you.",
                    "Hinge at the hips and grab the kettlebell.",
                    "Hike it back between your legs, then explosively extend hips.",
                    "Let kettlebell swing to about chest height and repeat."
                ),
                listOf("Do not squat the movement; hinge instead.", "Keep shoulders packed and back neutral."),
                "kettlebell", "intermediate", null, videoFile = "kettlebell_swings", imageFile = "kettlebell_swings"),
            sub(subs, "functional_cardio", "ex105", "Medicine Ball Slams",
                "Powerful overhead slam with a medicine ball for full-body conditioning.",
                listOf(
                    "Hold a medicine ball and raise it overhead.",
                    "Brace your core and slam the ball down in front of you.",
                    "Catch or pick it up and repeat.",
                    "Maintain a strong stance throughout."
                ),
                listOf("Use a non-bouncing ball if possible.", "Avoid flexing spine excessively when picking the ball up."),
                "medicine ball", "beginner", null, videoFile = "medicine_ball_slams", imageFile = "medicine_ball_slams"),
            sub(subs, "functional_cardio", "ex106", "Farmer’s Carry (Cardio)",
                "Heavier, longer-distance farmer’s walks for conditioning.",
                listOf(
                    "Pick up heavy dumbbells or farmer’s handles.",
                    "Walk for a set distance or time without stopping.",
                    "Turn carefully at the end of the path.",
                    "Set weights down safely and rest."
                ),
                listOf("Keep chest up and core braced.", "Choose distances that challenge breathing but keep form solid."),
                "dumbbell / farmer’s handles", "intermediate", null, videoFile = "farmers_carry_cardio", imageFile = "farmers_carry_cardio"),
            sub(subs, "functional_cardio", "ex107", "Sled Push",
                "Pushing a loaded sled for functional strength and cardio.",
                listOf(
                    "Grip the sled handles and lean slightly forward.",
                    "Drive through your legs to push the sled.",
                    "Take powerful but controlled steps.",
                    "Push for distance or time, then rest."
                ),
                listOf("Keep hips and shoulders aligned.", "Use appropriate load to maintain good mechanics."),
                "sled", "intermediate", null, videoFile = "sled_push", imageFile = "sled_push"),
            sub(subs, "functional_cardio", "ex108", "Agility Ladder",
                "Footwork drills through a floor ladder to develop speed and coordination.",
                listOf(
                    "Lay out an agility ladder on the floor.",
                    "Perform simple patterns like high-knee runs through the squares.",
                    "Progress to more complex footwork as you improve.",
                    "Maintain quick feet and light contacts."
                ),
                listOf("Start slow to learn patterns.", "Focus on quality before speed."),
                "ladder", "beginner", null, videoFile = "agility_ladder", imageFile = "agility_ladder"),

            // =====================
            // CARDIO — SPORTS
            // =====================
            sub(subs, "sports", "ex109", "Basketball Drills",
                "Continuous basketball-specific drills for cardio (dribbling, layups, defense slides).",
                listOf(
                    "Set up a series of court drills (e.g., suicides, layup lines).",
                    "Move between drills with minimal rest.",
                    "Incorporate direction changes and ball handling.",
                    "Repeat sequences for several rounds."
                ),
                listOf("Use proper basketball shoes.", "Scale intensity to your conditioning level."),
                "basketball + court", "intermediate", null, videoFile = "basketball_drills", imageFile = "basketball_drills"),
            sub(subs, "sports", "ex110", "Soccer Drills",
                "Interval-style soccer drills using sprints, ball control and agility.",
                listOf(
                    "Set cones for short sprints and dribbling patterns.",
                    "Alternate quick sprints with ball-control drills.",
                    "Include direction changes and turns.",
                    "Rest briefly between sets."
                ),
                listOf("Train on a safe surface.", "Warm up ankles and hips thoroughly."),
                "soccer ball + field", "intermediate", null, videoFile = "soccer_drills", imageFile = "soccer_drills"),
            sub(subs, "sports", "ex111", "Boxing Rounds",
                "Timed boxing rounds on a heavy bag or with shadowboxing.",
                listOf(
                    "Set a timer for 2–3 minute rounds with 30–60 seconds rest.",
                    "Throw combinations at a bag or into the air.",
                    "Move your feet and slip/duck as you punch.",
                    "Repeat for multiple rounds."
                ),
                listOf("Keep hands up and protect wrists with wraps/gloves.", "Control breathing, exhaling on punches."),
                "boxing gloves + bag", "intermediate", null, videoFile = "boxing_rounds", imageFile = "boxing_rounds"),
            sub(subs, "sports", "ex112", "Jump Rope",
                "Skipping rope at varied intensities for conditioning.",
                listOf(
                    "Hold rope handles with elbows close to your sides.",
                    "Turn the rope with wrists, not shoulders.",
                    "Jump just high enough to clear the rope.",
                    "Adjust pace for intervals or steady work."
                ),
                listOf("Land softly on the balls of your feet.", "Use proper rope length for your height."),
                "jump rope", "beginner", null, videoFile = "jump_rope", imageFile = "jump_rope"),
            sub(subs, "sports", "ex113", "Shadow Boxing",
                "Free-form striking and movement without equipment to simulate fight rounds.",
                listOf(
                    "Stand in a fighting stance with guard up.",
                    "Throw punches, slips and footwork in combinations.",
                    "Move around the space, visualizing an opponent.",
                    "Work in timed rounds."
                ),
                listOf("Stay light on your feet.", "Maintain guard and technique even as you fatigue."),
                "body weight", "beginner", null, videoFile = "shadow_boxing", imageFile = "shadow_boxing"),
            // =====================
            // CHEST — UPPER CHEST
            // =====================
            sub(subs, "upper_chest", "ex35", "Incline Bench Press",
                "Barbell bench press on an incline bench to emphasize the upper chest.",
                listOf(
                    "Set the bench to a 25–35° incline.",
                    "Lie back and grip the bar slightly wider than shoulders.",
                    "Unrack and lower the bar toward upper chest/clavicle.",
                    "Press the bar up until elbows are extended."
                ),
                listOf("Avoid too steep of an incline to protect shoulders.", "Keep shoulder blades pulled back on the bench."),
                "barbell", "intermediate", null, videoFile = "incline_bench_press", imageFile = "incline_bench_press"),
            sub(subs, "upper_chest", "ex36", "Incline Dumbbell Press",
                "Incline dumbbell press for upper chest with greater range of motion.",
                listOf(
                    "Sit on an incline bench with dumbbells on your thighs.",
                    "Kick dumbbells up and lie back, arms extended above chest.",
                    "Lower dumbbells in an arc toward upper chest.",
                    "Press back up, bringing dumbbells together above chest."
                ),
                listOf("Keep wrists neutral.", "Do not let elbows drop too low behind the body."),
                "dumbbell", "intermediate", null, videoFile = "incline_dumbbell_press", imageFile = "incline_dumbbell_press"),
            sub(subs, "upper_chest", "ex37", "Low-to-High Cable Fly",
                "Cable fly from low pulleys upward to target upper chest fibers.",
                listOf(
                    "Set cable pulleys at the lowest position.",
                    "Stand in the middle, grab handles with palms facing up.",
                    "Step forward into a staggered stance.",
                    "With slight elbow bend, sweep arms up and together toward eye level."
                ),
                listOf("Keep movement controlled; do not swing.", "Squeeze chest at the top of each rep."),
                "cable", "intermediate", null, videoFile = "low_to_high_cable_fly", imageFile = "low_to_high_cable_fly"),
            sub(subs, "upper_chest", "ex38", "Decline Push-up",
                "Feet-elevated push-up variation that shifts emphasis to upper chest and shoulders.",
                listOf(
                    "Place feet on a bench or box, hands on the floor shoulder-width apart.",
                    "Keep body in a straight line.",
                    "Lower chest toward the floor.",
                    "Push back up until arms are straight."
                ),
                listOf("Do not over-arch lower back.", "Choose height that allows solid form."),
                "body weight", "intermediate", null, videoFile = "decline_push_up", imageFile = "decline_push_up"),

            // =====================
            // CHEST — MID CHEST (дополнительные)
            // =====================
            sub(subs, "mid_chest", "ex39", "Dumbbell Bench Press",
                "Flat dumbbell bench press to train mid-chest with independent arms.",
                listOf(
                    "Lie flat on a bench with dumbbells above chest, palms facing forward.",
                    "Lower dumbbells toward chest with elbows at ~45°.",
                    "Pause briefly at the bottom.",
                    "Press dumbbells back up and slightly toward each other."
                ),
                listOf("Use full but comfortable range of motion.", "Avoid bouncing elbows off the bench."),
                "dumbbell", "beginner", null, videoFile = "dumbbell_bench_press", imageFile = "dumbbell_bench_press"),
            sub(subs, "mid_chest", "ex40", "Pec Deck Fly",
                "Machine chest fly targeting mid-chest with fixed path.",
                listOf(
                    "Sit on the pec deck machine with forearms on pads or hands on handles.",
                    "Set seat height so elbows are in line with mid-chest.",
                    "Bring arms together in front of chest.",
                    "Slowly return to starting position with stretch."
                ),
                listOf("Do not let weights slam down.", "Keep chest up and shoulders down."),
                "machine", "beginner", null, videoFile = "pec_deck_fly", imageFile = "pec_deck_fly"),
            sub(subs, "mid_chest", "ex41", "Cable Crossover",
                "Standing cable crossover at chest height for continuous tension on mid-chest.",
                listOf(
                    "Set pulleys slightly above or at chest height.",
                    "Grab handles and step forward into a staggered stance.",
                    "With elbows slightly bent, bring hands together in front of chest.",
                    "Return slowly until you feel a stretch in the chest."
                ),
                listOf("Cross hands slightly for extra squeeze if comfortable.", "Keep torso stable; do not rock."),
                "cable", "intermediate", null, videoFile = "cable_crossover", imageFile = "cable_crossover"),

            // =====================
            // CHEST — LOWER CHEST
            // =====================
            sub(subs, "lower_chest", "ex42", "Decline Bench Press",
                "Decline barbell bench press to emphasize the lower portion of the chest.",
                listOf(
                    "Secure feet on the decline bench and lie back.",
                    "Grip the bar slightly wider than shoulders.",
                    "Unrack and lower the bar toward lower chest/sternum.",
                    "Press the bar back up to arms’ length."
                ),
                listOf("Keep hips secured and do not lift back off the bench.", "Control the bar path and avoid bouncing."),
                "barbell", "intermediate", null, videoFile = "decline_bench_press", imageFile = "decline_bench_press"),
            sub(subs, "lower_chest", "ex43", "High-to-Low Cable Fly",
                "Cable fly from high pulleys down to hips, targeting lower chest.",
                listOf(
                    "Set cable pulleys at the highest position.",
                    "Stand in the middle, grab handles with palms facing down.",
                    "Step forward with one foot for balance.",
                    "With slight elbow bend, sweep arms down and together toward hips."
                ),
                listOf("Keep shoulders down and chest up.", "Use light to moderate weight for control."),
                "cable", "intermediate", null, videoFile = "high_to_low_cable_fly", imageFile = "high_to_low_cable_fly"),
            sub(subs, "lower_chest", "ex44", "Chest Dip",
                "Bodyweight or assisted dip leaning forward to emphasize lower chest.",
                listOf(
                    "Hold parallel bars with arms straight and feet off the ground.",
                    "Lean torso slightly forward.",
                    "Bend elbows to lower body until shoulders are below elbows.",
                    "Press back up to the starting position."
                ),
                listOf("Do not drop too fast into the bottom.", "Use assistance if you cannot control full body weight."),
                "body weight", "intermediate", null, videoFile = "chest_dip", imageFile = "chest_dip"),

            // =====================
            // BACK — LATS (ещё)
            // =====================
            sub(subs, "lats", "ex21", "Straight-Arm Pulldown",
                "Isolation movement for lats using a cable and nearly straight arms.",
                listOf(
                    "Set a high cable with a bar or rope attachment.",
                    "Stand tall, brace core, arms almost straight.",
                    "Pull attachment down toward your hips.",
                    "Pause and squeeze lats.",
                    "Return slowly to the start."
                ),
                listOf("Keep shoulders down.", "Don’t turn it into a triceps pushdown."),
                "cable", "beginner", null, videoFile = "straight_arm_pulldown", imageFile = "straight_arm_pulldown"),

            sub(subs, "lats", "ex22", "Single-Arm Dumbbell Row",
                "One-arm row that targets lats and builds back thickness.",
                listOf(
                    "Place one knee and hand on a bench for support.",
                    "Hold dumbbell with free hand, arm extended.",
                    "Pull dumbbell toward your hip.",
                    "Squeeze at the top.",
                    "Lower under control."
                ),
                listOf("Row toward the hip, not the chest.", "Keep back neutral."),
                "dumbbell", "beginner", null, videoFile = "single_arm_dumbbell_row", imageFile = "single_arm_dumbbell_row"),

            sub(subs, "lats", "ex23", "Neutral-Grip Pull-up",
                "Pull-up variation that is often easier on shoulders and targets lats.",
                listOf(
                    "Grab neutral handles shoulder-width.",
                    "Hang with arms extended and core tight.",
                    "Pull up until chin clears the handles.",
                    "Pause briefly.",
                    "Lower slowly to full extension."
                ),
                listOf("Avoid swinging.", "Use full range of motion."),
                "body weight", "intermediate", null, videoFile = "neutral_grip_pull_up", imageFile = "neutral_grip_pull_up"),

            sub(subs, "lats", "ex24", "Assisted Pull-up",
                "Pull-up with assistance to build strength toward strict pull-ups.",
                listOf(
                    "Set assistance level on the machine/band.",
                    "Grab the bar/handles and start from a dead hang.",
                    "Pull up by driving elbows down.",
                    "Pause at the top.",
                    "Lower slowly."
                ),
                listOf("Reduce assistance over time.", "Control the lowering phase."),
                "machine", "beginner", null, videoFile = "assisted_pull_up", imageFile = "assisted_pull_up"),

            sub(subs, "lats", "ex25", "Close-Grip Lat Pulldown",
                "Lat pulldown variation emphasizing the lower lats.",
                listOf(
                    "Sit and secure thighs under pads.",
                    "Grab close-grip handle.",
                    "Pull handle toward upper chest.",
                    "Squeeze lats.",
                    "Return slowly to full stretch."
                ),
                listOf("Keep chest up.", "Don’t lean too far back."),
                "cable", "beginner", null, videoFile = "close_grip_lat_pulldown", imageFile = "close_grip_lat_pulldown"),

// =====================
// BACK — UPPER BACK (ещё)
// =====================
            sub(subs, "upper_back", "ex26", "Seated Cable Row",
                "Rowing movement for upper back thickness and posture.",
                listOf(
                    "Sit tall and grab the handle.",
                    "Pull handle toward your lower ribs.",
                    "Squeeze shoulder blades together.",
                    "Return slowly with control."
                ),
                listOf("Don’t shrug.", "Keep chest up and core tight."),
                "cable", "beginner", null, videoFile = "seated_cable_row", imageFile = "seated_cable_row"),

            sub(subs, "upper_back", "ex27", "Face Pull",
                "Excellent exercise for rear delts and upper back posture.",
                listOf(
                    "Set rope attachment at face height.",
                    "Grab rope with thumbs pointing back.",
                    "Pull rope toward your face, elbows high.",
                    "Squeeze upper back.",
                    "Return slowly."
                ),
                listOf("Lead with elbows.", "Keep ribs down (no over-arch)."),
                "cable", "beginner", null, videoFile = "face_pull", imageFile = "face_pull"),

            sub(subs, "upper_back", "ex28", "Chest-Supported Row",
                "Row variation that reduces lower-back strain and targets upper back.",
                listOf(
                    "Set incline bench and lie chest-down.",
                    "Hold dumbbells with arms extended.",
                    "Row dumbbells up toward ribs.",
                    "Squeeze shoulder blades.",
                    "Lower slowly."
                ),
                listOf("Keep neck neutral.", "Don’t bounce at the bottom."),
                "dumbbell", "beginner", null, videoFile = "chest_supported_row", imageFile = "chest_supported_row"),

            sub(subs, "upper_back", "ex29", "Reverse Pec Deck Fly",
                "Machine rear-delt fly for upper back and rear shoulders.",
                listOf(
                    "Sit facing the pec deck (reverse fly setup).",
                    "Grip handles with slight elbow bend.",
                    "Open arms outward until shoulder blades squeeze.",
                    "Return slowly."
                ),
                listOf("Use light weight and control.", "Avoid shrugging."),
                "machine", "beginner", null, videoFile = "reverse_pec_deck_fly", imageFile = "reverse_pec_deck_fly"),

            sub(subs, "upper_back", "ex30", "T-Bar Row",
                "Rowing exercise for back thickness and mid-back strength.",
                listOf(
                    "Set up at T-bar station and grip handle.",
                    "Hinge slightly, keep spine neutral.",
                    "Row handle toward your torso.",
                    "Squeeze upper back.",
                    "Lower under control."
                ),
                listOf("Do not jerk the weight.", "Keep elbows close-ish to body."),
                "machine", "intermediate", null, videoFile = "t_bar_row", imageFile = "t_bar_row"),

// =====================
// BACK — LOWER BACK (ещё)
// =====================
            sub(subs, "lower_back", "ex31", "Back Extension",
                "Strengthens lower back and posterior chain using hip hinge.",
                listOf(
                    "Set up on back extension bench.",
                    "Brace core, keep spine neutral.",
                    "Lower torso with control.",
                    "Raise until body forms a straight line."
                ),
                listOf("Avoid hyperextension.", "Move from hips, not the spine."),
                "body weight", "beginner", null, videoFile = "back_extension", imageFile = "back_extension"),

            sub(subs, "lower_back", "ex32", "Good Morning",
                "Hip hinge movement strengthening lower back and hamstrings.",
                listOf(
                    "Place bar on upper back.",
                    "Feet shoulder-width, slight knee bend.",
                    "Hinge at hips, push hips back.",
                    "Return to standing by driving hips forward."
                ),
                listOf("Start light.", "Keep core tight and back neutral."),
                "barbell", "intermediate", null, videoFile = "good_morning", imageFile = "good_morning"),

            sub(subs, "lower_back", "ex33", "Bird Dog",
                "Core stability drill that supports lower back health.",
                listOf(
                    "Start on hands and knees.",
                    "Extend opposite arm and leg.",
                    "Hold briefly with stable hips.",
                    "Return and switch sides."
                ),
                listOf("Keep hips level.", "Move slowly and controlled."),
                "none", "beginner", null, videoFile = "bird_dog", imageFile = "bird_dog"),

            sub(subs, "lower_back", "ex34", "Superman Hold",
                "Bodyweight hold to engage lower back and glutes.",
                listOf(
                    "Lie face down on the floor.",
                    "Lift arms and legs slightly off the floor.",
                    "Hold position while breathing steadily.",
                    "Lower and rest."
                ),
                listOf("Keep neck neutral.", "Avoid holding your breath."),
                "none", "beginner", null, videoFile = "superman_hold", imageFile = "superman_hold"),
        )
    }

    private fun sub(
        subs: List<SubCategory>,
        subId: String,
        id: String,
        titleEn: String,
        description: String,
        steps: List<String>,
        tips: List<String>,
        equipment: String?,
        difficulty: String?,
        path: String?,
        videoFile: String? = null,
        imageFile: String? = null
    ): Exercise {
        val sub = subs.find { it.id == subId }!!
        val media = path?.let { ExerciseMedia.LocalAsset(it) }
            ?: imageFile?.let { ExerciseMedia.LocalAsset("drawable/$it") }
        return Exercise(
            id = id,
            titleEn = titleEn,
            description = description,
            steps = steps,
            tips = tips,
            main = sub.main,
            sub = sub,
            equipment = equipment,
            difficulty = difficulty,
            media = media,
            videoPath = "videos/${videoFile ?: id}.mp4"
        )
    }
}
