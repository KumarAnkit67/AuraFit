package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_splits")
data class WorkoutSplit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCustom: Boolean = false,
    val isActive: Boolean = false
)

@Entity(tableName = "workout_days")
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val splitId: Int,
    val name: String, // e.g., "Push", "Pull", "Legs", "Rest"
    val dayOfWeek: String, // e.g., "Monday", "Tuesday", etc.
    val orderIndex: Int
)

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // e.g., "Barbell", "Dumbbell", "Bodyweight", "Machine", "Cable"
    val muscleGroup: String, // e.g., "Chest", "Back", "Legs", "Shoulders", "Arms", "Core"
    val secondaryMuscles: String = "", // Comma-separated list
    val equipment: String = "None",
    val difficulty: String = "Beginner", // e.g., "Beginner", "Intermediate", "Advanced"
    val notes: String = "",
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val youtubeUrl: String = "",
    val description: String = "",
    val executionSteps: String = "", // Comma-separated or newline-separated
    val commonMistakes: String = "" // Newline-separated
)

@Entity(tableName = "day_exercise_refs")
data class DayExerciseRef(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayId: Int,
    val exerciseId: Int,
    val orderIndex: Int
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val splitName: String,
    val dayName: String,
    val startTime: Long,
    val endTime: Long,
    val notes: String = "",
    val duration: Long = 0, // in seconds
    val totalVolume: Float = 0f,
    val totalReps: Int = 0
)

@Entity(tableName = "logged_sets")
data class LoggedSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val exerciseId: Int,
    val exerciseName: String,
    val setIndex: Int, // 1-based index
    val weight: Float,
    val reps: Int,
    val isCompleted: Boolean = false,
    val isWarmup: Boolean = false,
    val isDropSet: Boolean = false,
    val isFailure: Boolean = false,
    val rpe: Int? = null, // Rate of Perceived Exertion (1 to 10)
    val tempo: String? = null, // e.g., "3-0-1-0"
    val restTime: Int = 60 // rest timer duration in seconds
)

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val weight: Float, // in kg or lbs
    val bodyFat: Float = 0f, // percent
    val chest: Float = 0f,
    val waist: Float = 0f,
    val arms: Float = 0f,
    val thighs: Float = 0f,
    val calves: Float = 0f,
    val shoulders: Float = 0f,
    val neck: Float = 0f,
    val hip: Float = 0f,
    val progressPhotoPath: String? = null
)

@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int,
    val protein: Float = 0f, // in grams
    val carbs: Float = 0f,   // in grams
    val fat: Float = 0f,     // in grams
    val mealType: String = "Breakfast", // "Breakfast", "Lunch", "Dinner", "Snack"
    val timestamp: Long = System.currentTimeMillis()
)

