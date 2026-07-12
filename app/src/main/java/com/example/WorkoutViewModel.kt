package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutViewModel(
    application: Application,
    private val repository: WorkoutRepository
) : AndroidViewModel(application) {

    // --- Navigation & UI State ---
    var currentScreen by mutableStateOf("dashboard")
    var selectedExercise by mutableStateOf<Exercise?>(null)
    var selectedDay by mutableStateOf<WorkoutDay?>(null)
    var selectedSplit by mutableStateOf<WorkoutSplit?>(null)
    var searchQuery by mutableStateOf("")
    var selectedMuscleFilter by mutableStateOf("All")
    var selectedDifficultyFilter by mutableStateOf("All")

    // --- Database Streams ---
    val splits = repository.allSplits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeSplit = repository.activeSplit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val exercises = repository.allExercises.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sessions = repository.allSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLoggedSets = repository.allLoggedSets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bodyMeasurements = repository.allBodyMeasurements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Workout State (Strong / Hevy Style) ---
    var activeSession by mutableStateOf<WorkoutSession?>(null)
    var activeDay by mutableStateOf<WorkoutDay?>(null)
    var activeSessionSets = mutableStateOf<List<LoggedSet>>(emptyList())
    var workoutDurationSeconds by mutableStateOf(0L)
    private var workoutTimerJob: Job? = null

    // --- Rest Timer State ---
    var isRestTimerActive by mutableStateOf(false)
    var restTimerRemainingSeconds by mutableStateOf(0)
    var restTimerTotalDurationSeconds by mutableStateOf(60) // default 60s
    private var restTimerJob: Job? = null

    // --- AI Coach State ---
    var aiAdvice by mutableStateOf("Tap the button to ask AuraFit AI for personalized elite training recommendations...")
    var isAiLoading by mutableStateOf(false)

    fun getDaysForSplit(splitId: Int): Flow<List<WorkoutDay>> = repository.getDaysForSplit(splitId)
    fun getExercisesForDay(dayId: Int): Flow<List<Exercise>> = repository.getExercisesForDay(dayId)
    fun getSetsForExercise(exerciseId: Int): Flow<List<LoggedSet>> = repository.getSetsForExercise(exerciseId)

    init {
        // Observe active split days if needed
    }

    // --- Active Workout Logic ---
    fun startWorkout(day: WorkoutDay) {
        viewModelScope.launch {
            val splitName = splits.value.find { it.id == day.splitId }?.name ?: "Custom"
            val session = WorkoutSession(
                splitName = splitName,
                dayName = day.name,
                startTime = System.currentTimeMillis(),
                endTime = 0L
            )
            activeSession = session
            activeDay = day
            workoutDurationSeconds = 0
            
            // Fetch default exercises mapped to this day
            repository.getExercisesForDay(day.id).first().let { exerciseList ->
                val initialSets = mutableListOf<LoggedSet>()
                exerciseList.forEach { exercise ->
                    // Find previous sets logged for this exercise on this day
                    val pastSets = getPreviousSetsForExerciseAndDay(exercise.id, day.name)
                    if (pastSets.isNotEmpty()) {
                        pastSets.forEach { pastSet ->
                            initialSets.add(
                                LoggedSet(
                                    sessionId = 0,
                                    exerciseId = exercise.id,
                                    exerciseName = exercise.name,
                                    setIndex = pastSet.setIndex,
                                    weight = pastSet.weight,
                                    reps = pastSet.reps,
                                    isCompleted = false,
                                    isWarmup = pastSet.isWarmup,
                                    isDropSet = pastSet.isDropSet,
                                    isFailure = pastSet.isFailure,
                                    rpe = pastSet.rpe,
                                    restTime = pastSet.restTime
                                )
                            )
                        }
                    } else {
                        // Add 3 default sets for each exercise for easy logging
                        repeat(3) { index ->
                            initialSets.add(
                                LoggedSet(
                                    sessionId = 0,
                                    exerciseId = exercise.id,
                                    exerciseName = exercise.name,
                                    setIndex = index + 1,
                                    weight = 60f,
                                    reps = 10,
                                    isCompleted = false
                                )
                            )
                        }
                    }
                }
                activeSessionSets.value = initialSets
            }

            startWorkoutTimer()
            currentScreen = "active_workout"
        }
    }

    fun startQuickWorkout() {
        val session = WorkoutSession(
            splitName = "Quick Workout",
            dayName = "Full Body Logging",
            startTime = System.currentTimeMillis(),
            endTime = 0L
        )
        activeSession = session
        activeDay = null
        workoutDurationSeconds = 0
        activeSessionSets.value = emptyList()

        startWorkoutTimer()
        currentScreen = "active_workout"
    }

    fun startActiveSplitWorkout() {
        viewModelScope.launch {
            val split = activeSplit.value
            if (split != null) {
                val days = repository.getDaysForSplit(split.id).first()
                if (days.isNotEmpty()) {
                    startWorkout(days.first())
                    return@launch
                }
            }
            startQuickWorkout()
        }
    }

    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                workoutDurationSeconds++
            }
        }
    }

    fun addExerciseToActiveWorkout(exercise: Exercise) {
        val currentSets = activeSessionSets.value.toMutableList()
        val nextIndex = currentSets.filter { it.exerciseId == exercise.id }.size + 1
        currentSets.add(
            LoggedSet(
                sessionId = 0,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                setIndex = nextIndex,
                weight = 60f,
                reps = 10,
                isCompleted = false
            )
        )
        activeSessionSets.value = currentSets
    }

    fun addSetToActiveExercise(exerciseId: Int, exerciseName: String) {
        val currentSets = activeSessionSets.value.toMutableList()
        val nextIndex = currentSets.filter { it.exerciseId == exerciseId }.size + 1
        currentSets.add(
            LoggedSet(
                sessionId = 0,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                setIndex = nextIndex,
                weight = 60f,
                reps = 10,
                isCompleted = false
            )
        )
        activeSessionSets.value = currentSets
    }

    fun removeSetFromActiveExercise(exerciseId: Int) {
        val currentSets = activeSessionSets.value.toMutableList()
        val lastSet = currentSets.filter { it.exerciseId == exerciseId }.lastOrNull()
        if (lastSet != null) {
            currentSets.remove(lastSet)
            // Re-index remaining sets
            var idx = 1
            val updatedSets = currentSets.map {
                if (it.exerciseId == exerciseId) {
                    it.copy(setIndex = idx++)
                } else {
                    it
                }
            }
            activeSessionSets.value = updatedSets
        }
    }

    fun updateSetLogging(setIndexInList: Int, weight: Float, reps: Int) {
        val currentSets = activeSessionSets.value.toMutableList()
        if (setIndexInList in currentSets.indices) {
            currentSets[setIndexInList] = currentSets[setIndexInList].copy(weight = weight, reps = reps)
            activeSessionSets.value = currentSets
        }
    }

    fun updateSetRpe(setIndexInList: Int, rpe: Int?) {
        val currentSets = activeSessionSets.value.toMutableList()
        if (setIndexInList in currentSets.indices) {
            currentSets[setIndexInList] = currentSets[setIndexInList].copy(rpe = rpe)
            activeSessionSets.value = currentSets
        }
    }

    fun getPreviousSetsForExerciseAndDay(exerciseId: Int, dayName: String): List<LoggedSet> {
        val completedSessionsOnDay = sessions.value
            .filter { it.dayName.equals(dayName, ignoreCase = true) }
            .map { it.id }

        if (completedSessionsOnDay.isEmpty()) {
            val lastSessionWithExercise = allLoggedSets.value
                .filter { it.exerciseId == exerciseId && it.isCompleted && it.sessionId > 0 }
                .map { it.sessionId }
                .maxOrNull()
            if (lastSessionWithExercise != null) {
                return allLoggedSets.value.filter { it.sessionId == lastSessionWithExercise && it.exerciseId == exerciseId }
            }
            return emptyList()
        }

        val lastSessionId = allLoggedSets.value
            .filter { it.exerciseId == exerciseId && it.isCompleted && completedSessionsOnDay.contains(it.sessionId) }
            .map { it.sessionId }
            .maxOrNull()

        if (lastSessionId != null) {
            return allLoggedSets.value.filter { it.sessionId == lastSessionId && it.exerciseId == exerciseId }
        }
        return emptyList()
    }

    fun toggleSetCompleted(setIndexInList: Int) {
        val currentSets = activeSessionSets.value.toMutableList()
        if (setIndexInList in currentSets.indices) {
            val set = currentSets[setIndexInList]
            val newCompleted = !set.isCompleted
            currentSets[setIndexInList] = set.copy(isCompleted = newCompleted)
            activeSessionSets.value = currentSets

            // If checked and timer is enabled, auto start the rest timer!
            if (newCompleted) {
                startRestTimer(set.restTime)
            }
        }
    }

    fun toggleSetWarmup(setIndexInList: Int) {
        val currentSets = activeSessionSets.value.toMutableList()
        if (setIndexInList in currentSets.indices) {
            val set = currentSets[setIndexInList]
            currentSets[setIndexInList] = set.copy(isWarmup = !set.isWarmup, isDropSet = false, isFailure = false)
            activeSessionSets.value = currentSets
        }
    }

    fun toggleSetDropSet(setIndexInList: Int) {
        val currentSets = activeSessionSets.value.toMutableList()
        if (setIndexInList in currentSets.indices) {
            val set = currentSets[setIndexInList]
            currentSets[setIndexInList] = set.copy(isDropSet = !set.isDropSet, isWarmup = false, isFailure = false)
            activeSessionSets.value = currentSets
        }
    }

    fun toggleSetFailure(setIndexInList: Int) {
        val currentSets = activeSessionSets.value.toMutableList()
        if (setIndexInList in currentSets.indices) {
            val set = currentSets[setIndexInList]
            currentSets[setIndexInList] = set.copy(isFailure = !set.isFailure, isWarmup = false, isDropSet = false)
            activeSessionSets.value = currentSets
        }
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        restTimerTotalDurationSeconds = seconds
        restTimerRemainingSeconds = seconds
        isRestTimerActive = true
        restTimerJob = viewModelScope.launch {
            while (restTimerRemainingSeconds > 0) {
                delay(1000)
                restTimerRemainingSeconds--
            }
            isRestTimerActive = false
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        isRestTimerActive = false
    }

    fun finishWorkout(notes: String) {
        val session = activeSession ?: return
        viewModelScope.launch {
            val completedSets = activeSessionSets.value.filter { it.isCompleted }
            val totalVolume = completedSets.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
            val totalReps = completedSets.sumOf { it.reps }

            val finishedSession = session.copy(
                endTime = System.currentTimeMillis(),
                duration = workoutDurationSeconds,
                notes = notes,
                totalVolume = totalVolume,
                totalReps = totalReps
            )

            repository.saveWorkoutSession(finishedSession, activeSessionSets.value)

            // Reset active workout state
            activeSession = null
            activeDay = null
            activeSessionSets.value = emptyList()
            workoutDurationSeconds = 0
            workoutTimerJob?.cancel()
            stopRestTimer()

            currentScreen = "dashboard"
        }
    }

    fun cancelWorkout() {
        activeSession = null
        activeDay = null
        activeSessionSets.value = emptyList()
        workoutDurationSeconds = 0
        workoutTimerJob?.cancel()
        stopRestTimer()
        currentScreen = "dashboard"
    }

    // --- Splits & Custom Plan Management ---
    fun createCustomSplit(name: String, days: List<Pair<String, String>>) {
        viewModelScope.launch {
            val splitId = repository.insertSplit(WorkoutSplit(name = name, isCustom = true, isActive = false))
            days.forEachIndexed { index, (dayName, dayOfWeek) ->
                repository.insertDay(
                    WorkoutDay(
                        splitId = splitId.toInt(),
                        name = dayName,
                        dayOfWeek = dayOfWeek,
                        orderIndex = index
                    )
                )
            }
        }
    }

    fun activateSplit(split: WorkoutSplit) {
        viewModelScope.launch {
            repository.updateSplitStatus(split, true)
        }
    }

    fun deleteSplit(split: WorkoutSplit) {
        viewModelScope.launch {
            repository.deleteSplit(split)
        }
    }

    fun updateDay(day: WorkoutDay) {
        viewModelScope.launch {
            repository.updateDay(day)
            if (selectedDay?.id == day.id) {
                selectedDay = day
            }
        }
    }

    fun deleteDay(day: WorkoutDay) {
        viewModelScope.launch {
            repository.deleteDay(day)
            currentScreen = "splits"
        }
    }

    fun addDayToSplit(splitId: Int, name: String, dayOfWeek: String) {
        viewModelScope.launch {
            repository.insertDay(
                WorkoutDay(
                    splitId = splitId,
                    name = name,
                    dayOfWeek = dayOfWeek,
                    orderIndex = 99
                )
            )
        }
    }

    fun addExerciseToDay(dayId: Int, exercise: Exercise) {
        viewModelScope.launch {
            repository.addExerciseToDay(dayId, exercise.id, 99)
        }
    }

    fun removeExerciseFromDay(dayId: Int, exerciseId: Int) {
        viewModelScope.launch {
            repository.removeExerciseFromDay(dayId, exerciseId)
        }
    }

    // --- Exercise Custom creation ---
    fun createCustomExercise(
        name: String,
        category: String,
        muscle: String,
        equip: String,
        diff: String,
        desc: String,
        steps: String
    ) {
        viewModelScope.launch {
            val customEx = Exercise(
                name = name,
                category = category,
                muscleGroup = muscle,
                equipment = equip,
                difficulty = diff,
                description = desc,
                executionSteps = steps,
                isCustom = true
            )
            repository.insertExercise(customEx)
        }
    }

    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch {
            repository.updateExercise(exercise.copy(isFavorite = !exercise.isFavorite))
        }
    }

    // --- Body Measurements ---
    fun addBodyMeasurement(
        weight: Float,
        bodyFat: Float,
        chest: Float,
        waist: Float,
        arms: Float,
        thighs: Float,
        calves: Float,
        shoulders: Float,
        neck: Float,
        hip: Float
    ) {
        viewModelScope.launch {
            val measurement = BodyMeasurement(
                timestamp = System.currentTimeMillis(),
                weight = weight,
                bodyFat = bodyFat,
                chest = chest,
                waist = waist,
                arms = arms,
                thighs = thighs,
                calves = calves,
                shoulders = shoulders,
                neck = neck,
                hip = hip
            )
            repository.insertBodyMeasurement(measurement)
            
            // Sync user profile weight to latest logged weight
            userWeight = weight.toString()
            prefs.edit().putString("user_weight", weight.toString()).apply()
        }
    }

    fun updateCurrentWeight(weight: Float) {
        viewModelScope.launch {
            val currentList = bodyMeasurements.value
            val lastFat = currentList.firstOrNull()?.bodyFat ?: 0f
            val lastChest = currentList.firstOrNull()?.chest ?: 0f
            val lastWaist = currentList.firstOrNull()?.waist ?: 0f
            val lastArms = currentList.firstOrNull()?.arms ?: 0f
            val lastThighs = currentList.firstOrNull()?.thighs ?: 0f
            val lastCalves = currentList.firstOrNull()?.calves ?: 0f
            val lastShoulders = currentList.firstOrNull()?.shoulders ?: 0f
            val lastNeck = currentList.firstOrNull()?.neck ?: 0f
            val lastHip = currentList.firstOrNull()?.hip ?: 0f

            val measurement = BodyMeasurement(
                timestamp = System.currentTimeMillis(),
                weight = weight,
                bodyFat = lastFat,
                chest = lastChest,
                waist = lastWaist,
                arms = lastArms,
                thighs = lastThighs,
                calves = lastCalves,
                shoulders = lastShoulders,
                neck = lastNeck,
                hip = lastHip
            )
            repository.insertBodyMeasurement(measurement)
            
            // Sync profile
            userWeight = weight.toString()
            prefs.edit().putString("user_weight", weight.toString()).apply()
        }
    }

    fun deleteBodyMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.deleteBodyMeasurement(measurement)
        }
    }

    // --- Personal Records Calculation ---
    fun calculatePRs(): PersonalRecords {
        val sets = allLoggedSets.value.filter { it.isCompleted }
        val sessionsVal = sessions.value

        val highestWeightSet = sets.maxByOrNull { it.weight }
        val highestWeight = highestWeightSet?.weight ?: 0f
        val highestWeightExercise = highestWeightSet?.exerciseName ?: "None"

        val highestVolumeSession = sessionsVal.maxByOrNull { it.totalVolume }
        val highestVolume = highestVolumeSession?.totalVolume ?: 0f
        val highestVolumeDayName = highestVolumeSession?.dayName ?: "None"

        val highestRepsSet = sets.maxByOrNull { it.reps }
        val highestReps = highestRepsSet?.reps ?: 0
        val highestRepsExercise = highestRepsSet?.exerciseName ?: "None"

        val longestWorkoutSession = sessionsVal.maxByOrNull { it.duration }
        val longestDurationMinutes = (longestWorkoutSession?.duration ?: 0) / 60

        return PersonalRecords(
            highestWeight = highestWeight,
            highestWeightExercise = highestWeightExercise,
            highestVolume = highestVolume,
            highestVolumeDay = highestVolumeDayName,
            highestReps = highestReps,
            highestRepsExercise = highestRepsExercise,
            longestWorkoutMinutes = longestDurationMinutes
        )
    }

    data class PersonalRecords(
        val highestWeight: Float,
        val highestWeightExercise: String,
        val highestVolume: Float,
        val highestVolumeDay: String,
        val highestReps: Int,
        val highestRepsExercise: String,
        val longestWorkoutMinutes: Long
    )

    // --- Stats & Streak Calculators ---
    fun getStreak(): Int {
        val sessionTimestamps = sessions.value.map { it.startTime }.sortedDescending()
        if (sessionTimestamps.isEmpty()) return 0

        var streak = 0
        val oneDayMs = 24 * 60 * 60 * 1000L
        var currentRef = System.currentTimeMillis()

        for (timestamp in sessionTimestamps) {
            val diff = currentRef - timestamp
            if (diff < oneDayMs * 1.8) { // allow a bit of buffer
                streak++
                currentRef = timestamp
            } else {
                break
            }
        }
        return streak
    }

    // --- AI Recommendations Advice Trigger ---
    fun askAiForRecommendations() {
        if (isAiLoading) return
        isAiLoading = true
        aiAdvice = "AuraFit AI is analyzing your statistics..."
        viewModelScope.launch {
            val activeSplitName = activeSplit.value?.name ?: "None"
            val totalSessions = sessions.value.size
            val streak = getStreak()
            val lastWorkout = sessions.value.firstOrNull()?.dayName ?: "None"
            val bodyWeight = bodyMeasurements.value.firstOrNull()?.weight ?: 75f

            val prompt = """
                The user's current workout split is: $activeSplitName.
                They have completed $totalSessions total logged workouts.
                Their current workout consistency streak is $streak days.
                Their last completed workout was: $lastWorkout.
                Their current logged bodyweight is: $bodyWeight kg.
                Please provide:
                1. A brief summary of their consistency.
                2. Weight progression or incremental recommendations for their next workout.
                3. Deloading or muscle recovery advice based on general guidelines.
                Speak like an elite strength coach. Bullet points only. Max 150 words.
            """.trimIndent()

            val result = withContext(Dispatchers.IO) {
                GeminiServiceClient.getRecommendations(prompt)
            }
            aiAdvice = result
            isAiLoading = false
        }
    }

    // --- JSON Backup & Restore ---
    fun exportBackupJson(): String {
        return try {
            val sb = StringBuilder()
            sb.append("{\n")
            sb.append("  \"app\": \"AuraFit Backup\",\n")
            sb.append("  \"timestamp\": ${System.currentTimeMillis()},\n")
            sb.append("  \"user_name\": \"$userName\",\n")
            sb.append("  \"user_age\": \"$userAge\",\n")
            sb.append("  \"user_gender\": \"$userGender\",\n")
            sb.append("  \"user_weight\": \"$userWeight\",\n")
            sb.append("  \"user_height\": \"$userHeight\",\n")
            sb.append("  \"user_goal\": \"$userGoal\",\n")
            sb.append("  \"user_activity_level\": \"$userActivityLevel\",\n")
            sb.append("  \"user_meal_frequency\": \"$userMealFrequency\",\n")
            sb.append("  \"is_onboarding_completed\": $isOnboardingCompleted,\n")
            sb.append("  \"target_calories\": $targetCalories,\n")
            sb.append("  \"target_protein\": $targetProtein,\n")
            sb.append("  \"target_carbs\": $targetCarbs,\n")
            sb.append("  \"target_fat\": $targetFat,\n")

            // Serialize sessions
            sb.append("  \"sessions\": [\n")
            val currentSessions = sessions.value
            currentSessions.forEachIndexed { sIdx, session ->
                sb.append("    {\n")
                sb.append("      \"id\": ${session.id},\n")
                sb.append("      \"splitName\": \"${session.splitName.replace("\"", "\\\"")}\",\n")
                sb.append("      \"dayName\": \"${session.dayName.replace("\"", "\\\"")}\",\n")
                sb.append("      \"startTime\": ${session.startTime},\n")
                sb.append("      \"endTime\": ${session.endTime},\n")
                sb.append("      \"notes\": \"${session.notes.replace("\"", "\\\"")}\",\n")
                sb.append("      \"duration\": ${session.duration},\n")
                sb.append("      \"totalVolume\": ${session.totalVolume},\n")
                sb.append("      \"totalReps\": ${session.totalReps}\n")
                sb.append("    }${if (sIdx < currentSessions.size - 1) "," else ""}\n")
            }
            sb.append("  ],\n")

            // Serialize sets
            sb.append("  \"logged_sets\": [\n")
            val currentSets = allLoggedSets.value
            currentSets.forEachIndexed { setIdx, set ->
                sb.append("    {\n")
                sb.append("      \"sessionId\": ${set.sessionId},\n")
                sb.append("      \"exerciseId\": ${set.exerciseId},\n")
                sb.append("      \"exerciseName\": \"${set.exerciseName.replace("\"", "\\\"")}\",\n")
                sb.append("      \"setIndex\": ${set.setIndex},\n")
                sb.append("      \"weight\": ${set.weight},\n")
                sb.append("      \"reps\": ${set.reps},\n")
                sb.append("      \"isCompleted\": ${set.isCompleted},\n")
                sb.append("      \"isWarmup\": ${set.isWarmup},\n")
                sb.append("      \"isDropSet\": ${set.isDropSet},\n")
                sb.append("      \"isFailure\": ${set.isFailure},\n")
                sb.append("      \"rpe\": ${set.rpe ?: "null"},\n")
                sb.append("      \"tempo\": ${if (set.tempo != null) "\"${set.tempo}\"" else "null"},\n")
                sb.append("      \"restTime\": ${set.restTime}\n")
                sb.append("    }${if (setIdx < currentSets.size - 1) "," else ""}\n")
            }
            sb.append("  ],\n")

            // Serialize body measurements
            sb.append("  \"body_measurements\": [\n")
            val currentMeasurements = bodyMeasurements.value
            currentMeasurements.forEachIndexed { mIdx, bm ->
                sb.append("    {\n")
                sb.append("      \"timestamp\": ${bm.timestamp},\n")
                sb.append("      \"weight\": ${bm.weight},\n")
                sb.append("      \"bodyFat\": ${bm.bodyFat},\n")
                sb.append("      \"chest\": ${bm.chest},\n")
                sb.append("      \"waist\": ${bm.waist},\n")
                sb.append("      \"arms\": ${bm.arms},\n")
                sb.append("      \"thighs\": ${bm.thighs},\n")
                sb.append("      \"calves\": ${bm.calves},\n")
                sb.append("      \"shoulders\": ${bm.shoulders},\n")
                sb.append("      \"neck\": ${bm.neck},\n")
                sb.append("      \"hip\": ${bm.hip}\n")
                sb.append("    }${if (mIdx < currentMeasurements.size - 1) "," else ""}\n")
            }
            sb.append("  ],\n")

            // Serialize food logs
            sb.append("  \"food_logs\": [\n")
            val currentFoodLogs = foodLogs.value
            currentFoodLogs.forEachIndexed { fIdx, fl ->
                sb.append("    {\n")
                sb.append("      \"name\": \"${fl.name.replace("\"", "\\\"")}\",\n")
                sb.append("      \"calories\": ${fl.calories},\n")
                sb.append("      \"protein\": ${fl.protein},\n")
                sb.append("      \"carbs\": ${fl.carbs},\n")
                sb.append("      \"fat\": ${fl.fat},\n")
                sb.append("      \"mealType\": \"${fl.mealType}\",\n")
                sb.append("      \"timestamp\": ${fl.timestamp}\n")
                sb.append("    }${if (fIdx < currentFoodLogs.size - 1) "," else ""}\n")
            }
            sb.append("  ]\n")

            sb.append("}")
            sb.toString()
        } catch (e: Exception) {
            "{\"error\": \"Failed to export: ${e.localizedMessage}\"}"
        }
    }

    fun importBackupJson(jsonString: String): String {
        if (jsonString.trim().isEmpty()) {
            return "Please paste or provide backup JSON text to import."
        }
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
            )
            val adapter = moshi.adapter<Map<String, Any>>(type)
            val data = adapter.fromJson(jsonString) ?: return "Failed to parse JSON backup. Format is invalid."

            if (data["app"] != "AuraFit Backup") {
                return "Invalid backup format: App metadata does not match."
            }

            // Restore user preferences
            val name = data["user_name"] as? String ?: ""
            val age = data["user_age"] as? String ?: ""
            val gender = data["user_gender"] as? String ?: ""
            val weight = data["user_weight"] as? String ?: ""
            val height = data["user_height"] as? String ?: ""
            val goal = data["user_goal"] as? String ?: "Lose weight"
            val activityLevel = data["user_activity_level"] as? String ?: "Moderately Active"
            val mealFrequency = data["user_meal_frequency"] as? String ?: ""
            val onboardingCompleted = data["is_onboarding_completed"] as? Boolean ?: false

            saveUserProfile(name, age, gender, weight, height, goal, activityLevel, mealFrequency)
            isOnboardingCompleted = onboardingCompleted
            prefs.edit().putBoolean("is_onboarding_completed", onboardingCompleted).apply()

            (data["target_calories"] as? Number)?.toInt()?.let { updateTargetCalories(it) }
            (data["target_protein"] as? Number)?.toInt()?.let { updateTargetProtein(it) }
            (data["target_carbs"] as? Number)?.toInt()?.let { updateTargetCarbs(it) }
            (data["target_fat"] as? Number)?.toInt()?.let { updateTargetFat(it) }

            val bmList = data["body_measurements"] as? List<Map<String, Any>>
            val flList = data["food_logs"] as? List<Map<String, Any>>
            val sessionsList = data["sessions"] as? List<Map<String, Any>>
            val loggedSetsList = data["logged_sets"] as? List<Map<String, Any>>

            // Parse and restore Room DB lists in a coroutine
            viewModelScope.launch {
                // Restore body measurements
                bmList?.forEach { item ->
                    repository.insertBodyMeasurement(
                        BodyMeasurement(
                            timestamp = (item["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            weight = (item["weight"] as? Number)?.toFloat() ?: 70f,
                            bodyFat = (item["bodyFat"] as? Number)?.toFloat() ?: 0f,
                            chest = (item["chest"] as? Number)?.toFloat() ?: 0f,
                            waist = (item["waist"] as? Number)?.toFloat() ?: 0f,
                            arms = (item["arms"] as? Number)?.toFloat() ?: 0f,
                            thighs = (item["thighs"] as? Number)?.toFloat() ?: 0f,
                            calves = (item["calves"] as? Number)?.toFloat() ?: 0f,
                            shoulders = (item["shoulders"] as? Number)?.toFloat() ?: 0f,
                            neck = (item["neck"] as? Number)?.toFloat() ?: 0f,
                            hip = (item["hip"] as? Number)?.toFloat() ?: 0f
                        )
                    )
                }

                // Restore food logs
                flList?.forEach { item ->
                    repository.insertFoodLog(
                        FoodLog(
                            name = item["name"] as? String ?: "Unknown Food",
                            calories = (item["calories"] as? Number)?.toInt() ?: 0,
                            protein = (item["protein"] as? Number)?.toFloat() ?: 0f,
                            carbs = (item["carbs"] as? Number)?.toFloat() ?: 0f,
                            fat = (item["fat"] as? Number)?.toFloat() ?: 0f,
                            mealType = item["mealType"] as? String ?: "Breakfast",
                            timestamp = (item["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    )
                }

                // Restore sessions & logged sets
                sessionsList?.forEach { sItem ->
                    val originalId = (sItem["id"] as? Number)?.toInt() ?: 0
                    val sessionObj = WorkoutSession(
                        splitName = sItem["splitName"] as? String ?: "Custom Split",
                        dayName = sItem["dayName"] as? String ?: "Workout",
                        startTime = (sItem["startTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        endTime = (sItem["endTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        notes = sItem["notes"] as? String ?: "",
                        duration = (sItem["duration"] as? Number)?.toLong() ?: 0,
                        totalVolume = (sItem["totalVolume"] as? Number)?.toFloat() ?: 0f,
                        totalReps = (sItem["totalReps"] as? Number)?.toInt() ?: 0
                    )

                    // Find all logged sets corresponding to this session
                    val matchingSets = loggedSetsList?.filter {
                        (it["sessionId"] as? Number)?.toInt() == originalId
                    }?.map { setItem ->
                        LoggedSet(
                            sessionId = 0, // will be overwritten during save
                            exerciseId = (setItem["exerciseId"] as? Number)?.toInt() ?: 1,
                            exerciseName = setItem["exerciseName"] as? String ?: "Exercise",
                            setIndex = (setItem["setIndex"] as? Number)?.toInt() ?: 1,
                            weight = (setItem["weight"] as? Number)?.toFloat() ?: 0f,
                            reps = (setItem["reps"] as? Number)?.toInt() ?: 0,
                            isCompleted = setItem["isCompleted"] as? Boolean ?: true,
                            isWarmup = setItem["isWarmup"] as? Boolean ?: false,
                            isDropSet = setItem["isDropSet"] as? Boolean ?: false,
                            isFailure = setItem["isFailure"] as? Boolean ?: false,
                            rpe = (setItem["rpe"] as? Number)?.toInt(),
                            tempo = setItem["tempo"] as? String,
                            restTime = (setItem["restTime"] as? Number)?.toInt() ?: 60
                        )
                    } ?: emptyList()

                    repository.saveWorkoutSession(sessionObj, matchingSets)
                }
            }

            "Backup imported successfully! Loaded ${sessionsList?.size ?: 0} sessions and ${bmList?.size ?: 0} metrics."
        } catch (e: Exception) {
            "Failed to import backup: ${e.localizedMessage}"
        }
    }

    // --- Food Logs ---
    val foodLogs = repository.allFoodLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val prefs = application.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)

    var targetCalories by mutableStateOf(prefs.getInt("target_calories", 2000))
    var targetProtein by mutableStateOf(prefs.getInt("target_protein", 150))
    var targetCarbs by mutableStateOf(prefs.getInt("target_carbs", 220))
    var targetFat by mutableStateOf(prefs.getInt("target_fat", 65))

    fun updateTargetCalories(value: Int) {
        targetCalories = value
        prefs.edit().putInt("target_calories", value).apply()
    }
    fun updateTargetProtein(value: Int) {
        targetProtein = value
        prefs.edit().putInt("target_protein", value).apply()
    }
    fun updateTargetCarbs(value: Int) {
        targetCarbs = value
        prefs.edit().putInt("target_carbs", value).apply()
    }
    fun updateTargetFat(value: Int) {
        targetFat = value
        prefs.edit().putInt("target_fat", value).apply()
    }

    var userName by mutableStateOf(prefs.getString("user_name", "") ?: "")
    var userAge by mutableStateOf(prefs.getString("user_age", "") ?: "")
    var userGender by mutableStateOf(prefs.getString("user_gender", "") ?: "")
    var userWeight by mutableStateOf(prefs.getString("user_weight", "") ?: "")
    var userHeight by mutableStateOf(prefs.getString("user_height", "") ?: "")
    var userGoal by mutableStateOf(prefs.getString("user_goal", "Lose weight") ?: "Lose weight")
    var userActivityLevel by mutableStateOf(prefs.getString("user_activity_level", "Moderately Active") ?: "Moderately Active")
    var userMealFrequency by mutableStateOf(prefs.getString("user_meal_frequency", "") ?: "")
    var isOnboardingCompleted by mutableStateOf(prefs.getBoolean("is_onboarding_completed", false))

    fun saveUserProfile(
        name: String,
        age: String,
        gender: String,
        weight: String,
        height: String,
        goal: String,
        activityLevel: String,
        mealFrequency: String
    ) {
        userName = name
        userAge = age
        userGender = gender
        userWeight = weight
        userHeight = height
        userGoal = goal
        userActivityLevel = activityLevel
        userMealFrequency = mealFrequency
        isOnboardingCompleted = true

        prefs.edit()
            .putString("user_name", name)
            .putString("user_age", age)
            .putString("user_gender", gender)
            .putString("user_weight", weight)
            .putString("user_height", height)
            .putString("user_goal", goal)
            .putString("user_activity_level", activityLevel)
            .putString("user_meal_frequency", mealFrequency)
            .putBoolean("is_onboarding_completed", true)
            .apply()
    }

    var isBodyAnalysisLoading by mutableStateOf(false)
    var bodyAnalysisResult by mutableStateOf<String?>(null)

    fun getOfflineSuggestedTargets(): SuggestedTargets {
        val w = userWeight.toDoubleOrNull() ?: 70.0
        val h = userHeight.toDoubleOrNull() ?: 170.0
        val a = userAge.toIntOrNull() ?: 25
        val g = userGender.trim().uppercase()

        // BMR (Mifflin-St Jeor)
        val bmr = if (g == "M" || g.startsWith("MALE")) {
            10.0 * w + 6.25 * h - 5.0 * a + 5.0
        } else if (g == "F" || g.startsWith("FEMALE")) {
            10.0 * w + 6.25 * h - 5.0 * a - 161.0
        } else {
            10.0 * w + 6.25 * h - 5.0 * a - 78.0 // neutral
        }

        // TDEE multiplier
        val multiplier = when {
            userActivityLevel.contains("sedentary", ignoreCase = true) -> 1.2
            userActivityLevel.contains("active", ignoreCase = true) -> 1.55
            userActivityLevel.contains("athlete", ignoreCase = true) -> 1.8
            else -> 1.4
        }

        val tdee = bmr * multiplier

        // Goal modifier
        val targetCals = when {
            userGoal.contains("lose", ignoreCase = true) -> (tdee - 500.0).coerceAtLeast(1200.0)
            userGoal.contains("gain", ignoreCase = true) -> tdee + 350.0
            else -> tdee
        }.toInt()

        // Macros splits
        val proteinPerKg = when {
            userGoal.contains("gain", ignoreCase = true) -> 2.0
            userGoal.contains("lose", ignoreCase = true) -> 2.2
            else -> 1.8
        }
        val targetProt = (w * proteinPerKg).coerceIn(40.0, 300.0).toInt()

        val fatPerKg = when {
            userGoal.contains("gain", ignoreCase = true) -> 0.9
            userGoal.contains("lose", ignoreCase = true) -> 0.7
            else -> 0.8
        }
        val targetFatVal = (w * fatPerKg).coerceIn(30.0, 150.0).toInt()

        // Remaining calories for carbs
        val proteinKcal = targetProt * 4
        val fatKcal = targetFatVal * 9
        val remainingKcal = (targetCals - proteinKcal - fatKcal).coerceAtLeast(100)
        val targetCarb = (remainingKcal / 4).toInt()

        val activeHours = when {
            userActivityLevel.contains("sedentary", ignoreCase = true) -> 0.5
            userActivityLevel.contains("active", ignoreCase = true) -> 1.0
            userActivityLevel.contains("athlete", ignoreCase = true) -> 1.5
            else -> 1.0
        }

        val suggestedFreq = if (userMealFrequency.isNotEmpty()) userMealFrequency else {
            if (userGoal.contains("gain", ignoreCase = true)) "4-5" else "3"
        }

        return SuggestedTargets(
            calories = targetCals,
            protein = targetProt,
            carbs = targetCarb,
            fat = targetFatVal,
            activeHours = activeHours,
            mealFrequency = suggestedFreq
        )
    }

    fun analyzeUserBodyMetrics(onComplete: (String) -> Unit = {}) {
        if (isBodyAnalysisLoading) return
        isBodyAnalysisLoading = true
        bodyAnalysisResult = null

        // First, calculate offline suggested targets and update state automatically!
        val suggested = getOfflineSuggestedTargets()
        updateTargetCalories(suggested.calories)
        updateTargetProtein(suggested.protein)
        updateTargetCarbs(suggested.carbs)
        updateTargetFat(suggested.fat)

        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                bodyAnalysisResult = "We calculated a personalized offline metabolic profile for you based on your stats:\n\n• **Daily Calories**: ${suggested.calories} kcal\n• **Protein**: ${suggested.protein}g\n• **Carbohydrates**: ${suggested.carbs}g\n• **Fat**: ${suggested.fat}g\n• **Target Active Hours**: ${suggested.activeHours} hrs/day\n• **Meal Frequency**: ${suggested.mealFrequency} meals/day\n\n*Note: To get Gyani's detailed coaching strategy, add your Gemini API key in the Secrets panel of AI Studio.*"
                isBodyAnalysisLoading = false
                onComplete(bodyAnalysisResult ?: "")
                return@launch
            }

            val prompt = """
                Analyze the following user profile and body metrics to recommend a perfect fitness and nutrition strategy:
                Name: $userName
                Age: $userAge
                Gender: $userGender
                Weight: $userWeight kg
                Height: $userHeight cm
                Fitness Goal: $userGoal
                Activity Level: $userActivityLevel
                Meal Frequency: $userMealFrequency times per day

                Calculate:
                1. Target Daily Calories (kcal) appropriate for their goal (fat loss, muscle gain, or maintenance).
                2. Optimal Protein (g), Carbohydrates (g), and Fat (g) splits.
                3. A highly motivating fitness strategy, nutrition tips, and meal splitting ideas.

                You MUST return ONLY a raw JSON object conforming exactly to this structure:
                {
                  "calories": Integer,
                  "protein": Integer,
                  "carbs": Integer,
                  "fat": Integer,
                  "analysis": "A detailed, encouraging analysis with bullet points and bold headers. Explain the math, the approach, and why these targets work. Under 250 words."
                }
            """.trimIndent()

            val resultText = withContext(Dispatchers.IO) {
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "You are Gyani, the elite personal trainer. You respond ONLY with raw, valid JSON conforming to the requested schema. No markdown wrapping unless inside the JSON fields."
                            )
                        )
                    )
                )

                try {
                    val response = GeminiServiceClient.api.generateWorkoutAdvice(apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                } catch (e: Exception) {
                    null
                }
            }

            if (resultText != null) {
                try {
                    val cleanedJson = resultText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(GeminiBodyAnalysisResponse::class.java)
                    val parsed = adapter.fromJson(cleanedJson)

                    if (parsed != null) {
                        updateTargetCalories(parsed.calories)
                        updateTargetProtein(parsed.protein)
                        updateTargetCarbs(parsed.carbs)
                        updateTargetFat(parsed.fat)
                        bodyAnalysisResult = parsed.analysis
                    } else {
                        throw Exception("Failed to parse response")
                    }
                } catch (e: Exception) {
                    bodyAnalysisResult = "We calculated a default profile for you: ${targetCalories} kcal (Protein: ${targetProtein}g, Carbs: ${targetCarbs}g, Fat: ${targetFat}g). Please edit them anytime in the nutrition log."
                }
            } else {
                bodyAnalysisResult = "Unable to reach Gyani. Defaulting to 2000 kcal targets. Please try again or update manually."
            }

            isBodyAnalysisLoading = false
            onComplete(bodyAnalysisResult ?: "")
        }
    }

    var gyaniChatHistory by mutableStateOf(listOf(
        ChatMessage(
            text = "Namaste! I am Gyani, your AI coach and fitness assistant. I can recommend workouts, track your diet, change splits, or add specific exercises. What are we achieving today?",
            isUser = false
        )
    ))
    var isGyaniAiLoading by mutableStateOf(false)

    fun sendGyaniChatMessage(userText: String) {
        if (userText.trim().isEmpty() || isGyaniAiLoading) return

        val userMessage = ChatMessage(text = userText, isUser = true)
        gyaniChatHistory = gyaniChatHistory + userMessage
        isGyaniAiLoading = true

        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                val errorMsg = "Gemini API key is not configured. Please add your key in the Secrets panel in AI Studio to talk with Gyani."
                gyaniChatHistory = gyaniChatHistory + ChatMessage(text = errorMsg, isUser = false)
                isGyaniAiLoading = false
                return@launch
            }

            val historyText = gyaniChatHistory.takeLast(10).joinToString("\n") { 
                if (it.isUser) "User: ${it.text}" else "Gyani: ${it.text}"
            }

            val activeSplitName = activeSplit.value?.name ?: "None"
            val activeSplitId = activeSplit.value?.id ?: 0

            val prompt = """
                The user has the following profile:
                - Name: $userName
                - Goal: $userGoal
                - Daily Calories Target: $targetCalories kcal
                - Active Workout Split: $activeSplitName (ID: $activeSplitId)

                Here is the recent conversation history:
                $historyText

                User's latest message: "$userText"

                You are Gyani, an intelligent, empathetic, and knowledgeable Indian fitness coach and AI workout assistant.
                Formulate an encouraging and helpful text response to their query.
                
                ACTION EXECUTION:
                If the user wants you to modify something in the app (like adding a specific exercise, updating nutrition targets, or changing active split), you can request an action execution by filling the "action" field in the JSON response.
                
                For adding an exercise:
                - If they describe a movement but do not know its name (e.g. "pull rope with cable on face like this"), recognize it as "Cable Face Pull" or another suitable exercise name.
                - Make sure to identify which day of the week (e.g. "Monday", "Tuesday", etc.) or workout day name (e.g. "Pull Day") they want to add it to.
                - Populate the ADD_EXERCISE action:
                  "action": {
                    "type": "ADD_EXERCISE",
                    "dayOfWeek": "Monday", 
                    "dayName": "Pull Day",
                    "exerciseName": "Cable Face Pulls",
                    "muscleGroup": "Shoulders",
                    "category": "Cable",
                    "equipment": "Cable Machine",
                    "difficulty": "Beginner",
                    "description": "Cable face pull is an outstanding shoulder isolation exercise focusing on rear delts and rotator cuffs.",
                    "executionSteps": "1. Set cable pulley to upper chest height with rope attachment.\n2. Hold rope ends, step back.\n3. Pull rope to face, flaring elbows out and squeezing upper back."
                  }

                For updating nutrition targets:
                - If they ask you to adjust their calories or targets, populate the SET_NUTRITION_TARGETS action:
                  "action": {
                    "type": "SET_NUTRITION_TARGETS",
                    "calories": 2100,
                    "protein": 160,
                    "carbs": 210,
                    "fat": 60
                  }

                Always respond ONLY with a raw JSON object matching this structure:
                {
                  "message": "A friendly conversational response under 100 words summarizing what you've done or explaining the exercise.",
                  "action": { ... } or null
                }
            """.trimIndent()

            val result = withContext(Dispatchers.IO) {
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "You are Gyani, the super-coach. You respond ONLY with raw JSON. No markdown, no enclosing tags."
                            )
                        )
                    )
                )

                try {
                    val response = GeminiServiceClient.api.generateWorkoutAdvice(apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                } catch (e: Exception) {
                    null
                }
            }

            if (result != null) {
                try {
                    val cleanedJson = result.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(GyaniResponse::class.java)
                    val parsedResponse = adapter.fromJson(cleanedJson)

                    if (parsedResponse != null) {
                        gyaniChatHistory = gyaniChatHistory + ChatMessage(text = parsedResponse.message, isUser = false)
                        
                        parsedResponse.action?.let { action ->
                            executeGyaniAction(action)
                        }
                    } else {
                        throw Exception("Failed to parse response")
                    }
                } catch (e: Exception) {
                    gyaniChatHistory = gyaniChatHistory + ChatMessage(text = "I processed your request, but had trouble parsing the action. Please try again with simpler words!", isUser = false)
                }
            } else {
                gyaniChatHistory = gyaniChatHistory + ChatMessage(text = "Unable to reach Gyani. Please check your internet connection.", isUser = false)
            }

            isGyaniAiLoading = false
        }
    }

    private fun executeGyaniAction(action: GyaniAction) {
        viewModelScope.launch {
            when (action.type.uppercase()) {
                "ADD_EXERCISE" -> {
                    val split = activeSplit.value ?: return@launch
                    val days = repository.getDaysForSplit(split.id).first()
                    
                    val targetDay = days.find { day ->
                        val dayOfWeekMatch = action.dayOfWeek != null && day.dayOfWeek.equals(action.dayOfWeek, ignoreCase = true)
                        val nameMatch = action.dayName != null && day.name.equals(action.dayName, ignoreCase = true)
                        dayOfWeekMatch || nameMatch
                    } ?: days.firstOrNull()

                    if (targetDay != null) {
                        val exerciseName = action.exerciseName ?: "Cable Face Pull"
                        val masterExercises = exercises.value
                        var matchingExercise = masterExercises.find { it.name.equals(exerciseName, ignoreCase = true) }
                        
                        if (matchingExercise == null) {
                            val newId = repository.insertExercise(
                                Exercise(
                                    name = exerciseName,
                                    category = action.category ?: "Cable",
                                    muscleGroup = action.muscleGroup ?: "Shoulders",
                                    equipment = action.equipment ?: "Cable Machine",
                                    difficulty = action.difficulty ?: "Beginner",
                                    description = action.description ?: "Added by Gyani AI",
                                    executionSteps = action.executionSteps ?: "",
                                    isCustom = true
                                )
                            )
                            matchingExercise = Exercise(
                                id = newId.toInt(),
                                name = exerciseName,
                                category = action.category ?: "Cable",
                                muscleGroup = action.muscleGroup ?: "Shoulders",
                                equipment = action.equipment ?: "Cable Machine"
                            )
                        }
                        
                        val dayExercises = repository.getExercisesForDay(targetDay.id).first()
                        val alreadyLinked = dayExercises.any { it.id == matchingExercise.id }
                        if (!alreadyLinked) {
                            repository.addExerciseToDay(targetDay.id, matchingExercise.id, 99)
                        }
                    }
                }
                "SET_NUTRITION_TARGETS" -> {
                    action.calories?.let { updateTargetCalories(it) }
                    action.protein?.let { updateTargetProtein(it) }
                    action.carbs?.let { updateTargetCarbs(it) }
                    action.fat?.let { updateTargetFat(it) }
                }
                "ACTIVATE_SPLIT" -> {
                    val splitList = splits.value
                    val matchingSplit = splitList.find { it.name.equals(action.splitName, ignoreCase = true) }
                    if (matchingSplit != null) {
                        activateSplit(matchingSplit)
                    }
                }
            }
        }
    }

    var nutritionChatHistory by mutableStateOf(listOf(
        ChatMessage(text = "Hello! I am AuraFit Nutrition AI. Tell me what you ate, e.g., 'I had 3 scrambled eggs and a banana for breakfast' or '100g of cooked chicken breast', and I'll estimate and log it for you!", isUser = false)
    ))
    var isNutritionAiLoading by mutableStateOf(false)

    fun addManualFoodLog(name: String, calories: Int, protein: Float, carbs: Float, fat: Float, mealType: String) {
        viewModelScope.launch {
            repository.insertFoodLog(
                FoodLog(
                    name = name,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    mealType = mealType
                )
            )
        }
    }

    fun deleteFoodLog(foodLog: FoodLog) {
        viewModelScope.launch {
            repository.deleteFoodLog(foodLog)
        }
    }

    fun clearAllFoodLogs() {
        viewModelScope.launch {
            repository.clearAllFoodLogs()
        }
    }

    fun sendNutritionChatMessage(userText: String) {
        if (userText.trim().isEmpty() || isNutritionAiLoading) return

        val userMessage = ChatMessage(text = userText, isUser = true)
        nutritionChatHistory = nutritionChatHistory + userMessage
        isNutritionAiLoading = true

        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                val errorMsg = "Gemini API key is not configured. Please add your key in the Secrets panel in AI Studio to get personalized nutrition recommendations."
                nutritionChatHistory = nutritionChatHistory + ChatMessage(text = errorMsg, isUser = false)
                isNutritionAiLoading = false
                return@launch
            }

            // Determine meal type dynamically based on current local time
            val currentTime = java.util.Calendar.getInstance()
            val hour = currentTime.get(java.util.Calendar.HOUR_OF_DAY)
            val suggestedMealType = when {
                hour < 11 -> "Breakfast"
                hour < 16 -> "Lunch"
                hour < 20 -> "Dinner"
                else -> "Snack"
            }

            val prompt = """
                The user says: "$userText".
                Suggesting meal type: $suggestedMealType.
                Estimate the food name(s), calories, protein (g), carbs (g), and fat (g) for the food item(s) described.
                Provide your response ONLY as a JSON object with two fields:
                1. "message": A friendly, encouraging message confirming what was parsed, their nutrition details (calories and protein), and greeting the user (under 80 words).
                2. "foods": A JSON array of food objects. Each food object must have:
                   - "name": String (be specific, e.g., "Scrambled Eggs (3 eggs)")
                   - "calories": Integer
                   - "protein": Float
                   - "carbs": Float
                   - "fat": Float
                   - "mealType": String (either "Breakfast", "Lunch", "Dinner", or "Snack")
                
                Ensure the JSON is perfectly valid. Do not wrap it in any markdown code blocks (such as ```json or ```). Return the raw JSON string only.
            """.trimIndent()

            val result = withContext(Dispatchers.IO) {
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "You are AuraFit Nutrition AI, an expert nutrition coach and calorie tracker. You respond ONLY with a raw JSON object containing 'message' (string) and 'foods' (array of objects)."
                            )
                        )
                    )
                )

                try {
                    val response = GeminiServiceClient.api.generateWorkoutAdvice(apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                } catch (e: Exception) {
                    null
                }
            }

            if (result != null) {
                try {
                    val cleanedJson = result.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(GeminiNutritionResponse::class.java)
                    val parsedResponse = adapter.fromJson(cleanedJson)

                    if (parsedResponse != null) {
                        nutritionChatHistory = nutritionChatHistory + ChatMessage(text = parsedResponse.message, isUser = false)
                        parsedResponse.foods.forEach { food ->
                            repository.insertFoodLog(
                                FoodLog(
                                    name = food.name,
                                    calories = food.calories,
                                    protein = food.protein,
                                    carbs = food.carbs,
                                    fat = food.fat,
                                    mealType = food.mealType
                                )
                            )
                        }
                    } else {
                        throw Exception("Failed to parse response")
                    }
                } catch (e: Exception) {
                    nutritionChatHistory = nutritionChatHistory + ChatMessage(text = "I estimated your food and logged it, but had a slight issue parsing details. Please try again or add manually!", isUser = false)
                }
            } else {
                nutritionChatHistory = nutritionChatHistory + ChatMessage(text = "Unable to reach AuraFit AI. Please check your internet connection or try again.", isUser = false)
            }
            isNutritionAiLoading = false
        }
    }
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeminiNutritionResponse(
    val message: String,
    val foods: List<GeminiNutritionFood>
)

data class GeminiNutritionFood(
    val name: String,
    val calories: Int,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val mealType: String = "Breakfast"
)

class WorkoutViewModelFactory(
    private val application: Application,
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@JsonClass(generateAdapter = true)
data class GeminiBodyAnalysisResponse(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val analysis: String
)

@JsonClass(generateAdapter = true)
data class GyaniResponse(
    val message: String,
    val action: GyaniAction? = null
)

@JsonClass(generateAdapter = true)
data class GyaniAction(
    val type: String,
    val dayOfWeek: String? = null,
    val dayName: String? = null,
    val exerciseName: String? = null,
    val muscleGroup: String? = null,
    val category: String? = null,
    val equipment: String? = null,
    val difficulty: String? = null,
    val description: String? = null,
    val executionSteps: String? = null,
    val calories: Int? = null,
    val protein: Int? = null,
    val carbs: Int? = null,
    val fat: Int? = null,
    val splitName: String? = null
)

data class SuggestedTargets(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val activeHours: Double,
    val mealFrequency: String
)
