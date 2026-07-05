package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // --- Splits & Days ---
    @Query("SELECT * FROM workout_splits ORDER BY id ASC")
    fun getAllSplits(): Flow<List<WorkoutSplit>>

    @Query("SELECT * FROM workout_splits WHERE isActive = 1 LIMIT 1")
    fun getActiveSplit(): Flow<WorkoutSplit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: WorkoutSplit): Long

    @Update
    suspend fun updateSplit(split: WorkoutSplit)

    @Delete
    suspend fun deleteSplit(split: WorkoutSplit)

    @Query("UPDATE workout_splits SET isActive = 0")
    suspend fun deactivateAllSplits()

    @Query("SELECT * FROM workout_days WHERE splitId = :splitId ORDER BY orderIndex ASC")
    fun getDaysForSplit(splitId: Int): Flow<List<WorkoutDay>>

    @Query("SELECT * FROM workout_days WHERE id = :dayId LIMIT 1")
    suspend fun getDayById(dayId: Int): WorkoutDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: WorkoutDay): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<WorkoutDay>)

    @Update
    suspend fun updateDay(day: WorkoutDay)

    @Delete
    suspend fun deleteDay(day: WorkoutDay)

    @Query("DELETE FROM workout_days WHERE splitId = :splitId")
    suspend fun deleteDaysForSplit(splitId: Int)

    // --- Exercises ---
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: Int): Flow<Exercise?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    // --- Day Exercise References (JOINs) ---
    @Query("""
        SELECT exercises.* FROM exercises 
        INNER JOIN day_exercise_refs ON exercises.id = day_exercise_refs.exerciseId 
        WHERE day_exercise_refs.dayId = :dayId 
        ORDER BY day_exercise_refs.orderIndex ASC
    """)
    fun getExercisesForDay(dayId: Int): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayExerciseRef(ref: DayExerciseRef): Long

    @Query("DELETE FROM day_exercise_refs WHERE dayId = :dayId")
    suspend fun deleteDayExerciseRefsForDay(dayId: Int)

    @Query("DELETE FROM day_exercise_refs WHERE dayId = :dayId AND exerciseId = :exerciseId")
    suspend fun deleteDayExerciseRef(dayId: Int, exerciseId: Int)

    // --- Workout Sessions & Logged Sets ---
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: Int): Flow<WorkoutSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Query("SELECT * FROM logged_sets ORDER BY sessionId DESC, id ASC")
    fun getAllLoggedSets(): Flow<List<LoggedSet>>

    @Query("SELECT * FROM logged_sets WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getSetsForSession(sessionId: Int): Flow<List<LoggedSet>>

    @Query("SELECT * FROM logged_sets WHERE exerciseId = :exerciseId ORDER BY sessionId DESC")
    fun getSetsForExercise(exerciseId: Int): Flow<List<LoggedSet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedSet(set: LoggedSet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedSets(sets: List<LoggedSet>)

    @Query("DELETE FROM logged_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: Int)

    // --- Body Measurements ---
    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC")
    fun getAllBodyMeasurements(): Flow<List<BodyMeasurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMeasurement(measurement: BodyMeasurement): Long

    @Delete
    suspend fun deleteBodyMeasurement(measurement: BodyMeasurement)

    // --- Food Logs ---
    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun getAllFoodLogs(): Flow<List<FoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(foodLog: FoodLog): Long

    @Delete
    suspend fun deleteFoodLog(foodLog: FoodLog)

    @Query("DELETE FROM food_logs")
    suspend fun clearAllFoodLogs()
}
