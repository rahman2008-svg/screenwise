package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "day_stats")
data class DayStats(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val screenTimeMinutes: Int,
    val unlocks: Int,
    val notifications: Int,
    val focusScore: Int
)

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val limitMinutes: Int,
    val usedMinutesToday: Int,
    val isBlocked: Boolean
)

@Entity(tableName = "focus_modes")
data class FocusMode(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val durationMinutes: Int,
    val isActive: Boolean,
    val blockedApps: String // Comma separated packages
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitType: String, // WATER, WALK, EYE, STRETCH, MEDITATION
    val date: String, // yyyy-MM-dd
    val target: Int,
    val progress: Int
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val pinCode: String = "",
    val themeMode: String = "AMOLED Dark",
    val bedtimeStartHour: Int = 22,
    val bedtimeStartMin: Int = 0,
    val bedtimeEndHour: Int = 7,
    val bedtimeEndMin: Int = 0,
    val isSleepModeActive: Boolean = false,
    val grayscaleEnabled: Boolean = false,
    val dimEnabled: Boolean = false
)

@Dao
interface ScreenWiseDao {
    // Day Stats
    @Query("SELECT * FROM day_stats ORDER BY date DESC")
    fun getAllDayStats(): Flow<List<DayStats>>

    @Query("SELECT * FROM day_stats WHERE date = :date")
    suspend fun getDayStats(date: String): DayStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayStats(dayStats: DayStats)

    // App Limits
    @Query("SELECT * FROM app_limits")
    fun getAllAppLimitsFlow(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits")
    suspend fun getAllAppLimits(): List<AppLimit>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getAppLimit(packageName: String): AppLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppLimit(appLimit: AppLimit)

    @Delete
    suspend fun deleteAppLimit(appLimit: AppLimit)

    // Focus Modes
    @Query("SELECT * FROM focus_modes")
    fun getAllFocusModes(): Flow<List<FocusMode>>

    @Query("SELECT * FROM focus_modes WHERE id = :id")
    suspend fun getFocusMode(id: String): FocusMode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusMode(focusMode: FocusMode)

    // Habit Logs
    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getHabitLogsFlow(date: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE date = :date AND habitType = :habitType")
    suspend fun getHabitLog(date: String, habitType: String): HabitLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(habitLog: HabitLog)

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)
}

@Database(
    entities = [DayStats::class, AppLimit::class, FocusMode::class, HabitLog::class, UserProfile::class],
    version = 1,
    exportSchema = false
)
abstract class ScreenWiseDatabase : RoomDatabase() {
    abstract fun dao(): ScreenWiseDao

    companion object {
        @Volatile
        private var INSTANCE: ScreenWiseDatabase? = null

        fun getDatabase(context: Context): ScreenWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScreenWiseDatabase::class.java,
                    "screenwise_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
