package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WorkoutSplit::class,
        WorkoutDay::class,
        Exercise::class,
        DayExerciseRef::class,
        WorkoutSession::class,
        LoggedSet::class,
        BodyMeasurement::class,
        FoodLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(WorkoutDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class WorkoutDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.workoutDao()
                    populateExercises(dao)
                    populateSplitsAndDays(dao)
                }
            }
        }

        private suspend fun populateExercises(dao: WorkoutDao) {
            val exercises = listOf(
                Exercise(
                    id = 1,
                    name = "Bench Press",
                    category = "Barbell",
                    muscleGroup = "Chest",
                    equipment = "Barbell",
                    difficulty = "Intermediate",
                    secondaryMuscles = "Triceps, Shoulders",
                    youtubeUrl = "https://www.youtube.com/watch?v=gRVjAtPip0Y",
                    description = "A classic barbell exercise that targets the pectoral muscles, triceps, and anterior deltoids.",
                    executionSteps = "1. Lie flat on the bench under the barbell.\n2. Grip the barbell slightly wider than shoulder-width.\n3. Unrack the bar and slowly lower it to your chest.\n4. Drive your feet into the floor and press the barbell back up.",
                    commonMistakes = "- Bouncing the bar off your chest.\n- Flaring your elbows out excessively.\n- Lifting your hips off the bench."
                ),
                Exercise(
                    id = 2,
                    name = "Incline Dumbbell Press",
                    category = "Dumbbell",
                    muscleGroup = "Chest",
                    equipment = "Dumbbells",
                    difficulty = "Intermediate",
                    secondaryMuscles = "Shoulders, Triceps",
                    youtubeUrl = "https://www.youtube.com/watch?v=0G2_XP0g2gM",
                    description = "Pressing dumbbells on an inclined bench puts more emphasis on the upper head of the chest (clavicular head).",
                    executionSteps = "1. Set an incline bench to 30-45 degrees.\n2. Sit back with a dumbbell in each hand, placed on your thighs.\n3. Raise the dumbbells to shoulder height and press them up until arms are extended.\n4. Lower the weights under control back to chest height.",
                    commonMistakes = "- Arching the lower back excessively.\n- Clashing the weights together at the top.\n- Incomplete range of motion."
                ),
                Exercise(
                    id = 3,
                    name = "Pull-Ups",
                    category = "Bodyweight",
                    muscleGroup = "Back",
                    equipment = "Pull-up Bar",
                    difficulty = "Hard",
                    secondaryMuscles = "Biceps, Forearms",
                    youtubeUrl = "https://www.youtube.com/watch?v=eGo4IYlbE5g",
                    description = "An excellent upper body compound exercise targeting the latissimus dorsi and pulling muscles.",
                    executionSteps = "1. Hang from a pull-up bar with hands wider than shoulder-width, palms facing away.\n2. Pull your body upward by driving your elbows down toward your sides.\n3. Elevate until your chin clears the bar.\n4. Lower your body slowly back to a dead hang.",
                    commonMistakes = "- Using momentum or kicking with your legs.\n- Shrugging your shoulders near the top.\n- Half reps at the bottom."
                ),
                Exercise(
                    id = 4,
                    name = "Barbell Row",
                    category = "Barbell",
                    muscleGroup = "Back",
                    equipment = "Barbell",
                    difficulty = "Intermediate",
                    secondaryMuscles = "Biceps, Rear Deltoids",
                    youtubeUrl = "https://www.youtube.com/watch?v=I0jtX7-S_o8",
                    description = "Builds thickness in the upper and middle back while training spinal stability.",
                    executionSteps = "1. Stand with feet shoulder-width apart, hinge forward at the hips while keeping a flat back.\n2. Hold the barbell with an overhand grip.\n3. Pull the barbell toward your lower ribs, squeeze your shoulder blades.\n4. Return the bar to the starting position under control.",
                    commonMistakes = "- Rounding the spine or lower back.\n- Shrugging the bar or using leg bounce.\n- Pulling with wrists rather than elbows."
                ),
                Exercise(
                    id = 5,
                    name = "Barbell Back Squat",
                    category = "Barbell",
                    muscleGroup = "Legs",
                    equipment = "Squat Rack",
                    difficulty = "Hard",
                    secondaryMuscles = "Glutes, Hamstrings",
                    youtubeUrl = "https://www.youtube.com/watch?v=Uv_K7mFfCjE",
                    description = "The king of all lower body exercises, training the quadriceps, glutes, and full core stability.",
                    executionSteps = "1. Set the barbell in a rack at chest height. Step under and rest it across your upper back trap muscles.\n2. Unrack, step back, and stand with feet slightly wider than shoulder-width.\n3. Hinge hips back and bend knees, lowering until your thighs are parallel to the floor (or deeper).\n4. Push through your mid-foot to stand up.",
                    commonMistakes = "- Knee cave-in (valgus collapse).\n- Heels lifting off the floor.\n- Rounded lower spine (butt wink)."
                ),
                Exercise(
                    id = 6,
                    name = "Romanian Deadlift",
                    category = "Barbell",
                    muscleGroup = "Legs",
                    equipment = "Barbell",
                    difficulty = "Intermediate",
                    secondaryMuscles = "Hamstrings, Glutes, Lower Back",
                    youtubeUrl = "https://www.youtube.com/watch?v=JCXUYuzw01M",
                    description = "Focuses on hip hinging to build powerful hamstrings and glutes.",
                    executionSteps = "1. Stand with a barbell at your hips. Keep your chest up and shoulders back.\n2. Push your hips backward, keeping a slight bend in your knees as you lower the barbell along your shins.\n3. Lower until you feel a deep stretch in your hamstrings, then contract your glutes to drive hips forward to the starting position.",
                    commonMistakes = "- Rounding the lower back.\n- Bending knees too much (making it a squat).\n- Moving the bar away from your body."
                ),
                Exercise(
                    id = 7,
                    name = "Overhead Press",
                    category = "Barbell",
                    muscleGroup = "Shoulders",
                    equipment = "Barbell",
                    difficulty = "Intermediate",
                    secondaryMuscles = "Triceps, Upper Chest",
                    youtubeUrl = "https://www.youtube.com/watch?v=2yjwHev9jGM",
                    description = "Builds solid, functional shoulder strength and core stability.",
                    executionSteps = "1. Set the barbell at upper chest height. Hold it with hands shoulder-width apart.\n2. Keep your glutes and abs extremely tight.\n3. Press the bar straight overhead, moving your face back slightly as it passes, then locking out at the top.\n4. Lower back down to collarbone height.",
                    commonMistakes = "- Leaning back excessively.\n- Flaring elbows out wide.\n- Using legs to push (which turns it into a push press)."
                ),
                Exercise(
                    id = 8,
                    name = "Dumbbell Lateral Raise",
                    category = "Dumbbell",
                    muscleGroup = "Shoulders",
                    equipment = "Dumbbells",
                    difficulty = "Beginner",
                    secondaryMuscles = "Trapezius",
                    youtubeUrl = "https://www.youtube.com/watch?v=3VcKaXpzqRo",
                    description = "Isolates the lateral head of the deltoid for capped, broad shoulders.",
                    executionSteps = "1. Stand tall holding dumbbells at your sides, palms facing inward.\n2. Raise your arms out to the sides in a slight forward diagonal (scapular plane).\n3. Lift until your arms are parallel to the ground, with pinkies tilted up slightly.\n4. Lower the weights slowly.",
                    commonMistakes = "- Swinging the body for momentum.\n- Lifting dumbbells above shoulder level.\n- Shrugging up with the traps."
                ),
                Exercise(
                    id = 9,
                    name = "Dumbbell Bicep Curl",
                    category = "Dumbbell",
                    muscleGroup = "Arms",
                    equipment = "Dumbbells",
                    difficulty = "Beginner",
                    secondaryMuscles = "Forearms",
                    youtubeUrl = "https://www.youtube.com/watch?v=ykJmrZ5v0Oo",
                    description = "Classic biceps building exercise.",
                    executionSteps = "1. Stand holding dumbbells, palms facing forward.\n2. Keep your elbows tucked close to your torso.\n3. Squeeze biceps to curl dumbbells up to shoulder level.\n4. Lower back down slowly.",
                    commonMistakes = "- Moving the elbows forward.\n- Swinging weights using hip momentum."
                ),
                Exercise(
                    id = 10,
                    name = "Cable Tricep Pushdown",
                    category = "Cable",
                    muscleGroup = "Arms",
                    equipment = "Cable Machine",
                    difficulty = "Beginner",
                    secondaryMuscles = "None",
                    youtubeUrl = "https://www.youtube.com/watch?v=2-LAMcpzODU",
                    description = "Isolates the triceps with constant tension.",
                    executionSteps = "1. Attach a rope or straight bar to a high cable pulley.\n2. Keep elbows tucked in at 90-degree flexion.\n3. Contract triceps to push the bar/rope down until arms are fully locked out.\n4. Return slowly to 90 degrees.",
                    commonMistakes = "- Allowing elbows to flare out or move forward.\n- Rounding shoulders to press using bodyweight."
                ),
                Exercise(
                    id = 11,
                    name = "Warm Up / Mobility",
                    category = "Bodyweight",
                    muscleGroup = "Full Body",
                    equipment = "None",
                    difficulty = "Beginner",
                    secondaryMuscles = "None",
                    description = "Dynamic stretching and mobility drills to prepare your joints and muscles for a lifting session.",
                    executionSteps = "1. Spend 5-10 minutes doing arm circles, leg swings, hip openers, and cat-cows.\n2. Do light, dynamic movements mimicking the lifts you are about to perform.",
                    commonMistakes = "- Doing static stretching before dynamic work.\n- Rushing through and not raising core body temperature."
                ),
                Exercise(
                    id = 12,
                    name = "Barbell Smith Flat Press",
                    category = "Machine",
                    muscleGroup = "Chest",
                    equipment = "Smith Machine",
                    difficulty = "Intermediate",
                    secondaryMuscles = "Triceps, Shoulders",
                    description = "A flat bench press performed on a Smith machine, offering increased stability and path control.",
                    executionSteps = "1. Lie flat on a bench centered under the Smith machine bar.\n2. Grip the bar slightly wider than shoulder width.\n3. Unrack the bar and lower it slowly under control to your mid-chest.\n4. Press the bar straight up to lockout.",
                    commonMistakes = "- Bouncing the bar off your chest.\n- Setting the bench off-center from the guide rails."
                )
            )
            for (exercise in exercises) {
                dao.insertExercise(exercise)
            }
        }

        private suspend fun populateSplitsAndDays(dao: WorkoutDao) {
            // 1. Push Pull Legs
            val pplSplitId = dao.insertSplit(WorkoutSplit(name = "Push Pull Legs", isCustom = false, isActive = true))
            val pplDays = listOf(
                WorkoutDay(splitId = pplSplitId.toInt(), name = "Push Day", dayOfWeek = "Monday", orderIndex = 0),
                WorkoutDay(splitId = pplSplitId.toInt(), name = "Pull Day", dayOfWeek = "Tuesday", orderIndex = 1),
                WorkoutDay(splitId = pplSplitId.toInt(), name = "Legs Day", dayOfWeek = "Wednesday", orderIndex = 2),
                WorkoutDay(splitId = pplSplitId.toInt(), name = "Rest Day", dayOfWeek = "Thursday", orderIndex = 3),
                WorkoutDay(splitId = pplSplitId.toInt(), name = "Push Day", dayOfWeek = "Friday", orderIndex = 4),
                WorkoutDay(splitId = pplSplitId.toInt(), name = "Pull Day", dayOfWeek = "Saturday", orderIndex = 5),
                WorkoutDay(splitId = pplSplitId.toInt(), name = "Legs Day", dayOfWeek = "Sunday", orderIndex = 6)
            )
            dao.insertDays(pplDays)

            // Let's pre-link some exercises for PPL
            // We need the day IDs.
            // Since they are generated sequentially, let's look up or query days, but we can also insert day-exercise refs if we want.
            // Let's query days for this split
            val pplDaysFromDb = mutableListOf<WorkoutDay>()
            // Since this is initialization and we inserted them, the IDs will be 1 to 7.
            // Monday Push Day (Day ID 1): Warm Up (ID 11), Incline Dumbbell Press (ID 2), Barbell Smith Flat Press (ID 12), Bench Press (ID 1), Lateral Raise (ID 8), Tricep Pushdown (ID 10)
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 1, exerciseId = 11, orderIndex = 0))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 1, exerciseId = 2, orderIndex = 1))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 1, exerciseId = 12, orderIndex = 2))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 1, exerciseId = 1, orderIndex = 3))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 1, exerciseId = 8, orderIndex = 4))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 1, exerciseId = 10, orderIndex = 5))

            // Tuesday Pull Day (Day ID 2): Pull-ups (ID 3), Barbell Row (ID 4), Lat Pulldown (custom ID or Bicep curl ID 9)
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 2, exerciseId = 3, orderIndex = 0))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 2, exerciseId = 4, orderIndex = 1))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 2, exerciseId = 9, orderIndex = 2))

            // Wednesday Legs Day (Day ID 3): Squat (ID 5), Romanian Deadlift (ID 6)
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 3, exerciseId = 5, orderIndex = 0))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 3, exerciseId = 6, orderIndex = 1))

            // Friday Push Day (Day ID 5)
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 5, exerciseId = 11, orderIndex = 0))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 5, exerciseId = 2, orderIndex = 1))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 5, exerciseId = 12, orderIndex = 2))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 5, exerciseId = 1, orderIndex = 3))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 5, exerciseId = 8, orderIndex = 4))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 5, exerciseId = 10, orderIndex = 5))

            // Saturday Pull Day (Day ID 6)
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 6, exerciseId = 3, orderIndex = 0))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 6, exerciseId = 4, orderIndex = 1))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 6, exerciseId = 9, orderIndex = 2))

            // Sunday Legs Day (Day ID 7)
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 7, exerciseId = 5, orderIndex = 0))
            dao.insertDayExerciseRef(DayExerciseRef(dayId = 7, exerciseId = 6, orderIndex = 1))

            // 2. Bro Split
            val broSplitId = dao.insertSplit(WorkoutSplit(name = "Bro Split", isCustom = false, isActive = false))
            val broDays = listOf(
                WorkoutDay(splitId = broSplitId.toInt(), name = "Chest Day", dayOfWeek = "Monday", orderIndex = 0),
                WorkoutDay(splitId = broSplitId.toInt(), name = "Back Day", dayOfWeek = "Tuesday", orderIndex = 1),
                WorkoutDay(splitId = broSplitId.toInt(), name = "Shoulders Day", dayOfWeek = "Wednesday", orderIndex = 2),
                WorkoutDay(splitId = broSplitId.toInt(), name = "Legs Day", dayOfWeek = "Thursday", orderIndex = 3),
                WorkoutDay(splitId = broSplitId.toInt(), name = "Arms Day", dayOfWeek = "Friday", orderIndex = 4),
                WorkoutDay(splitId = broSplitId.toInt(), name = "Rest Day", dayOfWeek = "Saturday", orderIndex = 5),
                WorkoutDay(splitId = broSplitId.toInt(), name = "Rest Day", dayOfWeek = "Sunday", orderIndex = 6)
            )
            dao.insertDays(broDays)

            // 3. Upper Lower Split
            val ulSplitId = dao.insertSplit(WorkoutSplit(name = "Upper Lower", isCustom = false, isActive = false))
            val ulDays = listOf(
                WorkoutDay(splitId = ulSplitId.toInt(), name = "Upper Body", dayOfWeek = "Monday", orderIndex = 0),
                WorkoutDay(splitId = ulSplitId.toInt(), name = "Lower Body", dayOfWeek = "Tuesday", orderIndex = 1),
                WorkoutDay(splitId = ulSplitId.toInt(), name = "Rest Day", dayOfWeek = "Wednesday", orderIndex = 2),
                WorkoutDay(splitId = ulSplitId.toInt(), name = "Upper Body", dayOfWeek = "Thursday", orderIndex = 3),
                WorkoutDay(splitId = ulSplitId.toInt(), name = "Lower Body", dayOfWeek = "Friday", orderIndex = 4),
                WorkoutDay(splitId = ulSplitId.toInt(), name = "Rest Day", dayOfWeek = "Saturday", orderIndex = 5),
                WorkoutDay(splitId = ulSplitId.toInt(), name = "Rest Day", dayOfWeek = "Sunday", orderIndex = 6)
            )
            dao.insertDays(ulDays)
        }
    }
}
