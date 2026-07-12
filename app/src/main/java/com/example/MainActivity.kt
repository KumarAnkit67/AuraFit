package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.WorkoutDatabase
import com.example.data.WorkoutRepository
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Database and Repository
        val database = WorkoutDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = WorkoutRepository(database.workoutDao())

        // 2. Instantiate central WorkoutViewModel via custom Factory
        val viewModel: WorkoutViewModel by viewModels {
            WorkoutViewModelFactory(application, repository)
        }

        // Initially show onboarding screen on launch if not completed
        viewModel.currentScreen = if (viewModel.isOnboardingCompleted) "dashboard" else "onboarding"

        setContent {
            MyApplicationTheme {
                MainLayout(viewModel = viewModel) { innerPadding ->
                    // State observing
                    val splitsList by viewModel.splits.collectAsState()
                    val activeSplitPlan by viewModel.activeSplit.collectAsState()
                    val sessionLogs by viewModel.sessions.collectAsState()
                    val allLibraryExercises by viewModel.exercises.collectAsState()
                    val allActiveSets = viewModel.activeSessionSets.value
                    val measurementLogs by viewModel.bodyMeasurements.collectAsState()

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                     ) { contentPadding ->
                        val screenModifier = Modifier.padding(contentPadding)

                        // State-based high-performance screen navigator (Strong/Hevy persist friendly!)
                        when (viewModel.currentScreen) {
                            "onboarding" -> {
                                OnboardingScreen(viewModel = viewModel, onGetStarted = { viewModel.currentScreen = "dashboard" })
                            }
                            "dashboard" -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    splits = splitsList,
                                    activeSplit = activeSplitPlan,
                                    sessions = sessionLogs
                                )
                            }
                            "splits" -> {
                                SplitsScreen(
                                    viewModel = viewModel,
                                    splits = splitsList
                                )
                            }
                            "split_day_details" -> {
                                val day = viewModel.selectedDay
                                val split = viewModel.selectedSplit
                                if (day != null && split != null) {
                                    SplitDayDetailsScreen(
                                        viewModel = viewModel,
                                        day = day,
                                        split = split
                                    )
                                } else {
                                    viewModel.currentScreen = "splits"
                                }
                            }
                            "exercise_library" -> {
                                ExerciseLibraryScreen(
                                    viewModel = viewModel,
                                    exercises = allLibraryExercises
                                )
                            }
                            "exercise_details" -> {
                                val exercise = viewModel.selectedExercise
                                if (exercise != null) {
                                    ExerciseDetailsScreen(
                                        viewModel = viewModel,
                                        exercise = exercise
                                    )
                                } else {
                                    viewModel.currentScreen = "exercise_library"
                                }
                            }
                            "active_workout" -> {
                                val session = viewModel.activeSession
                                if (session != null) {
                                    ActiveWorkoutScreen(
                                        viewModel = viewModel,
                                        session = session,
                                        sets = allActiveSets
                                    )
                                } else {
                                    viewModel.currentScreen = "dashboard"
                                }
                            }
                            "progress" -> {
                                ProgressScreen(
                                    viewModel = viewModel,
                                    sessions = sessionLogs,
                                    measurements = measurementLogs
                                )
                            }
                            "body_measurements" -> {
                                BodyMeasurementsScreen(
                                    viewModel = viewModel,
                                    measurements = measurementLogs
                                )
                            }
                            "settings" -> {
                                SettingsScreen(viewModel = viewModel)
                            }
                            "ai_coach" -> {
                                AICoachScreen(viewModel = viewModel)
                            }
                            "nutrition" -> {
                                val foodLogsList by viewModel.foodLogs.collectAsState()
                                NutritionScreen(
                                    viewModel = viewModel,
                                    foodLogs = foodLogsList
                                )
                            }
                            else -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    splits = splitsList,
                                    activeSplit = activeSplitPlan,
                                    sessions = sessionLogs
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
