package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.WorkoutViewModel
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// --- Custom Reusable Glassmorphic Components ---

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderGlow: Boolean = false,
    borderColor: Color = SurfaceCardBorder,
    backgroundBrush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (backgroundBrush != null) Color.Transparent else SurfaceCard
        ),
        border = BorderStroke(
            1.5.dp,
            if (borderGlow) NeonBlue else borderColor
        )
    ) {
        val colModifier = if (backgroundBrush != null) {
            Modifier.background(backgroundBrush).padding(18.dp)
        } else {
            Modifier.padding(18.dp)
        }
        Column(
            modifier = colModifier
        ) {
            content()
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isSecondary: Boolean = false,
    height: androidx.compose.ui.unit.Dp = 50.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .testTag("neon_btn_$text"),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(14.dp)
    ) {
        val bgBrush = if (isSecondary) {
            Brush.horizontalGradient(listOf(SurfaceCardBorder, SurfaceCard))
        } else {
            Brush.horizontalGradient(listOf(BlueGradientStart, BlueGradientEnd))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSecondary) TextWhite else Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = if (isSecondary) TextWhite else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

@Composable
fun MainLayout(
    viewModel: WorkoutViewModel,
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    var showGyaniChat by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (viewModel.currentScreen != "active_workout") {
                    Column {
                        // Floating active workout banner if minimized (Strong/Hevy Style!)
                        viewModel.activeSession?.let { session ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeonBlueDim)
                                    .clickable { viewModel.currentScreen = "active_workout" }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = "Active Workout",
                                        tint = NeonBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Active Session: ${session.dayName}",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                            )
                                        Text(
                                            text = "Duration: ${formatDuration(viewModel.workoutDurationSeconds)}",
                                            color = NeonBlue,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Expand",
                                    tint = NeonBlue
                                )
                            }
                        }

                        // Floating Rest Timer Alert
                        if (viewModel.isRestTimerActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B))
                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Rest Timer",
                                        tint = ActiveGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Rest Timer: ${viewModel.restTimerRemainingSeconds}s remaining",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.stopRestTimer() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Stop Timer",
                                        tint = FailureMagenta,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Standard Navigation Bar
                        NavigationBar(
                            containerColor = SurfaceCard,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = viewModel.currentScreen == "dashboard" || viewModel.currentScreen == "ai_coach",
                                onClick = { viewModel.currentScreen = "dashboard" },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                label = { Text("Home") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonBlue,
                                    indicatorColor = NeonBlue,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )
                            NavigationBarItem(
                                selected = viewModel.currentScreen == "splits",
                                onClick = { viewModel.currentScreen = "splits" },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Splits") },
                                label = { Text("Splits") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonBlue,
                                    indicatorColor = NeonBlue,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )
                            NavigationBarItem(
                                selected = viewModel.currentScreen == "exercise_library",
                                onClick = { viewModel.currentScreen = "exercise_library" },
                                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Library") },
                                label = { Text("Exercises") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonBlue,
                                    indicatorColor = NeonBlue,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )
                            NavigationBarItem(
                                selected = viewModel.currentScreen == "progress",
                                onClick = { viewModel.currentScreen = "progress" },
                                icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Analytics") },
                                label = { Text("Progress") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonBlue,
                                    indicatorColor = NeonBlue,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )
                            NavigationBarItem(
                                selected = viewModel.currentScreen == "nutrition",
                                onClick = { viewModel.currentScreen = "nutrition" },
                                icon = { Icon(Icons.Default.Restaurant, contentDescription = "Nutrition") },
                                label = { Text("Nutrition") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonBlue,
                                    indicatorColor = NeonBlue,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )
                        }
                    }
                }
            },
            containerColor = DarkBackground,
            content = content
        )

        // Floating Gyani Button overlay (glowing aura-ai design)
        if (viewModel.currentScreen != "onboarding") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (viewModel.currentScreen == "active_workout") 16.dp else 90.dp)
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonBlue, BlueGradientEnd)
                        ),
                        shape = CircleShape
                    )
                    .border(2.dp, TextWhite, CircleShape)
                    .clickable { showGyaniChat = true },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gyani Chat",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "GYANI",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        if (showGyaniChat) {
            GyaniChatDialog(
                viewModel = viewModel,
                onDismiss = { showGyaniChat = false }
            )
        }
    }
}

