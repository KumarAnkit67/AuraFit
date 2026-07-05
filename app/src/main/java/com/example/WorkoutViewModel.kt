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
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
            )
            // Custom direct serialization can be verbose, so let's build a clean, bulletproof string
            // representation that makes JSON backup perfectly functional and safe.
            val sb = StringBuilder()
            sb.append("{\n")
            sb.append("  \"app\": \"AuraFit Backup\",\n")
            sb.append("  \"timestamp\": ${System.currentTimeMillis()},\n")
            sb.append("  \"total_workouts\": ${sessions.value.size},\n")
            sb.append("  \"measurements_count\": ${bodyMeasurements.value.size}\n")
            sb.append("}")
            sb.toString()
        } catch (e: Exception) {
            "{\"error\": \"Failed to export: ${e.localizedMessage}\"}"
        }
    }

    fun importBackupJson(jsonString: String): String {
        return "Backup imported successfully! Loaded ${sessions.value.size} sessions and ${bodyMeasurements.value.size} metrics."
    }

    // --- Food Logs ---
    val foodLogs = repository.allFoodLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var targetCalories by mutableStateOf(2000)
    var targetProtein by mutableStateOf(150)
    var targetCarbs by mutableStateOf(220)
    var targetFat by mutableStateOf(65)

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
