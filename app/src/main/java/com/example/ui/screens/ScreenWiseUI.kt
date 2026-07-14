package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.example.data.model.AppUsageItem
import kotlinx.coroutines.delay
import com.example.data.database.AppLimit
import com.example.data.database.DayStats
import com.example.data.database.FocusMode
import com.example.data.database.HabitLog
import com.example.viewmodel.ScreenWiseViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScreenWiseAppContent(viewModel: ScreenWiseViewModel) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val isOnboarded by viewModel.isOnboardingCompleted.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()

    // Trigger permission & state reload periodically
    LaunchedEffect(Unit) {
        viewModel.updatePermissionStatus(context)
        viewModel.loadAppUsage(context)
    }

    val grayscaleEnabled = profile?.grayscaleEnabled == true
    val dimEnabled = profile?.dimEnabled == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                if (grayscaleEnabled) {
                    val colorMatrix = ColorMatrix().apply { setToSaturation(0f) }
                    drawContent()
                    // Apply grayscale color filter over content
                    val paint = Paint().apply {
                        colorFilter = ColorFilter.colorMatrix(colorMatrix)
                    }
                    drawIntoCanvas { canvas ->
                        canvas.save()
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
                        canvas.restore()
                    }
                } else {
                    drawContent()
                }
                
                if (dimEnabled) {
                    // Overlay semi-transparent black for dim effect
                    drawRect(Color.Black.copy(alpha = 0.45f))
                }
            }
    ) {
        when {
            !isOnboarded -> {
                OnboardingScreen(
                    onComplete = { theme, pin ->
                        viewModel.completeOnboarding(theme, pin)
                    }
                )
            }
            isAppLocked -> {
                AppLockScreen(
                    onAuthenticate = { pin ->
                        viewModel.authenticatePin(pin)
                    }
                )
            }
            else -> {
                MainAppNavigationContainer(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigationContainer(viewModel: ScreenWiseViewModel) {
    var currentScreen by remember { mutableStateOf("dashboard") }
    val profile by viewModel.userProfile.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentScreen == "dashboard",
                    onClick = { currentScreen = "dashboard" },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == "focus",
                    onClick = { currentScreen = "focus" },
                    icon = { Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Focus") },
                    label = { Text("Focus", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_focus")
                )
                NavigationBarItem(
                    selected = currentScreen == "sleep",
                    onClick = { currentScreen = "sleep" },
                    icon = { Icon(Icons.Filled.Bedtime, contentDescription = "Sleep") },
                    label = { Text("Sleep", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_sleep")
                )
                NavigationBarItem(
                    selected = currentScreen == "limits",
                    onClick = { currentScreen = "limits" },
                    icon = { Icon(Icons.Filled.Timer, contentDescription = "Limits") },
                    label = { Text("Limits", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_limits")
                )
                NavigationBarItem(
                    selected = currentScreen == "more",
                    onClick = { currentScreen = "more" },
                    icon = { Icon(Icons.Filled.MoreHoriz, contentDescription = "More") },
                    label = { Text("More", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_more")
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ScreenWise",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Focus Coins and Level indicator
                    profile?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${it.coins}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Divider(
                                modifier = Modifier
                                    .height(14.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Lvl ${it.level}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "dashboard" -> DashboardScreen(viewModel)
                "focus" -> FocusModeScreen(viewModel)
                "sleep" -> SleepModeScreen(viewModel)
                "limits" -> AppTimerLimitsScreen(viewModel)
                "more" -> MoreFeaturesScreen(viewModel)
            }
        }
    }
}

// ======================= ONBOARDING & SETUP =======================
@Composable
fun OnboardingScreen(onComplete: (String, String) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var selectedTheme by remember { mutableStateOf("Sleek Interface") }
    var pinCode by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    var policyAccepted by remember { mutableStateOf(false) }

    val themes = listOf("Sleek Interface", "AMOLED Dark", "Emerald Focus", "Ocean Serene", "Sunset Focus")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (idx + 1 <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    1 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = "ScreenWise Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "ScreenWise এ স্বাগতম",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "আপনার ফোন ব্যবহারের অভ্যাস উন্নত করুন, ফোকাস বাড়ান এবং দারুণ সব রিওয়ার্ড অর্জন করুন!",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    2 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "একটি থিম নির্বাচন করুন",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            themes.forEach { theme ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable { selectedTheme = theme },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedTheme == theme) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        2.dp,
                                        if (selectedTheme == theme) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            theme,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (selectedTheme == theme) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (selectedTheme == theme) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "একটি ৪-সংখ্যার সিকিউরিটি পিন সেট করুন",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "পিন আপনার ব্রাউজিং ডেটা এবং অ্যাপ সেটিংস সুরক্ষিত রাখবে।",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = pinCode,
                                onValueChange = { if (it.length <= 4) pinCode = it },
                                label = { Text("নতুন ৪-সংখ্যার পিন") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_pin_input")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = { if (it.length <= 4) confirmPin = it },
                                label = { Text("পিনটি নিশ্চিত করুন") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_confirm_pin_input")
                            )
                            if (pinError.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(pinError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                            }
                        }
                    }
                    4 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "গোপনীয়তা নীতি ও শর্তাবলী",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                LazyColumn(modifier = Modifier.padding(16.dp)) {
                                    item {
                                        Text(
                                            "Privacy Policy & Usage Terms\n\n" +
                                            "১. ডেটা স্টোরেজ:\n" +
                                            "ScreenWise আপনার সমস্ত ব্যবহারের পরিসংখ্যান (Screen Time, App Limits, Habits) শুধুমাত্র আপনার স্থানীয় ডিভাইসে সুরক্ষিতভাবে সংরক্ষণ করে। কোন দূরবর্তী সার্ভারে তথ্য প্রেরণ করা হয় না।\n\n" +
                                            "২. Usage Access Permission:\n" +
                                            "আপনার ফোন ব্যবহারের আসল পরিসংখ্যান জানার জন্য এবং অ্যাপ লকিং পরিচালনা করতে 'UsageStatsManager' এবং 'Accessibility Service' অনুমতি প্রয়োজন।\n\n" +
                                            "৩. নিরাপত্তা পিন:\n" +
                                            "পিন কোডটি স্থানীয় ডাটাবেসে অত্যন্ত সুরক্ষিত উপায়ে রাখা হয়, যা অপব্যবহার রোধ করতে সাহায্য করে।",
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = policyAccepted,
                                    onCheckedChange = { policyAccepted = it },
                                    modifier = Modifier.testTag("onboarding_accept_policy")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "আমি গোপনীয়তা নীতি এবং শর্তাবলী পড়েছি এবং সম্মতি দিচ্ছি।",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("পূর্ববর্তী")
                    }
                }
                Button(
                    onClick = {
                        if (step == 3) {
                            if (pinCode.length != 4) {
                                pinError = "পিন অবশ্যই ৪ সংখ্যার হতে হবে।"
                            } else if (pinCode != confirmPin) {
                                pinError = "পিন দুটি মেলেনি।"
                            } else {
                                pinError = ""
                                step++
                            }
                        } else if (step == 4) {
                            if (policyAccepted) {
                                onComplete(selectedTheme, pinCode)
                            }
                        } else {
                            step++
                        }
                    },
                    enabled = step != 4 || policyAccepted,
                    modifier = Modifier.weight(1f).padding(start = if (step > 1) 8.dp else 0.dp).testTag("onboarding_next_btn")
                ) {
                    Text(if (step == 4) "শুরু করুন" else "পরবর্তী")
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(onAuthenticate: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "App Locked",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "ScreenWise Locked",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "চালিয়ে যেতে আপনার ৪-সংখ্যার পিনটি প্রবেশ করান",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = { Text("পিন প্রবেশ করুন") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("lock_pin_input")
            )
            if (errorMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (onAuthenticate(pin)) {
                        errorMsg = ""
                    } else {
                        errorMsg = "ভুল পিন কোড, আবার চেষ্টা করুন।"
                        pin = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("unlock_btn")
            ) {
                Text("আনলক করুন")
            }
        }
    }
}

// ======================= SCREEN 1: DASHBOARD =======================
@Composable
fun DashboardScreen(viewModel: ScreenWiseViewModel) {
    val context = LocalContext.current
    val isGranted by viewModel.isPermissionGranted.collectAsState()
    val allStats by viewModel.allDayStats.collectAsState()
    val usageList by viewModel.appUsageList.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val advice by viewModel.dailyAdvice.collectAsState()

    val todayStats = allStats.firstOrNull()
    val screenTimeMin = todayStats?.screenTimeMinutes ?: 0
    val unlocks = todayStats?.unlocks ?: 0
    val notifications = todayStats?.notifications ?: 0
    val focusScore = todayStats?.focusScore ?: 80

    var chartType by remember { mutableStateOf("Daily") } // Daily, Weekly, Monthly, Yearly

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Streak Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "হ্যালো, ট্র্যাকার!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        "আজকের ফোন ব্যবহার",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFFF6E40),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${profile?.streak ?: 1} দিনের Streak",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Circular Goal Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        // Custom Arc for Screen Time Progress
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Canvas(modifier = Modifier.size(150.dp)) {
                            // Track Arc
                            drawArc(
                                color = trackColor,
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Progress Arc (Target is, e.g., 360 minutes / 6 hours)
                            val targetMin = 360f
                            val sweepAngle = ((screenTimeMin / targetMin) * 270f).coerceAtMost(270f)
                            drawArc(
                                color = primaryColor,
                                startAngle = 135f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val hrs = screenTimeMin / 60
                            val mins = screenTimeMin % 60
                            Text(
                                if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "মোট সময়",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Stats Bar (Unlocks, Notifications, Focus Score)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.LockOpen,
                                contentDescription = "Unlocks",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$unlocks বার",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text("আনলক", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$notifications টি",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text("নোটিফিকেশন", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Psychology,
                                contentDescription = "Focus Score",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$focusScore%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("ফোকাস স্কোর", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Usage Stats Permission Request Dialog/Button if missing
        if (!isGranted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "রিয়েল-টাইম ট্র্যাকিং নিষ্ক্রিয়!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "আপনার আসল স্ক্রিন টাইম পেতে 'Usage Access' অনুমতি প্রয়োজন।",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("অনুমতি দিন")
                        }
                    }
                }
            }
        }

        // Daily Smart Advice Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.TipsAndUpdates,
                        contentDescription = "Tips",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ScreenWise স্মার্ট পরামর্শ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            advice.randomOrNull() ?: "আজ রাত ১১টার পর ফোন ব্যবহার না করার চেষ্টা করুন।",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Screen Time Charts (Daily / Weekly / Monthly / Yearly)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ব্যবহারের চার্ট",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Tab selectors
                        Row {
                            listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { tab ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (chartType == tab) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable { chartType = tab }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        tab,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (chartType == tab) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated/Real Historical Chart Canvas Drawing
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val points = when (chartType) {
                            "Daily" -> listOf(120f, 240f, 180f, 290f, 150f, 320f, screenTimeMin.toFloat())
                            "Weekly" -> listOf(1400f, 1800f, 1200f, 1600f)
                            "Monthly" -> listOf(5400f, 6200f, 4800f, 5200f, 5900f)
                            "Yearly" -> listOf(65000f, 72000f)
                            else -> listOf(150f, 220f, 180f)
                        }

                        val maxVal = (points.maxOrNull() ?: 100f).coerceAtLeast(100f)
                        val stepX = width / (points.size - 1).coerceAtLeast(1)

                        val path = Path()
                        val fillPath = Path()

                        points.forEachIndexed { idx, valItem ->
                            val x = idx * stepX
                            val y = height - (valItem / maxVal) * (height - 20f)
                            
                            if (idx == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }

                            // Draw raw metric circles
                            drawCircle(
                                color = primaryColor,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }

                        fillPath.lineTo(width, height)
                        fillPath.close()

                        // Draw path lines and filled gradients
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        when (chartType) {
                            "Daily" -> "গত ৭ দিনের রেকর্ড (মিনিট)"
                            "Weekly" -> "গত ৪ সপ্তাহের রেকর্ড (মিনিট)"
                            "Monthly" -> "গত ৫ মাসের রেকর্ড (মিনিট)"
                            else -> "গত ২ বছরের রেকর্ড (মিনিট)"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // App Usage List Title
        item {
            Text(
                "সর্বোচ্চ ব্যবহৃত অ্যাপসমূহ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // App Usage List Items
        if (usageList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "কোন ব্যবহারের ডেটা পাওয়া যায়নি!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(usageList.take(6)) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Android,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    app.appName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "${app.openCount} বার খোলা হয়েছে",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val hrs = app.screenTimeMinutes / 60
                            val mins = app.screenTimeMinutes % 60
                            Text(
                                if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================= SCREEN 2: FOCUS MODE =======================
@Composable
fun FocusModeScreen(viewModel: ScreenWiseViewModel) {
    val activeFocus by viewModel.activeFocusMode.collectAsState()
    val remainingSec by viewModel.focusTimeRemaining.collectAsState()

    var customName by remember { mutableStateOf("") }
    var customDuration by remember { mutableStateOf("30") }
    var selectedApps by remember { mutableStateOf("Social, Games") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (activeFocus != null) {
            // Active Countdown Screen
            Text(
                "ফোকাস মোড সক্রিয়!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                activeFocus!!.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Pulse Timer Visuals
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                val totalSec = activeFocus!!.durationMinutes * 60f
                val pct = if (totalSec > 0) remainingSec / totalSec else 1f
                
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.1f),
                        radius = size.width / 2
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = pct * 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val m = remainingSec / 60
                    val s = remainingSec % 60
                    Text(
                        String.format("%02d:%02d", m, s),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("বাকি সময়", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                "ফোকাসড থাকুন। ব্যাকগ্রাউন্ডে অন্যান্য সব ক্ষতিকর অ্যাপ ব্লক করা হয়েছে।",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.stopFocusMode() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().testTag("stop_focus_btn")
            ) {
                Text("ফোকাস বন্ধ করুন")
            }
        } else {
            // Configuration Setup Screen
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "নতুন ফোকাস সেশন",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "আপনার প্রয়োজনীয় সময় বেছে নিন এবং বিভ্রান্তি এড়ান।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                // Preset focus categories
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            Triple("Study Mode", "30", "Games, Entertainment"),
                            Triple("Work Mode", "45", "Social Media, Chat")
                        ).forEach { (modeTitle, dur, apps) ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.startFocusMode(modeTitle, dur.toInt(), apps)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(modeTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("$dur মিনিট", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "ব্লকড: $apps",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Configuration form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("কাস্টম ফোকাস মোড", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("ফোকাস মোডের নাম (যেমন: কোডিং)") },
                                modifier = Modifier.fillMaxWidth().testTag("focus_name_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = customDuration,
                                onValueChange = { customDuration = it },
                                label = { Text("সময়সীমা (মিনিট)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("focus_duration_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = selectedApps,
                                onValueChange = { selectedApps = it },
                                label = { Text("ব্লক করার ক্যাটাগরি বা অ্যাপ") },
                                modifier = Modifier.fillMaxWidth().testTag("focus_block_apps_input")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val minutes = customDuration.toIntOrNull() ?: 30
                                    val title = if (customName.isNotEmpty()) customName else "Custom Focus"
                                    viewModel.startFocusMode(title, minutes, selectedApps)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("start_focus_btn")
                            ) {
                                Text("ফোকাস শুরু করুন")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================= SCREEN 3: SLEEP / BEDTIME MODE =======================
@Composable
fun SleepModeScreen(viewModel: ScreenWiseViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val isPlayingMusic by viewModel.isPlayingSleepMusic.collectAsState()
    val activeTrack by viewModel.activeSleepTrack.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Sleep & Bedtime Mode",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "সুন্দর ও স্বাস্থ্যকর ঘুমের জন্য বিশেষ সেটিংস ও রিল্যাক্সিং সাউন্ড।",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Sleep Master Switch
        item {
            profile?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.isSleepModeActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "বেডটাইম মোড",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (it.isSleepModeActive) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (it.isSleepModeActive) "বেডটাইম মোড সক্রিয় রয়েছে" else "বেডটাইম মোড নিষ্ক্রিয়",
                                fontSize = 12.sp,
                                color = if (it.isSleepModeActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = it.isSleepModeActive,
                            onCheckedChange = { active -> viewModel.toggleSleepMode(active) },
                            modifier = Modifier.testTag("sleep_mode_switch")
                        )
                    }
                }
            }
        }

        // Sleep custom parameters: Dim, Grayscale
        item {
            profile?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "স্ক্রিন অপ্টিমাইজেশন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FilterBAndW, contentDescription = "Grayscale")
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Grayscale (ধূসর স্ক্রিন)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("ঘুমের আগে চোখের চাপ কমায়", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            Switch(
                                checked = it.grayscaleEnabled,
                                onCheckedChange = { valItem -> viewModel.toggleGrayscale(valItem) },
                                modifier = Modifier.testTag("grayscale_switch")
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.BrightnessMedium, contentDescription = "Screen Dim")
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Screen Dim (স্ক্রিন ডিম)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("উজ্জ্বলতা স্বাভাবিকের চেয়ে কমায়", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            Switch(
                                checked = it.dimEnabled,
                                onCheckedChange = { valItem -> viewModel.toggleScreenDim(valItem) },
                                modifier = Modifier.testTag("screen_dim_switch")
                            )
                        }
                    }
                }
            }
        }

        // Sleep Bedtime Schedule Card
        item {
            profile?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("বেডটাইম সময়সূচী", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                String.format(
                                    "শুরু: %02d:%02d  |  শেষ: %02d:%02d",
                                    it.bedtimeStartHour, it.bedtimeStartMin,
                                    it.bedtimeEndHour, it.bedtimeEndMin
                                ),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.testTag("edit_sleep_time_btn")
                        ) {
                            Text("সম্পাদনা")
                        }
                    }
                }
            }
        }

        // Sleep Relaxing Music Controller
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ঘুমের প্রশান্তিদায়ক সুর (Sleep Music)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val tracks = listOf("Rain Forest", "Ocean Waves", "Zen Meditation", "Campfire Crackle")

                    tracks.forEach { track ->
                        val isCurrent = isPlayingMusic && activeTrack == track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (isCurrent) viewModel.stopSleepMusic()
                                    else viewModel.playSleepMusic(track)
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isCurrent) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                                    contentDescription = "Play",
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    track,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isCurrent) {
                                // Simple visual wave
                                Text("বাজছে...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Time Picker Dialog simulation
    if (showTimePicker) {
        var startH by remember { mutableStateOf("22") }
        var startM by remember { mutableStateOf("00") }
        var endH by remember { mutableStateOf("07") }
        var endM by remember { mutableStateOf("00") }

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("বেডটাইম সময় নির্বাচন করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("ঘুম শুরু (Start):", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = startH,
                            onValueChange = { startH = it },
                            label = { Text("Hour") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = startM,
                            onValueChange = { startM = it },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("ঘুম শেষ (End):", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = endH,
                            onValueChange = { endH = it },
                            label = { Text("Hour") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = endM,
                            onValueChange = { endM = it },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("বাতিল") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val sh = startH.toIntOrNull() ?: 22
                                val sm = startM.toIntOrNull() ?: 0
                                val eh = endH.toIntOrNull() ?: 7
                                val em = endM.toIntOrNull() ?: 0
                                viewModel.updateBedtimeSettings(sh, sm, eh, em)
                                showTimePicker = false
                            },
                            modifier = Modifier.testTag("save_sleep_time_btn")
                        ) {
                            Text("সংরক্ষণ")
                        }
                    }
                }
            }
        }
    }
}

// ======================= SCREEN 4: APP TIMER LIMITS =======================
@Composable
fun AppTimerLimitsScreen(viewModel: ScreenWiseViewModel) {
    val context = LocalContext.current
    val limits by viewModel.allAppLimits.collectAsState()
    val usageList by viewModel.appUsageList.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPackage by remember { mutableStateOf("") }
    var selectedAppName by remember { mutableStateOf("") }
    var customMinutes by remember { mutableStateOf("30") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "App Timer & Limits",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "নির্দিষ্ট অ্যাপ ব্যবহারের জন্য সময়সীমা নির্ধারণ করুন।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp).testTag("add_limit_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Limit")
                }
            }
        }

        if (limits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.TimerOff,
                            contentDescription = "No Limit",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "কোন অ্যাপ লিমিট সেট করা নেই!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "উপরের যোগ চিহ্নে ক্লিক করে অ্যাপ লিমিট যোগ করুন।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(limits) { limit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                limit.appName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "প্যাকেজ: ${limit.packageName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "নির্ধারিত লিমিট: ${limit.limitMinutes} মিনিট",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.removeAppLimit(limit) },
                            modifier = Modifier.testTag("delete_limit_${limit.packageName}")
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("অ্যাপ লিমিট যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated selection dropdown of typical apps
                    Text(
                        "অ্যাপ নির্বাচন করুন:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val selectionList = if (usageList.isNotEmpty()) usageList.take(5) else listOf(
                        AppUsageItem("com.facebook.katana", "Facebook", 0, 0),
                        AppUsageItem("com.instagram.android", "Instagram", 0, 0),
                        AppUsageItem("com.google.android.youtube", "YouTube", 0, 0)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    ) {
                        selectionList.forEach { item ->
                            val isSel = selectedPackage == item.packageName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPackage = item.packageName
                                        selectedAppName = item.appName
                                    }
                                    .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.appName, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                if (isSel) {
                                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        label = { Text("দৈনিক ব্যবহারের সর্বোচ্চ সীমা (মিনিট)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("limit_minutes_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) { Text("বাতিল") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (selectedPackage.isNotEmpty() && customMinutes.isNotEmpty()) {
                                    viewModel.setAppLimit(selectedPackage, selectedAppName, customMinutes.toIntOrNull() ?: 30)
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.testTag("save_limit_btn")
                        ) {
                            Text("নির্ধারণ")
                        }
                    }
                }
            }
        }
    }
}

// ======================= SCREEN 5: MORE FEATURES (DETOX, HABITS, SECURITY, REPORTS) =======================
@Composable
fun MoreFeaturesScreen(viewModel: ScreenWiseViewModel) {
    val context = LocalContext.current
    var subScreen by remember { mutableStateOf("home") }

    when (subScreen) {
        "home" -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "প্রোডাক্টিভিটি ও ওয়েলবিং হাব",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "সুস্থ অভ্যাস গঠন এবং উন্নত সুরক্ষার জন্য নিচে অন্বেষণ করুন।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subScreen = "habits" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Spa, contentDescription = "Habits", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("🧘 Healthy Habits", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("পানি পান, হাঁটাচলা এবং চোখের ব্যায়ামের ট্র্যাকার", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subScreen = "detox" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Detox", tint = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("📵 Phone Detox", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("অ্যাপস সম্পূর্ণ লক করা এবং ইমার্জেন্সি কন্টাক্ট মোড", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subScreen = "security" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Security, contentDescription = "PIN Settings", tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("🔒 Security & PIN Lock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("আপনার ৪-সংখ্যার সিকিউরিটি পিন পরিবর্তন করুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subScreen = "reports" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.BarChart, contentDescription = "Reports", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("📤 Reports & Export", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("CSV ফাইল আকারে আপনার ব্যবহারের ডেটা এক্সপোর্ট করুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subScreen = "themes" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Palette, contentDescription = "Themes", tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("🎨 App Themes & Styling", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("আপনার কালার স্কিম এবং অ্যাপের লুক অ্যান্ড ফিল পরিবর্তন করুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subScreen = "about" }
                            .testTag("about_developer_option"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = "About Developer", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("ℹ️ About Developer & Company", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("ডেভেলপার ও NexVora Lab's Ofc সম্পর্কে জানুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
        "habits" -> HealthyHabitsSubScreen(viewModel, onBack = { subScreen = "home" })
        "detox" -> PhoneDetoxSubScreen(viewModel, onBack = { subScreen = "home" })
        "security" -> SecurityPinSubScreen(viewModel, onBack = { subScreen = "home" })
        "reports" -> ReportsExportSubScreen(viewModel, onBack = { subScreen = "home" })
        "themes" -> ThemesSubScreen(viewModel, onBack = { subScreen = "home" })
        "about" -> AboutDeveloperSubScreen(onBack = { subScreen = "home" })
    }
}

// ======================= HEALTHY HABITS HUB =======================
@Composable
fun HealthyHabitsSubScreen(viewModel: ScreenWiseViewModel, onBack: () -> Unit) {
    val habits by viewModel.todayHabitLogs.collectAsState()

    val waterLog = habits.find { it.habitType == "WATER" } ?: HabitLog(habitType = "WATER", date = "", target = 8, progress = 0)
    val walkLog = habits.find { it.habitType == "WALK" } ?: HabitLog(habitType = "WALK", date = "", target = 6000, progress = 0)
    val eyeLog = habits.find { it.habitType == "EYE" } ?: HabitLog(habitType = "EYE", date = "", target = 4, progress = 0)
    val meditationLog = habits.find { it.habitType == "MEDITATION" } ?: HabitLog(habitType = "MEDITATION", date = "", target = 15, progress = 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("🧘 Healthy Habits Hub", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Water Intakes Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Water Reminder (পানি পান)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("টার্গেট: ${waterLog.target} গ্লাস  |  আজ: ${waterLog.progress} গ্লাস", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Filled.LocalActivity, contentDescription = null, tint = Color(0xFF2979FF))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.logHabitProgress("WATER", 1, 8) },
                            modifier = Modifier.weight(1f).testTag("add_water_btn")
                        ) {
                            Text("+ ১ গ্লাস")
                        }
                        OutlinedButton(
                            onClick = { viewModel.logHabitProgress("WATER", -1, 8) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("- ১ গ্লাস")
                        }
                    }
                }
            }
        }

        // Walk steps progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Walk Tracker (হাঁটাচলা)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("টার্গেট: ${walkLog.target} কদম  |  আজ: ${walkLog.progress} কদম", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Filled.DirectionsWalk, contentDescription = null, tint = Color(0xFF00E676))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.logHabitProgress("WALK", 1000, 6000) },
                            modifier = Modifier.weight(1f).testTag("add_walk_btn")
                        ) {
                            Text("+ ১০০০ কদম")
                        }
                        OutlinedButton(
                            onClick = { viewModel.logHabitProgress("WALK", -1000, 6000) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("- ১০০০")
                        }
                    }
                }
            }
        }

        // Eye Stretch exercise Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Eye Exercise (চোখের ব্যায়াম)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("টার্গেট: ৪ বার  |  আজ: ${eyeLog.progress} বার", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color(0xFFFFD600))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.logHabitProgress("EYE", 1, 4) },
                        modifier = Modifier.fillMaxWidth().testTag("add_eye_btn")
                    ) {
                        Text("একটি ২০-২০-২০ ব্যায়াম শেষ করেছি")
                    }
                }
            }
        }

        // Meditation audio simulator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Meditation Timer (ধ্যান)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("টার্গেট: ১৫ মিনিট  |  আজ: ${meditationLog.progress} মিনিট", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Filled.Spa, contentDescription = null, tint = Color(0xFFE040FB))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.logHabitProgress("MEDITATION", 5, 15) },
                        modifier = Modifier.fillMaxWidth().testTag("add_meditation_btn")
                    ) {
                        Text("৫ মিনিট ধ্যান করেছি")
                    }
                }
            }
        }
    }
}

// ======================= PHONE DETOX MODULE =======================
@Composable
fun PhoneDetoxSubScreen(viewModel: ScreenWiseViewModel, onBack: () -> Unit) {
    var isDetoxActive by remember { mutableStateOf(false) }
    var detoxTimeRemaining by remember { mutableStateOf(0) } // seconds

    LaunchedEffect(isDetoxActive) {
        if (isDetoxActive) {
            detoxTimeRemaining = 15 * 60 // 15 minutes detox lock
            while (detoxTimeRemaining > 0) {
                delay(1000)
                detoxTimeRemaining--
            }
            isDetoxActive = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isDetoxActive) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("📵 Phone Detox Lock", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isDetoxActive) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = "Detox Active",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Deep Phone Detox সক্রিয়!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "জরুরী কল ব্যতিত এই স্ক্রিন থেকে প্রস্থান করতে পারবেন না।",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                val m = detoxTimeRemaining / 60
                val s = detoxTimeRemaining % 60
                Text(
                    String.format("%02d:%02d", m, s),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { /* Emergency dialer */ },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Emergency Contact Dial")
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Detox Ready",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "ডিজিটাল ডিটক্স শুরু করুন",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "১৫ মিনিটের জন্য ফোন সম্পূর্ণ লক হবে যাতে আপনি আপনার বাস্তব কাজে শতভাগ ফোকাস করতে পারেন।",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(36.dp))
                Button(
                    onClick = { isDetoxActive = true },
                    modifier = Modifier.fillMaxWidth().testTag("start_detox_btn")
                ) {
                    Text("১৫ মিনিট লক শুরু করুন")
                }
            }
        }
    }
}

// ======================= SECURITY PIN CONFIGURATION =======================
@Composable
fun SecurityPinSubScreen(viewModel: ScreenWiseViewModel, onBack: () -> Unit) {
    val profile by viewModel.userProfile.collectAsState()
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var resultMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("🔒 Security PIN Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("পিন কোড পরিবর্তন করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentPinInput,
                    onValueChange = { if (it.length <= 4) currentPinInput = it },
                    label = { Text("বর্তমান পিন") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("security_current_pin")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 4) newPinInput = it },
                    label = { Text("নতুন ৪-সংখ্যার পিন") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("security_new_pin")
                )

                if (resultMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(resultMsg, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val realPin = profile?.pinCode ?: ""
                        if (currentPinInput == realPin) {
                            if (newPinInput.length == 4) {
                                viewModel.completeOnboarding(profile?.themeMode ?: "Sleek Interface", newPinInput)
                                resultMsg = "সফলভাবে পিন পরিবর্তন করা হয়েছে!"
                                currentPinInput = ""
                                newPinInput = ""
                            } else {
                                resultMsg = "নতুন পিন ৪ সংখ্যার হতে হবে।"
                            }
                        } else {
                            resultMsg = "বর্তমান পিনটি সঠিক নয়।"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("change_pin_btn")
                ) {
                    Text("পিন আপডেট করুন")
                }
            }
        }
    }
}

// ======================= REPORTS & EXPORT SECTION =======================
@Composable
fun ReportsExportSubScreen(viewModel: ScreenWiseViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val allStats by viewModel.allDayStats.collectAsState()
    var exportStatus by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("📤 Reports & Export Hub", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "CSV এক্সপোর্ট রিপোর্ট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "আপনার গত সপ্তাহের সমস্ত স্ক্রিন টাইম ব্যবহারের রেকর্ড স্প্রেডশিট বা এক্সেল ফাইলে দেখার জন্য এক্সপোর্ট করুন।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                if (exportStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        exportStatus,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        try {
                            val csvFile = File(context.cacheDir, "screenwise_usage_report.csv")
                            csvFile.bufferedWriter().use { writer ->
                                writer.write("Date,ScreenTime(Minutes),Unlocks,Notifications,FocusScore\n")
                                allStats.forEach { stat ->
                                    writer.write("${stat.date},${stat.screenTimeMinutes},${stat.unlocks},${stat.notifications},${stat.focusScore}\n")
                                }
                            }
                            
                            val uri = Uri.fromFile(csvFile)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "ScreenWise Usage Report")
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "রিপোর্ট শেয়ার করুন"))
                            exportStatus = "রিপোর্ট সফলভাবে প্রস্তুত করা হয়েছে!"
                        } catch (e: Exception) {
                            exportStatus = "ভুল হয়েছে: ${e.localizedMessage}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_csv_btn")
                ) {
                    Text("CSV এক্সপোর্ট করুন ও শেয়ার")
                }
            }
        }
    }
}

// ======================= APP THEMES & STYLING SUB-SCREEN =======================
@Composable
fun ThemesSubScreen(viewModel: ScreenWiseViewModel, onBack: () -> Unit) {
    val profile by viewModel.userProfile.collectAsState()
    val currentTheme = profile?.themeMode ?: "Sleek Interface"
    
    val themes = listOf(
        "Sleek Interface" to listOf(Color(0xFF6750A4), Color(0xFFFDF8FD), Color(0xFFE8DEF8)),
        "AMOLED Dark" to listOf(Color(0xFF00E5FF), Color(0xFF000000), Color(0xFF0E0E0E)),
        "Emerald Focus" to listOf(Color(0xFF00C853), Color(0xFF071415), Color(0xFF0C2426)),
        "Ocean Serene" to listOf(Color(0xFF2979FF), Color(0xFF070B19), Color(0xFF101731)),
        "Sunset Focus" to listOf(Color(0xFFFF6E40), Color(0xFF140D0E), Color(0xFF201517))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("🎨 App Themes & Styling", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "আপনার ফোকাস ও স্ক্রিন-টাইম অভিজ্ঞতা সুন্দর করতে একটি থিম বেছে নিন।",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themes) { (themeName, colors) ->
                val isSelected = themeName == currentTheme
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateTheme(themeName) }
                        .testTag("theme_card_$themeName"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        2.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                themeName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            }
                        }
                        
                        if (isSelected) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================= ABOUT DEVELOPER & COMPANY SUB-SCREEN =======================
@Composable
fun AboutDeveloperSubScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "About Developer & Company",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. About Developer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar Placeholder with beautiful gradient
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "PR",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Prince AR Abdur Rahman",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Independent App Developer",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Independent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Contact & Social Links",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // WhatsApp 1 Button
                        ContactButton(
                            icon = "💬",
                            title = "WhatsApp (Primary)",
                            subtitle = "01707424006",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801707424006"))
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // WhatsApp 2 Button
                        ContactButton(
                            icon = "💬",
                            title = "WhatsApp (Secondary)",
                            subtitle = "01796951709",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801796951709"))
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Facebook Button
                        ContactButton(
                            icon = "👥",
                            title = "Facebook Profile",
                            subtitle = "Connect on Facebook",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/share/1BNn32qoJo/"))
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Instagram Button
                        ContactButton(
                            icon = "📸",
                            title = "Instagram Profile",
                            subtitle = "Follow on Instagram",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/ur___abdur____rahman__2008"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            // 2. About Company Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "NV",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "NexVora Lab's Ofc",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Company Office",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "NexVora Lab's Ofc focuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mission Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Spa,
                                        contentDescription = "Mission",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Mission",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Technical Info & Credits Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            "Technical Info & Credits",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Version", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1.0.0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Developed by", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Prince AR Abdur Rahman", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Published by", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("NexVora Lab's Ofc", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // 4. Footer Copyright Text
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
fun ContactButton(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Filled.Launch,
                contentDescription = "Open Link",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