@Composable
fun GyaniChatDialog(
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            color = DarkBackground,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp), // Safe area for full screen
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeonBlueDim, CircleShape)
                                .border(1.dp, NeonBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("GY", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GYANI AI ASSISTANT",
                                color = TextWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Your AI Workout & Nutrition Coach",
                                color = NeonBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray
                        )
                    }
                }

                Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Chat Messages List
                val lazyListState = rememberLazyListState()

                // Automatically scroll to bottom when history changes
                LaunchedEffect(viewModel.gyaniChatHistory.size) {
                    if (viewModel.gyaniChatHistory.isNotEmpty()) {
                        lazyListState.animateScrollToItem(viewModel.gyaniChatHistory.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.gyaniChatHistory.size) { index ->
                            val msg = viewModel.gyaniChatHistory[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (msg.isUser) NeonBlueDim else SurfaceCardBorder
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (msg.isUser) 12.dp else 0.dp,
                                        bottomEnd = if (msg.isUser) 0.dp else 12.dp
                                    ),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (msg.isUser) "You" else "Gyani",
                                            color = if (msg.isUser) NeonBlue else TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                            text = msg.text,
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (viewModel.isGyaniAiLoading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCardBorder),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                color = NeonBlue,
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Gyani is typing & updating...",
                                                color = TextGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Suggestion chips row
                val suggestions = listOf(
                    "Add face pull to Monday Pull Day",
                    "Change split to Bro Split",
                    "How many calories should I eat?"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(suggestions.size) { index ->
                        val suggestion = suggestions[index]
                        Box(
                            modifier = Modifier
                                .background(SurfaceCardBorder, RoundedCornerShape(20.dp))
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.sendGyaniChatMessage(suggestion)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = suggestion, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Input field
                var textInput by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask Gyani anything...", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = SurfaceCardBorder
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                viewModel.sendGyaniChatMessage(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .background(NeonBlue, CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}

// --- Screen 1: Onboarding / Landing ---

@Composable
fun OnboardingScreen(viewModel: WorkoutViewModel, onGetStarted: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    
    var nameInput by remember { mutableStateOf(viewModel.userName) }
    var ageInput by remember { mutableStateOf(viewModel.userAge) }
    var genderInput by remember { mutableStateOf(viewModel.userGender) }
    var weightInput by remember { mutableStateOf(viewModel.userWeight) }
    var heightInput by remember { mutableStateOf(viewModel.userHeight) }
    var goalInput by remember { mutableStateOf(viewModel.userGoal) }
    var activityInput by remember { mutableStateOf(viewModel.userActivityLevel) }
    var mealFreqInput by remember { mutableStateOf(viewModel.userMealFrequency) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (step == 1) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(NeonBlueDim, CircleShape)
                    .border(2.dp, NeonBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "AuraFit logo",
                    tint = NeonBlue,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "AURAFIT",
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = 4.sp
            )

            Text(
                text = "ELEVATE YOUR STRENGTH",
                color = NeonBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "A sleek, premium, and offline-first personal tracking environment optimized with smart performance logging.",
                color = TextGray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            var showOnboardingImport by remember { mutableStateOf(false) }
            var onboardingImportJson by remember { mutableStateOf("") }
            val onboardingContext = LocalContext.current

            if (showOnboardingImport) {
                OutlinedTextField(
                    value = onboardingImportJson,
                    onValueChange = { onboardingImportJson = it },
                    label = { Text("Paste JSON Backup Here", color = TextGray) },
                    modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = SurfaceCardBorder
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { showOnboardingImport = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextGray)
                    }
                    Button(
                        onClick = {
                            val msg = viewModel.importBackupJson(onboardingImportJson)
                            Toast.makeText(onboardingContext, msg, Toast.LENGTH_LONG).show()
                            if (msg.startsWith("Backup imported successfully")) {
                                onGetStarted() // Go directly to main app!
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import & Restore", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                NeonButton(
                    text = "GET STARTED",
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = { showOnboardingImport = true }
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "IMPORT EXISTING DATA BACKUP", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        } else if (step == 2) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "TELL US ABOUT YOURSELF",
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "Gyani AI will calculate the ideal strategy for you",
                color = NeonBlue,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Your Name", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = SurfaceCardBorder
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                OutlinedTextField(
                    value = ageInput,
                    onValueChange = { ageInput = it },
                    label = { Text("Age", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )

                OutlinedTextField(
                    value = genderInput,
                    onValueChange = { genderInput = it },
                    label = { Text("Gender (M/F/O)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )

                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("Height (cm)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                )
            }

            OutlinedTextField(
                value = mealFreqInput,
                onValueChange = { mealFreqInput = it },
                label = { Text("Meal Frequency (How many times you eat per day)", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = SurfaceCardBorder
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text("Your Fitness Goal:", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Lose Weight", "Gain Muscle", "Maintain").forEach { goal ->
                    val isSelected = goalInput.equals(goal, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .background(if (isSelected) NeonBlueDim else SurfaceCard, RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) NeonBlue else SurfaceCardBorder, RoundedCornerShape(8.dp))
                            .clickable { goalInput = goal }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = goal, color = if (isSelected) NeonBlue else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Activity Level:", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Sedentary", "Active", "Athlete").forEach { level ->
                    val isSelected = activityInput.equals(level, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .background(if (isSelected) NeonBlueDim else SurfaceCard, RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) NeonBlue else SurfaceCardBorder, RoundedCornerShape(8.dp))
                            .clickable { activityInput = level }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = level, color = if (isSelected) NeonBlue else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            NeonButton(
                text = "CALCULATE MY PLAN",
                onClick = {
                    if (nameInput.trim().isNotEmpty() && weightInput.trim().isNotEmpty() && heightInput.trim().isNotEmpty()) {
                        viewModel.saveUserProfile(
                            name = nameInput,
                            age = ageInput,
                            gender = genderInput,
                            weight = weightInput,
                            height = heightInput,
                            goal = goalInput,
                            activityLevel = activityInput,
                            mealFrequency = mealFreqInput
                        )
                        step = 3
                        viewModel.analyzeUserBodyMetrics()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
        } else if (step == 3) {
            Spacer(modifier = Modifier.height(40.dp))
            if (viewModel.isBodyAnalysisLoading) {
                CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(60.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Gyani is calculating your customized metabolic targets...",
                    color = TextWhite,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "YOUR METABOLIC REPORT",
                    color = NeonBlue,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DAILY CALORIE TARGET", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${viewModel.targetCalories} kcal", color = NeonBlue, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        
                        Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PROTEIN", color = TextGray, fontSize = 10.sp)
                                Text("${viewModel.targetProtein}g", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CARBS", color = TextGray, fontSize = 10.sp)
                                Text("${viewModel.targetCarbs}g", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FAT", color = TextGray, fontSize = 10.sp)
                                Text("${viewModel.targetFat}g", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GYANI'S COACHING STRATEGY",
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = viewModel.bodyAnalysisResult ?: "Calculated default strategy based on your targets.",
                            color = TextGray,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
                
                NeonButton(
                    text = "SAVE & ENTER",
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- Screen 2: Dashboard ---

@Composable
fun DashboardScreen(
    viewModel: WorkoutViewModel,
    splits: List<WorkoutSplit>,
    activeSplit: WorkoutSplit?,
    sessions: List<WorkoutSession>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        val displayName = viewModel.userName.ifBlank { "Alex Rivera" }
        val initials = displayName.split(" ")
            .filter { it.isNotEmpty() }
            .map { it.first().uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "AR" }

        // Premium Elegant Dark Header (From Design HTML)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WELCOME BACK",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = displayName,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.currentScreen = "settings" },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Glowing Profile Avatar / AR badge with light-blue glowing border (From Design HTML)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.dp, NeonBlue, CircleShape)
                        .padding(2.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .clickable { viewModel.currentScreen = "ai_coach" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Session Glassmorphism Card (From Design HTML)
        val sessionCardBrush = Brush.verticalGradient(
            colors = listOf(NeonBlue.copy(alpha = 0.15f), Color.Transparent)
        )
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonBlue.copy(alpha = 0.2f),
            backgroundBrush = sessionCardBrush
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "CURRENT SPLIT",
                        color = NeonBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeSplit?.name ?: "No Split Activated",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(NeonBlue, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DAY 1: PUSH",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Next: Barbell Bench Press",
                        color = TextGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "3 Sets • 8-12 Reps",
                        color = TextGrayMuted,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { viewModel.startActiveSplitWorkout() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = "START WORKOUT",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats Bento Grid (From Design HTML)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Streak
            NeonCard(
                modifier = Modifier.weight(1f),
                borderColor = SurfaceCardBorder
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeonBlue, CircleShape)
                        )
                        Text(
                            text = "STREAK",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${viewModel.getStreak()}",
                            color = TextWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "days",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // 1RM Est. / Total Sessions
            NeonCard(
                modifier = Modifier.weight(1f),
                borderColor = SurfaceCardBorder
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeonBlue, CircleShape)
                        )
                        Text(
                            text = "1RM EST.",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (sessions.isNotEmpty()) "105" else "0",
                            color = NeonBlue,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kg",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Performance Chart Section - Weekly Volume (From Design HTML)
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Volume",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (sessions.isNotEmpty()) "+14% VS LAST WEEK" else "ACTIVE WEEK",
                    color = NeonBlue,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val daysVolume = remember(sessions) {
                val vols = FloatArray(7) { 0f }
                val cal = Calendar.getInstance()
                for (s in sessions) {
                    cal.timeInMillis = s.startTime
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val idx = when (dayOfWeek) {
                        Calendar.MONDAY -> 0
                        Calendar.TUESDAY -> 1
                        Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3
                        Calendar.FRIDAY -> 4
                        Calendar.SATURDAY -> 5
                        Calendar.SUNDAY -> 6
                        else -> 0
                    }
                    vols[idx] += s.totalVolume
                }
                vols
            }

            val maxVol = daysVolume.maxOrNull()?.coerceAtLeast(100f) ?: 100f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val daysLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val currentDayIdx = when (currentDayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }

                daysLabels.forEachIndexed { idx, label ->
                    val volume = daysVolume[idx]
                    val percentage = if (maxVol > 0f && volume > 0f) {
                        (volume / maxVol).coerceIn(0.12f, 1f)
                    } else {
                        0.02f
                    }

                    val isToday = idx == currentDayIdx

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(percentage)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isToday) {
                                            Brush.verticalGradient(listOf(BlueGradientStart, BlueGradientEnd))
                                        } else {
                                            Brush.verticalGradient(listOf(SurfaceCardBorder, SurfaceCardBorder.copy(alpha = 0.5f)))
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isToday) NeonBlue else Color.Transparent,
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = label.uppercase(),
                            color = if (isToday) NeonBlue else TextGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Muscle Recovery Warning Banner (From Design HTML)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, OrangeWarm.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(OrangeWarm.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Recovery Warning",
                        tint = OrangeWarm,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "RECOVERY WARNING",
                        color = OrangeWarm,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Chest & Shoulders need 12h more rest for peak performance.",
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Advice Prompt Banner
        NeonCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.currentScreen = "ai_coach" },
            borderColor = PurpleAura
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI recommendations",
                    tint = PurpleAura,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AuraFit Smart AI Coach",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Ask Gemini for hyper-personalized volume recommendations, deload targets, and recovery strategy.",
                        color = TextGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Coach",
                    tint = TextGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Muscle Recovery Heat Map (Calculated on training focus)
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "MUSCLE RECOVERY MAP",
                color = TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val muscles = listOf("Chest", "Back", "Shoulders", "Legs", "Arms", "Core")
                muscles.forEach { muscle ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceCardBorder, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = muscle, color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ActiveGreen, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Training Calendar / History Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT COMPLETED SESSIONS",
                color = TextGray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "HISTORY",
                color = NeonBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { viewModel.currentScreen = "progress" }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = "Empty",
                        tint = TextGray,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "No workouts completed yet.", color = TextGray, fontSize = 13.sp)
                }
            }
        } else {
            sessions.take(3).forEach { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = session.dayName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = session.splitName, color = TextGray, fontSize = 12.sp)
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(session.startTime)),
                                color = TextGrayMuted,
                                fontSize = 11.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "${session.totalVolume.toInt()} kg", color = NeonBlue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(text = "${session.totalReps} Reps", color = TextGray, fontSize = 12.sp)
                            Text(text = formatDuration(session.duration), color = TextGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- Screen 3: Splits & Customized Plans ---

@Composable
fun SplitsScreen(
    viewModel: WorkoutViewModel,
    splits: List<WorkoutSplit>
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newSplitName by remember { mutableStateOf("") }

    var showDeleteSplitConfirm by remember { mutableStateOf(false) }
    var splitToDelete by remember { mutableStateOf<WorkoutSplit?>(null) }

    var showAddDayDialog by remember { mutableStateOf(false) }
    var selectedSplitForNewDay by remember { mutableStateOf<WorkoutSplit?>(null) }
    var newDayName by remember { mutableStateOf("") }
    var newDayOfWeek by remember { mutableStateOf("Monday") }

    var expandedSplitId by remember { mutableStateOf<Int?>(null) }
    val activeSplit by viewModel.activeSplit.collectAsState()

    // Automatically expand the active split when it updates
    LaunchedEffect(activeSplit) {
        if (expandedSplitId == null && activeSplit != null) {
            expandedSplitId = activeSplit?.id
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WORKOUT SPLITS",
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )

            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .background(NeonBlueDim, CircleShape)
                    .size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Split", tint = NeonBlue)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Select a template below or define your custom split. Tap a strategy to expand its scheduled days, active exercises, or to activate it.",
            color = TextGray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(splits) { split ->
                val isExpanded = expandedSplitId == split.id
                val daysState = viewModel.getDaysForSplit(split.id).collectAsState(initial = emptyList())
                val dayCount = daysState.value.size
                val summaryText = if (dayCount == 0) "No training days scheduled" else "$dayCount training days • ${7 - dayCount} rest days"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clickable {
                            expandedSplitId = if (isExpanded) null else split.id
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (split.isActive) SurfaceCardBorder else SurfaceCard
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (split.isActive) NeonBlue else SurfaceCardBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = split.name,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    if (split.isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(ActiveGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .border(1.dp, ActiveGreen, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = ActiveGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (split.isCustom) "Custom Strategy" else "Predefined Routine",
                                    color = if (split.isActive) NeonBlue else TextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (split.isCustom) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(FailureMagenta.copy(alpha = 0.1f), CircleShape)
                                            .clickable {
                                                splitToDelete = split
                                                showDeleteSplitConfirm = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Split",
                                            tint = FailureMagenta.copy(alpha = 0.85f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (!split.isActive) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                brush = Brush.horizontalGradient(listOf(BlueGradientStart, BlueGradientEnd)),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.activateSplit(split) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ACTIVATE",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = NeonBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (!isExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = summaryText,
                                color = TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                HorizontalDivider(color = SurfaceCardBorder, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "WEEKLY SCHEDULE",
                                    color = NeonBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                if (daysState.value.isEmpty()) {
                                    Text(
                                        text = "No workout days scheduled. Add one below!",
                                        color = TextGray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    val sortedDays = daysState.value.sortedWith(compareBy {
                                        when (it.dayOfWeek.lowercase()) {
                                            "monday" -> 1
                                            "tuesday" -> 2
                                            "wednesday" -> 3
                                            "thursday" -> 4
                                            "friday" -> 5
                                            "saturday" -> 6
                                            "sunday" -> 7
                                            else -> 8
                                        }
                                    })

                                    sortedDays.forEach { day ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .background(SurfaceCard, RoundedCornerShape(12.dp))
                                                .border(BorderStroke(1.dp, if (split.isActive) NeonBlueDim else SurfaceCardBorder), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    viewModel.selectedDay = day
                                                    viewModel.selectedSplit = split
                                                    viewModel.currentScreen = "split_day_details"
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(if (split.isActive) NeonBlueDim else SurfaceCardBorder, RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = day.dayOfWeek.take(3).uppercase(),
                                                        color = if (split.isActive) NeonBlue else TextWhite,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Text(
                                                    text = day.name,
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val exercisesState = viewModel.getExercisesForDay(day.id).collectAsState(initial = emptyList())
                                                val count = exercisesState.value.size
                                                if (count > 0) {
                                                    Text(
                                                        text = "$count ${if (count == 1) "ex" else "exs"}",
                                                        color = TextGray,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = "View exercises",
                                                    tint = NeonBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSplitForNewDay = split
                                            showAddDayDialog = true
                                        }
                                        .border(BorderStroke(1.dp, NeonBlueDim), RoundedCornerShape(12.dp))
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add day",
                                        tint = NeonBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ADD WORKOUT DAY",
                                        color = NeonBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Custom Split", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newSplitName,
                            onValueChange = { newSplitName = it },
                            label = { Text("Split Name (e.g. 5-Day Power)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = TextGray
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Creates a standard 3-day workout framework which you can fully customize.", color = TextGray, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newSplitName.isNotEmpty()) {
                                viewModel.createCustomSplit(
                                    name = newSplitName,
                                    days = listOf(
                                        Pair("Day 1 - Push", "Monday"),
                                        Pair("Day 2 - Pull", "Wednesday"),
                                        Pair("Day 3 - Legs", "Friday")
                                    )
                                )
                                showCreateDialog = false
                                newSplitName = ""
                            }
                        }
                    ) {
                        Text("Create", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        if (showDeleteSplitConfirm && splitToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteSplitConfirm = false },
                title = { Text("Delete Custom Split?", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete the split \"${splitToDelete?.name}\"? This will also delete all its scheduled days.", color = TextGray) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            splitToDelete?.let { viewModel.deleteSplit(it) }
                            showDeleteSplitConfirm = false
                            splitToDelete = null
                        }
                    ) {
                        Text("Delete", color = FailureMagenta, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSplitConfirm = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        if (showAddDayDialog && selectedSplitForNewDay != null) {
            val daysOfWeekList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            AlertDialog(
                onDismissRequest = { showAddDayDialog = false },
                title = { Text("Add Day to ${selectedSplitForNewDay?.name}", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newDayName,
                            onValueChange = { newDayName = it },
                            label = { Text("Day Name (e.g. Pull Day)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = TextGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scheduled Day of Week:", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            daysOfWeekList.forEach { dOfWeek ->
                                val isSelected = newDayOfWeek == dOfWeek
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) NeonBlue else SurfaceCardBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { newDayOfWeek = dOfWeek }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = dOfWeek,
                                        color = if (isSelected) Color.Black else TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newDayName.isNotEmpty()) {
                                selectedSplitForNewDay?.let {
                                    viewModel.addDayToSplit(it.id, newDayName, newDayOfWeek)
                                }
                                showAddDayDialog = false
                                newDayName = ""
                            }
                        }
                    ) {
                        Text("Add", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDayDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- Screen 3b: Split Day Details / Edit Day Exercises ---

@Composable
fun SplitDayDetailsScreen(
    viewModel: WorkoutViewModel,
    day: WorkoutDay,
    split: WorkoutSplit
) {
    val dayExercises by viewModel.getExercisesForDay(day.id).collectAsState(initial = emptyList())
    val allLibraryExercises by viewModel.exercises.collectAsState()
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        var showEditDayDialog by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = { viewModel.currentScreen = "splits" }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonBlue)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = split.name.uppercase(), color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${day.name} (${day.dayOfWeek})", color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showEditDayDialog = true },
                    modifier = Modifier
                        .background(SurfaceCardBorder, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Day Details",
                        tint = NeonBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.deleteDay(day) },
                    modifier = Modifier
                        .background(SurfaceCardBorder, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Day",
                        tint = FailureMagenta,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (showEditDayDialog) {
            var editedName by remember { mutableStateOf(day.name) }
            var editedDayOfWeek by remember { mutableStateOf(day.dayOfWeek) }
            val daysOfWeekList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

            AlertDialog(
                onDismissRequest = { showEditDayDialog = false },
                title = { Text("Edit Day Settings", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Day Name (e.g. Pull Day)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = TextGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scheduled Day of Week:", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            daysOfWeekList.forEach { dOfWeek ->
                                val isSelected = editedDayOfWeek.equals(dOfWeek, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) NeonBlue else SurfaceCardBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { editedDayOfWeek = dOfWeek }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = dOfWeek,
                                        color = if (isSelected) Color.Black else TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editedName.isNotEmpty()) {
                                viewModel.updateDay(day.copy(name = editedName, dayOfWeek = editedDayOfWeek))
                                showEditDayDialog = false
                            }
                        }
                    ) {
                        Text("Save", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDayDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NeonButton(
                text = "START WORKOUT",
                onClick = { viewModel.startWorkout(day) },
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PlayArrow
            )

            NeonButton(
                text = "ADD EXERCISE",
                onClick = { showAddExerciseDialog = true },
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Add,
                isSecondary = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "EXERCISES SCHEDULED", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(10.dp))

        if (dayExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No exercises added to this day. Add exercises to schedule.", color = TextGray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayExercises) { exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    viewModel.selectedExercise = exercise
                                    viewModel.currentScreen = "exercise_details"
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = exercise.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "${exercise.muscleGroup} • ${exercise.equipment}", color = TextGray, fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = { viewModel.removeExerciseFromDay(day.id, exercise.id) }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = FailureMagenta)
                            }
                        }
                    }
                }
            }
        }

        if (showAddExerciseDialog) {
            AlertDialog(
                onDismissRequest = { showAddExerciseDialog = false },
                title = { Text("Add Scheduled Exercise", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.height(250.dp)) {
                        Text(text = "Select from library:", color = TextGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(allLibraryExercises) { exercise ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addExerciseToDay(day.id, exercise)
                                            showAddExerciseDialog = false
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = exercise.name, color = TextWhite)
                                    Text(text = exercise.muscleGroup, color = NeonBlue, fontSize = 12.sp)
                                }
                                Divider(color = SurfaceCardBorder)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddExerciseDialog = false }) {
                        Text("Close", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }
    }
}

// --- Screen 4: Exercise Library ---

@Composable
fun ExerciseLibraryScreen(
    viewModel: WorkoutViewModel,
    exercises: List<Exercise>
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newExName by remember { mutableStateOf("") }
    var newExMuscle by remember { mutableStateOf("Chest") }
    var newExEquip by remember { mutableStateOf("Barbell") }
    var newExDiff by remember { mutableStateOf("Beginner") }
    var newExDesc by remember { mutableStateOf("") }

    val categories = listOf("All", "Chest", "Back", "Shoulders", "Legs", "Arms", "Core")

    val filteredExercises = exercises.filter {
        val matchesSearch = it.name.contains(viewModel.searchQuery, ignoreCase = true)
        val matchesCategory = viewModel.selectedMuscleFilter == "All" || it.muscleGroup == viewModel.selectedMuscleFilter
        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXERCISE LIBRARY",
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )

            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .background(NeonBlueDim, CircleShape)
                    .size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Exercise", tint = NeonBlue)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { Text("Search exercise name...", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Category Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = viewModel.selectedMuscleFilter == cat
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) NeonBlue else SurfaceCard,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectedMuscleFilter = cat }
                        .padding(vertical = 8.dp, horizontal = 14.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.Black else TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredExercises) { exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                viewModel.selectedExercise = exercise
                                viewModel.currentScreen = "exercise_details"
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NeonBlueDim, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = NeonBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = exercise.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "${exercise.muscleGroup} • ${exercise.equipment}", color = TextGray, fontSize = 12.sp)
                            }
                        }

                        IconButton(onClick = { viewModel.toggleFavorite(exercise) }) {
                            Icon(
                                imageVector = if (exercise.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (exercise.isFavorite) OrangeWarm else TextGray
                            )
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Custom Exercise", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = newExName,
                            onValueChange = { newExName = it },
                            label = { Text("Exercise Name") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = NeonBlue, unfocusedBorderColor = TextGray),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newExMuscle,
                            onValueChange = { newExMuscle = it },
                            label = { Text("Muscle Group") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = NeonBlue, unfocusedBorderColor = TextGray),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newExEquip,
                            onValueChange = { newExEquip = it },
                            label = { Text("Equipment Required") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = NeonBlue, unfocusedBorderColor = TextGray),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newExDesc,
                            onValueChange = { newExDesc = it },
                            label = { Text("Short Description") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = NeonBlue, unfocusedBorderColor = TextGray)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newExName.isNotEmpty()) {
                                viewModel.createCustomExercise(
                                    name = newExName,
                                    category = "Custom",
                                    muscle = newExMuscle,
                                    equip = newExEquip,
                                    diff = newExDiff,
                                    desc = newExDesc,
                                    steps = "1. Keep posture tight.\n2. Execute movement safely."
                                )
                                showCreateDialog = false
                                newExName = ""
                                newExDesc = ""
                            }
                        }
                    ) {
                        Text("Create", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- Screen 5: Exercise Details ---

@Composable
fun ExerciseDetailsScreen(
    viewModel: WorkoutViewModel,
    exercise: Exercise
) {
    val context = LocalContext.current
    val historySets by viewModel.getSetsForExercise(exercise.id).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.currentScreen = "exercise_library" }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonBlue)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "EXERCISE DETAIL", color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderGlow = true
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = exercise.name, color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text(text = "${exercise.muscleGroup} • ${exercise.equipment}", color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = { viewModel.toggleFavorite(exercise) }) {
                    Icon(
                        imageVector = if (exercise.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (exercise.isFavorite) OrangeWarm else TextGray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Difficulty: ${exercise.difficulty}",
                color = if (exercise.difficulty == "Hard") FailureMagenta else ActiveGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = exercise.description.ifEmpty { "Classic barbell movement targeting key lifting muscle clusters." },
                color = TextGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (exercise.youtubeUrl.isNotEmpty()) {
            NeonButton(
                text = "WATCH YOUTUBE TUTORIAL",
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exercise.youtubeUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open video link.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.PlayCircleFilled
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Execution Steps
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "EXECUTION STEPS", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exercise.executionSteps.ifEmpty { "1. Stand tall.\n2. Engage core.\n3. Execute movement with control." },
                color = TextGray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // History logs
        Text(text = "PERSONAL HISTORY LOGS", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (historySets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No workouts logged yet for this exercise.", color = TextGray, fontSize = 13.sp)
            }
        } else {
            for (set in historySets.take(10)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Set ${set.setIndex}", color = TextWhite, fontSize = 13.sp)
                    Text(text = "${set.weight} kg x ${set.reps}", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "Done", color = ActiveGreen, fontSize = 12.sp)
                }
                Divider(color = SurfaceCardBorder)
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- Screen 6: Active Workout (Logger) ---

@Composable
fun ActiveWorkoutScreen(
    viewModel: WorkoutViewModel,
    session: WorkoutSession,
    sets: List<LoggedSet>
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    val allExercises by viewModel.exercises.collectAsState()

    val groupedSets = sets.groupBy { it.exerciseId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Header timer + title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.dayName.uppercase(),
                    color = NeonBlue,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Text(
                    text = "LIFTING SESSION",
                    color = TextGray,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            // Duration Clock
            Box(
                modifier = Modifier
                    .background(NeonBlueDim, RoundedCornerShape(10.dp))
                    .border(1.dp, NeonBlue, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = formatDuration(viewModel.workoutDurationSeconds),
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive List of Exercises & Set tables
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(groupedSets.keys.toList()) { exerciseId ->
                val exerciseSets = groupedSets[exerciseId] ?: emptyList()
                val exName = exerciseSets.firstOrNull()?.exerciseName ?: "Exercise"
                
                val previousSets = remember(exerciseId) {
                    viewModel.getPreviousSetsForExerciseAndDay(exerciseId, session.dayName)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exName,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Row {
                                IconButton(onClick = { viewModel.addSetToActiveExercise(exerciseId, exName) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Set", tint = NeonBlue)
                                }
                                IconButton(onClick = { viewModel.removeSetFromActiveExercise(exerciseId) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Remove Set", tint = FailureMagenta)
                                }
                            }
                        }

                        if (previousSets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceCardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 6.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = NeonBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Last on ${session.dayName}: " + previousSets.joinToString(", ") { set ->
                                        "${set.weight.toInt()}kg x ${set.reps}" + (set.rpe?.let { " @RPE$it" } ?: "") +
                                                (if (set.isWarmup) " (W)" else if (set.isDropSet) " (D)" else if (set.isFailure) " (F)" else "")
                                    },
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sets Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "SET", color = TextGray, fontSize = 10.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                            Text(text = "KG", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(text = "REPS", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(text = "RPE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                            Text(text = "TYPE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                            Text(text = "DONE", color = TextGray, fontSize = 10.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                        }

                        Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 6.dp))

                        // Sets list
                        exerciseSets.forEach { set ->
                            // Find the global index in the lists of all active sets
                            val listIndex = sets.indexOf(set)
                            if (listIndex != -1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${set.setIndex}",
                                        color = if (set.isCompleted) ActiveGreen else TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(30.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    // Weight input
                                    var weightText by remember(set.weight) { mutableStateOf(set.weight.toString()) }
                                    Box(modifier = Modifier.weight(1f).padding(horizontal = 3.dp)) {
                                        BasicTextField(
                                            value = weightText,
                                            onValueChange = {
                                                weightText = it
                                                val parsed = it.toFloatOrNull() ?: 0f
                                                viewModel.updateSetLogging(listIndex, parsed, set.reps)
                                            },
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                            modifier = Modifier
                                                .background(SurfaceCardBorder, RoundedCornerShape(8.dp))
                                                .padding(6.dp)
                                                .fillMaxWidth()
                                        )
                                    }

                                    // Reps input
                                    var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }
                                    Box(modifier = Modifier.weight(1f).padding(horizontal = 3.dp)) {
                                        BasicTextField(
                                            value = repsText,
                                            onValueChange = {
                                                repsText = it
                                                val parsed = it.toIntOrNull() ?: 0
                                                viewModel.updateSetLogging(listIndex, set.weight, parsed)
                                            },
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                            modifier = Modifier
                                                .background(SurfaceCardBorder, RoundedCornerShape(8.dp))
                                                .padding(6.dp)
                                                .fillMaxWidth()
                                        )
                                    }

                                    // RPE intensity input
                                    var rpeText by remember(set.rpe) { mutableStateOf(set.rpe?.toString() ?: "") }
                                    Box(modifier = Modifier.weight(0.9f).padding(horizontal = 3.dp)) {
                                        BasicTextField(
                                            value = rpeText,
                                            onValueChange = {
                                                rpeText = it
                                                val parsed = it.toIntOrNull()?.coerceIn(1, 10)
                                                viewModel.updateSetRpe(listIndex, parsed)
                                            },
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            modifier = Modifier
                                                .background(SurfaceCardBorder, RoundedCornerShape(8.dp))
                                                .padding(6.dp)
                                                .fillMaxWidth(),
                                            decorationBox = { innerTextField ->
                                                if (rpeText.isEmpty()) {
                                                    Text(
                                                        text = "-",
                                                        color = TextGrayMuted,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }

                                    // Type toggles (W: Warmup, D: Dropset, F: Failure, R: Regular)
                                    Row(
                                        modifier = Modifier.weight(0.9f),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        val typeChar = when {
                                            set.isWarmup -> "W"
                                            set.isDropSet -> "D"
                                            set.isFailure -> "F"
                                            else -> "R" // Normal/Regular
                                        }

                                        val typeColor = when (typeChar) {
                                            "W" -> OrangeWarm
                                            "D" -> PurpleAura
                                            "F" -> FailureMagenta
                                            else -> NeonBlue
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clickable {
                                                    when (typeChar) {
                                                        "R" -> viewModel.toggleSetWarmup(listIndex)
                                                        "W" -> viewModel.toggleSetDropSet(listIndex)
                                                        "D" -> viewModel.toggleSetFailure(listIndex)
                                                        "F" -> {
                                                            // cycle back to R
                                                            viewModel.toggleSetFailure(listIndex) // resets to R
                                                        }
                                                    }
                                                }
                                                .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .border(1.dp, typeColor, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(text = typeChar, color = typeColor, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        }
                                    }

                                    // Completed checkbox
                                    Box(
                                        modifier = Modifier.width(42.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Checkbox(
                                            checked = set.isCompleted,
                                            onCheckedChange = { viewModel.toggleSetCompleted(listIndex) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = ActiveGreen,
                                                uncheckedColor = TextGray,
                                                checkmarkColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Session notes
        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            placeholder = { Text("Log training notes for this session...", color = TextGray, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Action panel (Cancel, Add exercise, Finish)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showCancelDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FailureMagenta.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).border(1.dp, FailureMagenta, RoundedCornerShape(12.dp))
            ) {
                Text(text = "CANCEL", color = FailureMagenta, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showAddExerciseSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.2f).border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = NeonBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "ADD LIFT", color = NeonBlue, fontWeight = FontWeight.Bold)
            }

            NeonButton(
                text = "FINISH",
                onClick = { viewModel.finishWorkout(notesText) },
                modifier = Modifier.weight(1.4f)
            )
        }

        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Discard Workout?", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = { Text("All logged progress for the active session will be permanently erased.", color = TextGray) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.cancelWorkout()
                        showCancelDialog = false
                    }) {
                        Text("Discard", color = FailureMagenta, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Keep Logging", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        if (showAddExerciseSheet) {
            AlertDialog(
                onDismissRequest = { showAddExerciseSheet = false },
                title = { Text("Add Exercise to Active Session", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.height(260.dp)) {
                        LazyColumn {
                            items(allExercises) { exercise ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addExerciseToActiveWorkout(exercise)
                                            showAddExerciseSheet = false
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = exercise.name, color = TextWhite)
                                    Text(text = exercise.muscleGroup, color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Divider(color = SurfaceCardBorder)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddExerciseSheet = false }) {
                        Text("Close", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }
    }
}

// --- Screen 7: Progress Analytics Dashboard & Calendar ---

@Composable
fun ProgressScreen(
    viewModel: WorkoutViewModel,
    sessions: List<WorkoutSession>,
    measurements: List<BodyMeasurement>
) {
    val prs = viewModel.calculatePRs()

    var showEditWeightDialog by remember { mutableStateOf(false) }
    var weightEditValue by remember { mutableStateOf("") }

    // Calculate weekly workouts
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    val workoutsThisWeek = sessions.count { it.startTime >= sevenDaysAgo }
    val targetWorkouts = when {
        viewModel.userActivityLevel.contains("sedentary", ignoreCase = true) -> 2
        viewModel.userActivityLevel.contains("athlete", ignoreCase = true) -> 5
        else -> 3
    }

    // Weight tracking
    val sortedMeasurements = measurements.sortedBy { it.timestamp }
    val currentWeight = if (sortedMeasurements.isNotEmpty()) {
        sortedMeasurements.last().weight
    } else {
        viewModel.userWeight.toFloatOrNull() ?: 70f
    }

    val previousWeight = if (sortedMeasurements.size > 1) {
        sortedMeasurements[sortedMeasurements.size - 2].weight
    } else {
        viewModel.userWeight.toFloatOrNull() ?: 70f
    }

    val weightDiff = currentWeight - previousWeight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "ANALYTICS & PROGRESS",
            color = TextWhite,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // High Quality Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NeonCard(modifier = Modifier.weight(1f)) {
                Text(text = "VOLUME COMPLETED", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val totalVol = sessions.sumOf { it.totalVolume.toDouble() }.toInt()
                Text(text = "$totalVol kg", color = NeonBlue, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }

            NeonCard(modifier = Modifier.weight(1f)) {
                Text(text = "WORKOUT MINUTES", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val totalMin = sessions.sumOf { it.duration } / 60
                Text(text = "$totalMin mins", color = ActiveGreen, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Weight Progress & Insights Card
        Text(text = "BODY STATS & FREQUENCY ANALYTICS", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            val frequencyIsGood = workoutsThisWeek >= targetWorkouts
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                weightEditValue = currentWeight.toString()
                                showEditWeightDialog = true
                            }
                            .padding(4.dp)
                    ) {
                        Column {
                            Text(text = "CURRENT WEIGHT", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "$currentWeight kg", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Weight",
                                    tint = NeonBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "PREVIOUS WEIGHT", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$previousWeight kg", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Change Indicator
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "NET CHANGE", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val changeSign = if (weightDiff > 0f) "+" else ""
                        val changeColor = if (viewModel.userGoal.contains("lose", ignoreCase = true)) {
                            if (weightDiff <= 0f) ActiveGreen else FailureMagenta
                        } else if (viewModel.userGoal.contains("gain", ignoreCase = true)) {
                            if (weightDiff >= 0f) ActiveGreen else FailureMagenta
                        } else {
                            if (Math.abs(weightDiff) < 0.5f) ActiveGreen else TextGray
                        }
                        Text(
                            text = "$changeSign${String.format(Locale.getDefault(), "%.1f", weightDiff)} kg",
                            color = changeColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 14.dp))

                // Frequency analytics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "WORKOUTS THIS WEEK", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "$workoutsThisWeek", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = " / $targetWorkouts target", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    // Evaluation Badge
                    val statusText = if (frequencyIsGood) "GOING GOOD 🔥" else "INCREASE FREQUENCY ⚡"
                    val statusColor = if (frequencyIsGood) ActiveGreen else OrangeWarm

                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gyani's Dynamic Progress Advice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCardBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !frequencyIsGood -> "Gyani recommends adding at least ${targetWorkouts - workoutsThisWeek} more workout session(s) this week. Increasing your training frequency is key to triggering muscle synthesis and optimizing metabolic output for your '${viewModel.userGoal}' goal!"
                            weightDiff < 0f && viewModel.userGoal.contains("lose", ignoreCase = true) -> "Excellent work! You are currently losing weight at a healthy pace. Your consistency in logging meals and workouts is paying off. Keep pushing and maintain your current caloric targets!"
                            weightDiff > 0f && viewModel.userGoal.contains("gain", ignoreCase = true) -> "Fantastic growth! You are successfully gaining mass. Continue tracking your protein intake of ${viewModel.targetProtein}g to ensure the weight goes towards muscle mass. You are going strong!"
                            else -> "You are going great! Your consistency is exemplary. Continue logging your daily nutrition and training splits to maintain high training volume."
                        },
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Personal Records Hall of Badges
        Text(text = "PERSONAL RECORDS & BADGES", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(OrangeWarm.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, OrangeWarm, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = "PR", tint = OrangeWarm, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = "HEAVIEST LIFT (PR)", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (prs.highestWeight > 0) "${prs.highestWeight} kg on ${prs.highestWeightExercise}" else "No Lift Logged Yet",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(PurpleAura.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, PurpleAura, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Volume", tint = PurpleAura, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = "MAX SINGLE SESSION VOLUME", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (prs.highestVolume > 0) "${prs.highestVolume.toInt()} kg on ${prs.highestVolumeDay}" else "No Session Logged Yet",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(NeonBlue.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, NeonBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = "Reps", tint = NeonBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = "MOST REPS SINGLE SET", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (prs.highestReps > 0) "${prs.highestReps} reps on ${prs.highestRepsExercise}" else "No Reps Logged Yet",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive Calendar Layout Grid
        val cal = Calendar.getInstance()
        val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        Text(text = "TRAINING COMPLIANCE - ${currentMonthName.uppercase(Locale.getDefault())}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { dayLabel ->
                    Text(text = dayLabel, color = TextGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Highlight days of the month with completed workouts
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)
            val completedDates = sessions.filter {
                val sessionCal = Calendar.getInstance()
                sessionCal.timeInMillis = it.startTime
                sessionCal.get(Calendar.MONTH) == currentMonth && sessionCal.get(Calendar.YEAR) == currentYear
            }.map {
                val sessionCal = Calendar.getInstance()
                sessionCal.timeInMillis = it.startTime
                sessionCal.get(Calendar.DAY_OF_MONTH)
            }.toSet()

            val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val startDayOfWeek = (startCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday is 0

            val totalSlots = totalDays + startDayOfWeek
            val rows = (totalSlots + 6) / 7

            Column {
                repeat(rows) { rowIndex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(7) { colIndex ->
                            val slotIndex = rowIndex * 7 + colIndex
                            val dayNum = slotIndex - startDayOfWeek + 1
                            val isValidDay = dayNum in 1..totalDays
                            val isCompleted = isValidDay && completedDates.contains(dayNum)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .background(
                                        if (isCompleted) NeonBlue else if (isValidDay) SurfaceCardBorder else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (isCompleted) NeonBlue else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isValidDay) {
                                    Text(
                                        text = "$dayNum",
                                        color = if (isCompleted) Color.Black else TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Body Measurements Section shortcut button
        NeonButton(
            text = "LOG BODY MEASUREMENTS",
            onClick = { viewModel.currentScreen = "body_measurements" },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.AccessibilityNew
        )

        if (showEditWeightDialog) {
            AlertDialog(
                onDismissRequest = { showEditWeightDialog = false },
                title = { Text("Update Current Weight", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Update your current weight. This will log a new weight point on your timeline and automatically update all your nutrition and calorie targets!",
                            color = TextGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = weightEditValue,
                            onValueChange = { weightEditValue = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = TextGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val w = weightEditValue.toFloatOrNull() ?: 0f
                            if (w > 0f) {
                                viewModel.updateCurrentWeight(w)
                                showEditWeightDialog = false
                            }
                        }
                    ) {
                        Text("Save", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditWeightDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- Screen 8: Body Measurements Tracking ---

@Composable
fun BodyMeasurementsScreen(
    viewModel: WorkoutViewModel,
    measurements: List<BodyMeasurement>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }
    var fatInput by remember { mutableStateOf("") }
    var chestInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var armsInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.currentScreen = "progress" }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonBlue)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "BODY STATS", color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        NeonButton(
            text = "LOG NEW METRICS",
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Add
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "HISTORICAL METRIC TIMELINE", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(10.dp))

        if (measurements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No body stats logged yet. Track your weight & body fat progress offline.", color = TextGray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(measurements) { metric ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(metric.timestamp)),
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(text = "Weight: ${metric.weight} kg", color = TextGray, fontSize = 12.sp)
                                    Text(text = "Fat: ${metric.bodyFat}%", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteBodyMeasurement(metric) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = FailureMagenta)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Log Body Metrics", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = NeonBlue, unfocusedBorderColor = TextGray),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fatInput,
                            onValueChange = { fatInput = it },
                            label = { Text("Body Fat %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = NeonBlue, unfocusedBorderColor = TextGray),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = chestInput,
                            onValueChange = { chestInput = it },
                            label = { Text("Chest Circumference (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = NeonBlue, unfocusedBorderColor = TextGray),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val w = weightInput.toFloatOrNull() ?: 0f
                            val f = fatInput.toFloatOrNull() ?: 0f
                            val c = chestInput.toFloatOrNull() ?: 0f
                            if (w > 0f) {
                                viewModel.addBodyMeasurement(
                                    weight = w,
                                    bodyFat = f,
                                    chest = c,
                                    waist = 0f, arms = 0f, thighs = 0f, calves = 0f, shoulders = 0f, neck = 0f, hip = 0f
                                )
                                showAddDialog = false
                                weightInput = ""
                                fatInput = ""
                                chestInput = ""
                            }
                        }
                    ) {
                        Text("Log", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }
    }
}

// --- Screen 9: Settings & Local Backup ---

@Composable
fun SettingsScreen(viewModel: WorkoutViewModel) {
    val context = LocalContext.current
    var showBackupString by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "SYSTEM SETTINGS",
            color = TextWhite,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "PREFERENCES", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Metric Units (kg/cm)", color = TextWhite)
                Switch(
                    checked = true,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonBlue)
                )
            }

            Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Neon Glowing Accents", color = TextWhite)
                Switch(
                    checked = true,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonBlue)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Backup Options
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "JSON OFFLINE BACKUP", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Generate or load a portable JSON text file containing your complete workout log history to prevent data loss.", color = TextGray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        backupJsonText = viewModel.exportBackupJson()
                        showBackupString = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f).border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = NeonBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "EXPORT", color = NeonBlue, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (!showBackupString) {
                            showBackupString = true
                            backupJsonText = ""
                        } else {
                            val msg = viewModel.importBackupJson(backupJsonText)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f).border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = ActiveGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (showBackupString) "CONFIRM" else "IMPORT", color = ActiveGreen, fontWeight = FontWeight.Bold)
                }
            }

            if (showBackupString) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = backupJsonText,
                    onValueChange = { backupJsonText = it },
                    label = { Text("Paste JSON Backup here to Import / Copy to Export", color = TextGray) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = NeonBlue, unfocusedBorderColor = SurfaceCardBorder),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- Screen 10: AI Recommendation Coach with Gemini ---

@Composable
fun AICoachScreen(viewModel: WorkoutViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.currentScreen = "dashboard" }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonBlue)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "AURAFIT AI COACH", color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = PurpleAura,
            borderGlow = true
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PurpleAura,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = "Gemini Powered Strategy", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Analyzes your logged metrics & volume to supply premium strength feedback.", color = TextGray, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Response Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SurfaceCard, RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(18.dp))
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            if (viewModel.isAiLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonBlue)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Consulting AuraFit AI...", color = TextGray, fontSize = 14.sp)
                }
            } else {
                Text(
                    text = viewModel.aiAdvice,
                    color = TextWhite,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        NeonButton(
            text = "GENERATE COACHING INSIGHTS",
            onClick = { viewModel.askAiForRecommendations() },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.AutoAwesome
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- Helper Functions ---

fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

// --- Screen 11: Nutrition Tracker & AI Calorie Bot ---

@Composable
fun NutritionScreen(
    viewModel: WorkoutViewModel,
    foodLogs: List<FoodLog>
) {
    var showAddManualDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val todayStart = remember(foodLogs) {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todaysLogs = remember(foodLogs, todayStart) {
        foodLogs.filter { it.timestamp >= todayStart }
    }

    val totalCaloriesEaten = todaysLogs.sumOf { it.calories }
    val totalProteinEaten = todaysLogs.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbsEaten = todaysLogs.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFatEaten = todaysLogs.sumOf { it.fat.toDouble() }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Title and Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AURA NUTRITION",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Fuel Your Gains",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            Row {
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .background(SurfaceCard, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Goals",
                        tint = NeonBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showAddManualDialog = true },
                    modifier = Modifier
                        .background(NeonBlue, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Add",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Diary directly displayed
        NutritionDiaryView(
            viewModel = viewModel,
            todaysLogs = todaysLogs,
            totalCaloriesEaten = totalCaloriesEaten,
            totalProteinEaten = totalProteinEaten,
            totalCarbsEaten = totalCarbsEaten,
            totalFatEaten = totalFatEaten,
            onEditTargetsClick = { showSettingsDialog = true }
        )
    }

    // Goal Targets Dialog
    if (showSettingsDialog) {
        var calInput by remember { mutableStateOf(viewModel.targetCalories.toString()) }
        var proteinInput by remember { mutableStateOf(viewModel.targetProtein.toString()) }
        var carbsInput by remember { mutableStateOf(viewModel.targetCarbs.toString()) }
        var fatInput by remember { mutableStateOf(viewModel.targetFat.toString()) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SET NUTRITION GOALS",
                        color = TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = calInput,
                        onValueChange = { calInput = it },
                        label = { Text("Daily Calories Target (kcal)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = SurfaceCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = proteinInput,
                        onValueChange = { proteinInput = it },
                        label = { Text("Protein Target (g)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = SurfaceCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = carbsInput,
                        onValueChange = { carbsInput = it },
                        label = { Text("Carbohydrates Target (g)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = SurfaceCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fatInput,
                        onValueChange = { fatInput = it },
                        label = { Text("Fat Target (g)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = SurfaceCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val suggested = viewModel.getOfflineSuggestedTargets()
                            calInput = suggested.calories.toString()
                            proteinInput = suggested.protein.toString()
                            carbsInput = suggested.carbs.toString()
                            fatInput = suggested.fat.toString()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AUTO-CALCULATE FROM PROFILE", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Cancel", color = TextGray)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                calInput.toIntOrNull()?.let { viewModel.updateTargetCalories(it) }
                                proteinInput.toIntOrNull()?.let { viewModel.updateTargetProtein(it) }
                                carbsInput.toIntOrNull()?.let { viewModel.updateTargetCarbs(it) }
                                fatInput.toIntOrNull()?.let { viewModel.updateTargetFat(it) }
                                showSettingsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                        ) {
                            Text("Save Goals", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Manual Food Add Dialog
    if (showAddManualDialog) {
        var foodName by remember { mutableStateOf("") }
        var calsInput by remember { mutableStateOf("") }
        var proteinVal by remember { mutableStateOf("") }
        var carbsVal by remember { mutableStateOf("") }
        var fatVal by remember { mutableStateOf("") }
        var mealTypeChoice by remember { mutableStateOf("Breakfast") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddManualDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MANUALLY LOG FOOD",
                        color = TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Food Name (e.g. Rice & Chicken)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = SurfaceCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = calsInput,
                        onValueChange = { calsInput = it },
                        label = { Text("Calories (kcal)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = SurfaceCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = proteinVal,
                            onValueChange = { proteinVal = it },
                            label = { Text("Prot (g)", color = TextGray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = SurfaceCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = carbsVal,
                            onValueChange = { carbsVal = it },
                            label = { Text("Carb (g)", color = TextGray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = SurfaceCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                        )
                        OutlinedTextField(
                            value = fatVal,
                            onValueChange = { fatVal = it },
                            label = { Text("Fat (g)", color = TextGray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = SurfaceCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Meal Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val types = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                        types.forEach { type ->
                            val isSelected = mealTypeChoice == type
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) NeonBlue else SurfaceCardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { mealTypeChoice = type }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSelected) Color.Black else TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddManualDialog = false }) {
                            Text("Cancel", color = TextGray)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (foodName.isNotEmpty()) {
                                    val calories = calsInput.toIntOrNull() ?: 0
                                    val p = proteinVal.toFloatOrNull() ?: 0f
                                    val c = carbsVal.toFloatOrNull() ?: 0f
                                    val f = fatVal.toFloatOrNull() ?: 0f
                                    viewModel.addManualFoodLog(
                                        name = foodName,
                                        calories = calories,
                                        protein = p,
                                        carbs = c,
                                        fat = f,
                                        mealType = mealTypeChoice
                                    )
                                    showAddManualDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                        ) {
                            Text("Log Item", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionDiaryView(
    viewModel: WorkoutViewModel,
    todaysLogs: List<FoodLog>,
    totalCaloriesEaten: Int,
    totalProteinEaten: Float,
    totalCarbsEaten: Float,
    totalFatEaten: Float,
    onEditTargetsClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Daily Summary Ring & Progress Card (Clickable to Edit Targets!)
        NeonCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditTargetsClick() },
            borderColor = NeonBlue.copy(alpha = 0.35f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TODAY'S CALORIES",
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Targets",
                                tint = NeonBlue,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$totalCaloriesEaten",
                                color = TextWhite,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = " / ${viewModel.targetCalories} kcal",
                                color = TextGray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    // Simple circular indicator or remaining text
                    val remaining = (viewModel.targetCalories - totalCaloriesEaten).coerceAtLeast(0)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "REMAINING",
                            color = TextGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$remaining kcal",
                            color = if (remaining > 0) ActiveGreen else FailureMagenta,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Linear Calorie progress
                val calProgress = if (viewModel.targetCalories > 0) {
                    (totalCaloriesEaten.toFloat() / viewModel.targetCalories).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = calProgress,
                    color = NeonBlue,
                    trackColor = SurfaceCardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Macronutrient Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Protein
                    MacroProgressItem(
                        name = "Protein",
                        current = totalProteinEaten,
                        target = viewModel.targetProtein,
                        color = ActiveGreen,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // Carbs
                    MacroProgressItem(
                        name = "Carbs",
                        current = totalCarbsEaten,
                        target = viewModel.targetCarbs,
                        color = OrangeWarm,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // Fat
                    MacroProgressItem(
                        name = "Fat",
                        current = totalFatEaten,
                        target = viewModel.targetFat,
                        color = PurpleAura,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Food Diary Categories (Breakfast, Lunch, Dinner, Snack)
        val mealCategories = listOf("Breakfast", "Lunch", "Dinner", "Snack")
        mealCategories.forEach { category ->
            val logsForCategory = todaysLogs.filter { it.mealType.equals(category, ignoreCase = true) }
            val calsForCategory = logsForCategory.sumOf { it.calories }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.uppercase(),
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$calsForCategory kcal",
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            if (logsForCategory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(12.dp))
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items logged. Tap '+' or ask AI Bot to estimate!",
                        color = TextGrayMuted,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(12.dp))
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    logsForCategory.forEachIndexed { index, food ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.name,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${food.protein.toInt()}g P  •  ${food.carbs.toInt()}g C  •  ${food.fat.toInt()}g F",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${food.calories} kcal",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteFoodLog(food) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = FailureMagenta.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (index < logsForCategory.lastIndex) {
                            Divider(color = SurfaceCardBorder, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (todaysLogs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = { viewModel.clearAllFoodLogs() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FailureMagenta),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 100.dp),
                border = BorderStroke(1.dp, FailureMagenta.copy(alpha = 0.5f))
            ) {
                Text("Clear All Food Logs", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun MacroProgressItem(
    name: String,
    current: Float,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SurfaceCardBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = name.uppercase(),
            color = TextGray,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${current.toInt()} / ${target}g",
            color = TextWhite,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        val progress = if (target > 0) (current / target).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = progress,
            color = color,
            trackColor = SurfaceCardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun NutritionAiBotView(viewModel: WorkoutViewModel) {
    var textInput by remember { mutableStateOf("") }
    val chatState = viewModel.nutritionChatHistory
    val listState = rememberLazyListState()

    // Auto scroll chat list to bottom
    LaunchedEffect(chatState.size) {
        if (chatState.isNotEmpty()) {
            listState.animateScrollToItem(chatState.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat History Scroll List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .background(SurfaceCard, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            items(chatState) { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!message.isUser) {
                        // Robot Avatar Badge
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(PurpleAura.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, PurpleAura, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PurpleAura,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Chat Bubble Card
                    Box(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .background(
                                if (message.isUser) NeonBlue else SurfaceCardBorder,
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (message.isUser) 16.dp else 2.dp,
                                    bottomEnd = if (message.isUser) 2.dp else 16.dp
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message.text,
                            color = if (message.isUser) Color.Black else TextWhite,
                            fontSize = 13.sp,
                            fontWeight = if (message.isUser) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 18.sp
                        )
                    }

                    if (message.isUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }

            if (viewModel.isNutritionAiLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(PurpleAura.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, PurpleAura, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PurpleAura, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(SurfaceCardBorder, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "AuraFit AI is estimating calories...",
                                color = TextGray,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chat Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 90.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Log what you ate...", color = TextGrayMuted, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )

            IconButton(
                onClick = {
                    if (textInput.trim().isNotEmpty()) {
                        val input = textInput
                        textInput = ""
                        viewModel.sendNutritionChatMessage(input)
                    }
                },
                enabled = !viewModel.isNutritionAiLoading && textInput.trim().isNotEmpty(),
                modifier = Modifier
                    .background(if (textInput.trim().isNotEmpty()) NeonBlue else SurfaceCard, RoundedCornerShape(12.dp))
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (textInput.trim().isNotEmpty()) Color.Black else TextGrayMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

