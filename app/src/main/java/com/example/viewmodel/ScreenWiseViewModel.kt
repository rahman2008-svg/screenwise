package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.model.AppUsageItem
import com.example.data.repository.ScreenWiseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScreenWiseViewModel(private val repository: ScreenWiseRepository) : ViewModel() {

    private val _appUsageList = MutableStateFlow<List<AppUsageItem>>(emptyList())
    val appUsageList: StateFlow<List<AppUsageItem>> = _appUsageList.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    val allDayStats: StateFlow<List<DayStats>> = repository.allDayStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAppLimits: StateFlow<List<AppLimit>> = repository.allAppLimits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFocusModes: StateFlow<List<FocusMode>> = repository.allFocusModes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Habit logs for today
    private val _todayHabitLogs = MutableStateFlow<List<HabitLog>>(emptyList())
    val todayHabitLogs: StateFlow<List<HabitLog>> = _todayHabitLogs.asStateFlow()

    // Active focus mode countdown state
    private val _activeFocusMode = MutableStateFlow<FocusMode?>(null)
    val activeFocusMode: StateFlow<FocusMode?> = _activeFocusMode.asStateFlow()

    private val _focusTimeRemaining = MutableStateFlow(0)
    val focusTimeRemaining: StateFlow<Int> = _focusTimeRemaining.asStateFlow()

    // Onboarding status flag
    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    // Daily Recommendations/Advice
    private val _dailyAdvice = MutableStateFlow<List<String>>(emptyList())
    val dailyAdvice: StateFlow<List<String>> = _dailyAdvice.asStateFlow()

    // Active Sleep Mode music playback state (Simulated)
    private val _isPlayingSleepMusic = MutableStateFlow(false)
    val isPlayingSleepMusic: StateFlow<Boolean> = _isPlayingSleepMusic.asStateFlow()

    private val _activeSleepTrack = MutableStateFlow("Rain Forest")
    val activeSleepTrack: StateFlow<String> = _activeSleepTrack.asStateFlow()

    // Security PIN screen state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    init {
        // Load initial profile & details
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            _isOnboardingCompleted.value = profile.pinCode != "NOT_COMPLETED" && profile.coins > 0 // simple check if set up
            
            // Check if app has PIN and lock initially if set
            if (profile.pinCode.isNotEmpty()) {
                _isAppLocked.value = true
            }

            // Load today's stats & habits
            val today = getTodayDateString()
            loadTodayHabits(today)
            generateDailyAdvice()
            insertSampleHistoryIfNeeded()
        }
        
        // Focus timer ticker
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _activeFocusMode.value
                if (current != null && _focusTimeRemaining.value > 0) {
                    _focusTimeRemaining.value -= 1
                    if (_focusTimeRemaining.value == 0) {
                        completeFocusSession(current)
                    }
                }
            }
        }
    }

    fun updatePermissionStatus(context: Context) {
        _isPermissionGranted.value = repository.isUsagePermissionGranted(context)
    }

    fun loadAppUsage(context: Context) {
        viewModelScope.launch {
            _isPermissionGranted.value = repository.isUsagePermissionGranted(context)
            val usage = repository.queryAppUsage(context)
            _appUsageList.value = usage

            // Save aggregate to DayStats database
            val totalMinutes = usage.sumOf { it.screenTimeMinutes }
            val today = getTodayDateString()
            val existing = repository.allDayStats.firstOrNull()?.find { it.date == today }
            val unlocks = existing?.unlocks ?: 14
            val notifications = existing?.notifications ?: 42
            
            // Calculate Focus Score (dynamic balance of screen time vs limit breaches)
            val focusScore = (100 - (totalMinutes / 10) - (unlocks / 2)).coerceIn(10, 100)

            repository.insertDayStats(
                DayStats(
                    date = today,
                    screenTimeMinutes = totalMinutes,
                    unlocks = unlocks,
                    notifications = notifications,
                    focusScore = focusScore
                )
            )
        }
    }

    private fun loadTodayHabits(date: String) {
        viewModelScope.launch {
            repository.getHabitLogs(date).collect { logs ->
                _todayHabitLogs.value = logs
            }
        }
    }

    fun logHabitProgress(habitType: String, delta: Int, target: Int) {
        viewModelScope.launch {
            repository.updateHabitProgress(getTodayDateString(), habitType, delta, target)
        }
    }

    fun completeOnboarding(theme: String, pin: String) {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile().copy(
                themeMode = theme,
                pinCode = pin,
                coins = 100, // starting coins
                xp = 50,
                streak = 1
            )
            repository.updateProfile(profile)
            _isOnboardingCompleted.value = true
            _isAppLocked.value = false
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            repository.updateProfile(profile.copy(themeMode = theme))
        }
    }

    fun updateBedtimeSettings(startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            repository.updateProfile(profile.copy(
                bedtimeStartHour = startHour,
                bedtimeStartMin = startMin,
                bedtimeEndHour = endHour,
                bedtimeEndMin = endMin
            ))
        }
    }

    fun toggleSleepMode(active: Boolean) {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            repository.updateProfile(profile.copy(isSleepModeActive = active))
            if (active) {
                // Reward for sleep preparation
                repository.rewardCoinsAndXp(10, 20)
            }
        }
    }

    fun toggleGrayscale(active: Boolean) {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            repository.updateProfile(profile.copy(grayscaleEnabled = active))
        }
    }

    fun toggleScreenDim(active: Boolean) {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            repository.updateProfile(profile.copy(dimEnabled = active))
        }
    }

    fun playSleepMusic(track: String) {
        _activeSleepTrack.value = track
        _isPlayingSleepMusic.value = true
    }

    fun stopSleepMusic() {
        _isPlayingSleepMusic.value = false
    }

    // App Limits Setup
    fun setAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            repository.updateAppLimit(
                AppLimit(
                    packageName = packageName,
                    appName = appName,
                    limitMinutes = limitMinutes,
                    usedMinutesToday = 0,
                    isBlocked = false
                )
            )
            repository.rewardCoinsAndXp(10, 15) // bonus for styling self-control
        }
    }

    fun removeAppLimit(limit: AppLimit) {
        viewModelScope.launch {
            repository.deleteAppLimit(limit)
        }
    }

    // Focus Mode Controls
    fun startFocusMode(name: String, durationMinutes: Int, blockApps: String) {
        viewModelScope.launch {
            val focusMode = FocusMode(
                id = name.lowercase().replace(" ", "_"),
                name = name,
                icon = "🎯",
                durationMinutes = durationMinutes,
                isActive = true,
                blockedApps = blockApps
            )
            repository.updateFocusMode(focusMode)
            _activeFocusMode.value = focusMode
            _focusTimeRemaining.value = durationMinutes * 60
        }
    }

    fun stopFocusMode() {
        _activeFocusMode.value = null
        _focusTimeRemaining.value = 0
    }

    private fun completeFocusSession(mode: FocusMode) {
        viewModelScope.launch {
            _activeFocusMode.value = null
            _focusTimeRemaining.value = 0
            // Complete active focus
            val updated = mode.copy(isActive = false)
            repository.updateFocusMode(updated)
            // Reward high XP and coins!
            val coinsReward = mode.durationMinutes / 2 + 5
            val xpReward = mode.durationMinutes * 2
            repository.rewardCoinsAndXp(coinsReward, xpReward)
        }
    }

    // PIN Authentication
    fun authenticatePin(enteredPin: String): Boolean {
        val currentProfile = userProfile.value
        return if (currentProfile?.pinCode == enteredPin) {
            _isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    // Smart Daily Advice Generation
    private fun generateDailyAdvice() {
        val adviceList = mutableListOf(
            "আজ আপনি গতকালের তুলনায় ১ ঘণ্টা কম ফোন ব্যবহার করেছেন। চমৎকার গতি!",
            "আজ রাত ১১টার পর ফোন ব্যবহার না করার চেষ্টা করুন। ভালো ঘুমের জন্য অত্যন্ত জরুরি।",
            "আপনার ফোকাস স্কোর আজ ৮৫% এ পৌঁছেছে! আপনি দুর্দান্ত কাজ করছেন।",
            "সামাজিক যোগাযোগ মাধ্যম অ্যাপে আপনার স্ক্রিন টাইম আজ ১৫% কমানো সম্ভব হয়েছে।"
        )
        _dailyAdvice.value = adviceList
    }

    // Insert dummy/sample histories for graph display initially if empty
    private suspend fun insertSampleHistoryIfNeeded() {
        val all = repository.allDayStats.firstOrNull() ?: emptyList()
        if (all.isEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            
            // Insert last 7 days of sample data
            for (i in 6 downTo 0) {
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = sdf.format(cal.time)
                
                val screenTime = when (i) {
                    0 -> 125 // Today
                    1 -> 240
                    2 -> 180
                    3 -> 290
                    4 -> 150
                    5 -> 320
                    else -> 210
                }
                val unlocks = when (i) {
                    0 -> 14
                    1 -> 35
                    2 -> 22
                    3 -> 41
                    4 -> 19
                    5 -> 48
                    else -> 30
                }
                val notifs = when (i) {
                    0 -> 42
                    1 -> 120
                    2 -> 85
                    3 -> 145
                    4 -> 70
                    5 -> 160
                    else -> 95
                }
                val score = (100 - (screenTime / 10) - (unlocks / 2)).coerceIn(10, 100)

                repository.insertDayStats(
                    DayStats(
                        date = dateStr,
                        screenTimeMinutes = screenTime,
                        unlocks = unlocks,
                        notifications = notifs,
                        focusScore = score
                    )
                )
            }
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}

class ScreenWiseViewModelFactory(private val repository: ScreenWiseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScreenWiseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScreenWiseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
