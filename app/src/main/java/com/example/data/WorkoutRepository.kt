package com.example.data

import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val dao: WorkoutDao) {

    // --- Splits & Days ---
    val allSplits: Flow<List<WorkoutSplit>> = dao.getAllSplits()
    val activeSplit: Flow<WorkoutSplit?> = dao.getActiveSplit()

    fun getDaysForSplit(splitId: Int): Flow<List<WorkoutDay>> = dao.getDaysForSplit(splitId)

    suspend fun getDayById(dayId: Int): WorkoutDay? = dao.getDayById(dayId)

    suspend fun insertSplit(split: WorkoutSplit): Long = dao.insertSplit(split)

    suspend fun updateSplit(split: WorkoutSplit) = dao.updateSplit(split)

    suspend fun deleteSplit(split: WorkoutSplit) {
        dao.deleteDaysForSplit(split.id)
        dao.deleteSplit(split)
    }

    suspend fun activateSplit(splitId: Int) {
        dao.deactivateAllSplits()
        val splits = mutableListOf<WorkoutSplit>()
        // Let's activate this split. Since updateSplit requires the object, we deactivate all then update
        // this one.
    }

    suspend fun updateSplitStatus(split: WorkoutSplit, isActive: Boolean) {
        if (isActive) {
            dao.deactivateAllSplits()
        }
        dao.updateSplit(split.copy(isActive = isActive))
    }

    suspend fun insertDay(day: WorkoutDay): Long = dao.insertDay(day)
    suspend fun updateDay(day: WorkoutDay) = dao.updateDay(day)
    suspend fun deleteDay(day: WorkoutDay) = dao.deleteDay(day)

    // --- Exercises ---
    val allExercises: Flow<List<Exercise>> = dao.getAllExercises()

    fun getExerciseById(exerciseId: Int): Flow<Exercise?> = dao.getExerciseById(exerciseId)

    suspend fun insertExercise(exercise: Exercise): Long = dao.insertExercise(exercise)

    suspend fun updateExercise(exercise: Exercise) = dao.updateExercise(exercise)

    suspend fun deleteExercise(exercise: Exercise) = dao.deleteExercise(exercise)

    // --- Day Exercise References ---
    fun getExercisesForDay(dayId: Int): Flow<List<Exercise>> = dao.getExercisesForDay(dayId)

    suspend fun addExerciseToDay(dayId: Int, exerciseId: Int, orderIndex: Int) {
        dao.insertDayExerciseRef(DayExerciseRef(dayId = dayId, exerciseId = exerciseId, orderIndex = orderIndex))
    }

    suspend fun removeExerciseFromDay(dayId: Int, exerciseId: Int) {
        dao.deleteDayExerciseRef(dayId, exerciseId)
    }

    suspend fun updateDayExercises(dayId: Int, exerciseIds: List<Int>) {
        dao.deleteDayExerciseRefsForDay(dayId)
        exerciseIds.forEachIndexed { index, exerciseId ->
            dao.insertDayExerciseRef(DayExerciseRef(dayId = dayId, exerciseId = exerciseId, orderIndex = index))
        }
    }

    // --- Workout Sessions & Logged Sets ---
    val allSessions: Flow<List<WorkoutSession>> = dao.getAllSessions()
    val allLoggedSets: Flow<List<LoggedSet>> = dao.getAllLoggedSets()

    fun getSessionById(sessionId: Int): Flow<WorkoutSession?> = dao.getSessionById(sessionId)

    fun getSetsForSession(sessionId: Int): Flow<List<LoggedSet>> = dao.getSetsForSession(sessionId)

    fun getSetsForExercise(exerciseId: Int): Flow<List<LoggedSet>> = dao.getSetsForExercise(exerciseId)

    suspend fun saveWorkoutSession(session: WorkoutSession, sets: List<LoggedSet>): Long {
        val sessionId = dao.insertSession(session)
        val setsWithSessionId = sets.map { it.copy(sessionId = sessionId.toInt()) }
        dao.insertLoggedSets(setsWithSessionId)
        return sessionId
    }

    suspend fun deleteSession(session: WorkoutSession) {
        dao.deleteSetsForSession(session.id)
        dao.deleteSession(session)
    }

    // --- Body Measurements ---
    val allBodyMeasurements: Flow<List<BodyMeasurement>> = dao.getAllBodyMeasurements()

    suspend fun insertBodyMeasurement(measurement: BodyMeasurement): Long = dao.insertBodyMeasurement(measurement)

    suspend fun deleteBodyMeasurement(measurement: BodyMeasurement) = dao.deleteBodyMeasurement(measurement)

    // --- Food Logs ---
    val allFoodLogs: Flow<List<FoodLog>> = dao.getAllFoodLogs()

    suspend fun insertFoodLog(foodLog: FoodLog): Long = dao.insertFoodLog(foodLog)

    suspend fun deleteFoodLog(foodLog: FoodLog) = dao.deleteFoodLog(foodLog)

    suspend fun clearAllFoodLogs() = dao.clearAllFoodLogs()
}
