package com.thatguyalpha.classbar

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()

        setContent {
            val isDark = isSystemInDarkTheme()
            val colorScheme = if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFD0BCFF),
                    onPrimary = Color(0xFF381E72),
                    primaryContainer = Color(0xFF4F378B),
                    surface = Color(0xFF141218),
                    surfaceContainer = Color(0xFF211F26),
                    surfaceContainerLow = Color(0xFF1D1B20)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF6750A4),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFEADDFF),
                    surface = Color(0xFFFEF7FF),
                    surfaceContainer = Color(0xFFF3EDF7),
                    surfaceContainerLow = Color(0xFFF7F2FA)
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted && viewModel.settings.value.isEnabled && viewModel.isLoggedIn.value) {
                        ClassBarService.startOrRefresh(context)
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    if (!isLoggedIn) {
                        val isLoggingIn by viewModel.isLoggingIn.collectAsState()
                        val loginError by viewModel.loginError.collectAsState()

                        LoginScreen(
                            isLoggingIn = isLoggingIn,
                            loginError = loginError,
                            onLogin = { email, pass -> viewModel.login(email, pass) }
                        )
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 1. LOGIN SCREEN
// =========================================================================

@Composable
fun LoginScreen(
    isLoggingIn: Boolean,
    loginError: String?,
    onLogin: (String, String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon & Title
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "ClassBar",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "ClassBar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Sign in with your Shkolo account to view your lessons and status bar room icon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Email / Username Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Username or Email") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "User")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Password")
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isLoggingIn && email.isNotBlank() && password.isNotBlank()) {
                                onLogin(email, password)
                            }
                        }
                    )
                )

                // Error message banner
                if (loginError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = loginError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sign In Button
                Button(
                    onClick = { onLogin(email, password) },
                    enabled = !isLoggingIn && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text("Log In", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. MAIN APPLICATION SCREEN (Tabs: Lessons & Icon)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val studentName by viewModel.studentName.collectAsState()
    val className by viewModel.className.collectAsState()

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out of ClassBar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "ClassBar",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        if (studentName.isNotBlank()) {
                            Text(
                                text = studentName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Log Out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Top Two Tabs: Lessons and Settings
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Lessons") },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tab_lessons),
                                contentDescription = "Lessons"
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Settings") },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LessonsTab(viewModel = viewModel)
                1 -> IconTab(viewModel = viewModel)
            }
        }
    }
}

// =========================================================================
// 3. TAB 1: LESSONS (with Top & Bottom Left/Right Arrows)
// =========================================================================

@Composable
fun LessonsTab(viewModel: MainViewModel) {
    val dateDisplay by viewModel.selectedDateFormatted.collectAsState()
    val isToday by viewModel.isToday.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    val isLoading by viewModel.isLoadingLessons.collectAsState()
    val error by viewModel.lessonsError.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        // TOP NAVIGATION BAR (Left Arrow, Date, Right Arrow)
        DayNavigationHeader(
            dateText = dateDisplay,
            isToday = isToday,
            onPrevClick = { viewModel.navigateDate(-1) },
            onNextClick = { viewModel.navigateDate(1) },
            onTodayClick = { viewModel.goToToday() },
            onRefreshClick = { viewModel.refreshSchedule() }
        )

        // SCROLLABLE LESSONS LIST
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading && lessons.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null && lessons.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading schedule",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshSchedule() }) {
                            Text("Retry")
                        }
                    }
                }
                lessons.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No lessons scheduled for this day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Free day, weekend, or holiday",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(lessons) { lesson ->
                            LessonCardItem(lesson = lesson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayNavigationHeader(
    dateText: String,
    isToday: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onTodayClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day"
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTodayClick() }
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (isToday) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Tap for Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Schedule"
                    )
                }
                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day"
                    )
                }
            }
        }
    }
}

@Composable
fun LessonCardItem(lesson: Lesson) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (lesson.isFreeHour) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Hour Badge & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "#${lesson.hourNumber}  •  ${lesson.fromTime} - ${lesson.toTime}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (lesson.isFreeHour) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Coffee,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Free Hour",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    if (!lesson.group.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = lesson.group,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Subject Title
            Text(
                text = lesson.courseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Bottom Row: Room and Teacher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Room Pill
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = "Room",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lesson.roomName.isNotBlank()) lesson.roomName else "—",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Teacher
                if (lesson.teacherName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Teacher",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lesson.teacherName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. TAB 2: ICON (Position, Width, Boldness Controls & Live Preview)
// =========================================================================

enum class SliderConfigTarget(val label: String, val subtitle: String) {
    ROOM_NUMBER("Room Number Icon", "Configure upcoming classroom number (#)"),
    MINUTES_LEFT("Minutes Left Icon", "Configure lesson countdown minutes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconTab(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val previewCountdownMinutes by viewModel.previewCountdownMinutes.collectAsState()
    val previewCountdownBitmap by viewModel.previewCountdownBitmap.collectAsState()

    var isSlidersExpanded by rememberSaveable { mutableStateOf(true) }
    var selectedTarget by rememberSaveable { mutableStateOf(SliderConfigTarget.ROOM_NUMBER) }
    var targetDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Master Toggle Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Status Bar Icon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (settings.isEnabled) {
                            "Active in status bar • Displays upcoming room number"
                        } else {
                            "Disabled • Turn on to display room in status bar"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { viewModel.setServiceEnabled(it) },
                    modifier = Modifier.testTag("switch_master_enable")
                )
            }
        }

        // 2. Lesson Countdown Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lesson Countdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (settings.countdownEnabled) {
                            "Shows minutes remaining during class • Switches to room on break"
                        } else {
                            "Disabled • Always shows upcoming classroom number"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.countdownEnabled,
                    onCheckedChange = { viewModel.setCountdownEnabled(it) }
                )
            }
        }

        // Notification Appearance Lead Time Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = "Notification Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Controls how early the notification appears before your first lesson of the day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val leadMinutes = settings.notificationLeadTimeMinutes
                val formattedTimeText = when {
                    leadMinutes == 0 -> "At class start (0 min)"
                    leadMinutes < 60 -> "$leadMinutes min before"
                    leadMinutes % 60 == 0 -> "${leadMinutes / 60}h before"
                    else -> "${leadMinutes / 60}h ${leadMinutes % 60}m before"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lead Time",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formattedTimeText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = leadMinutes.toFloat(),
                    onValueChange = { viewModel.setNotificationLeadTimeMinutes(it.roundToInt()) },
                    valueRange = AppPreferences.MIN_NOTIFICATION_LEAD_TIME_MINUTES.toFloat()..AppPreferences.MAX_NOTIFICATION_LEAD_TIME_MINUTES.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Presets:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = { viewModel.setNotificationLeadTimeMinutes(0) }) {
                        Text("0m")
                    }
                    OutlinedButton(onClick = { viewModel.setNotificationLeadTimeMinutes(15) }) {
                        Text("15m")
                    }
                    OutlinedButton(onClick = { viewModel.setNotificationLeadTimeMinutes(30) }) {
                        Text("30m (Default)")
                    }
                    OutlinedButton(onClick = { viewModel.setNotificationLeadTimeMinutes(60) }) {
                        Text("1h")
                    }
                    OutlinedButton(onClick = { viewModel.setNotificationLeadTimeMinutes(120) }) {
                        Text("2h")
                    }
                    OutlinedButton(onClick = { viewModel.setNotificationLeadTimeMinutes(300) }) {
                        Text("5h (Max)")
                    }
                }
            }
        }

        // 3. Position and Width Controls (Sliders with Dropdown)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .clickable { isSlidersExpanded = !isSlidersExpanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Position & Size Sliders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Separate slider configs • ${selectedTarget.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isSlidersExpanded = !isSlidersExpanded }) {
                        Icon(
                            imageVector = if (isSlidersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isSlidersExpanded) "Collapse Sliders" else "Expand Sliders"
                        )
                    }
                }

                AnimatedVisibility(visible = isSlidersExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Dropdown to select which icon's sliders to configure
                        ExposedDropdownMenuBox(
                            expanded = targetDropdownExpanded,
                            onExpandedChange = { targetDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedTarget.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Configuring Icon Sliders For") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (selectedTarget == SliderConfigTarget.ROOM_NUMBER) {
                                            Icons.Default.MeetingRoom
                                        } else {
                                            Icons.Default.Timer
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = targetDropdownExpanded,
                                onDismissRequest = { targetDropdownExpanded = false }
                            ) {
                                SliderConfigTarget.entries.forEach { target ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = target.label,
                                                    fontWeight = if (selectedTarget == target) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = target.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (target == SliderConfigTarget.ROOM_NUMBER) {
                                                    Icons.Default.MeetingRoom
                                                } else {
                                                    Icons.Default.Timer
                                                },
                                                contentDescription = null,
                                                tint = if (selectedTarget == target) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        },
                                        onClick = {
                                            selectedTarget = target
                                            targetDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Compact live preview of the currently selected icon
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentBitmap = if (selectedTarget == SliderConfigTarget.ROOM_NUMBER) {
                                    previewBitmap
                                } else {
                                    previewCountdownBitmap
                                }
                                val targetSummary = if (selectedTarget == SliderConfigTarget.ROOM_NUMBER) {
                                    "Room #${settings.previewRoom}"
                                } else {
                                    "${previewCountdownMinutes}m Left"
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Canvas ($targetSummary)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .border(
                                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = currentBitmap.asImageBitmap(),
                                            contentDescription = "Live Canvas Preview",
                                            modifier = Modifier.size(60.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Status Bar Simulation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0F0F14))
                                            .border(BorderStroke(1.dp, Color(0xFF2E2E38)), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "9:30",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Image(
                                                bitmap = currentBitmap.asImageBitmap(),
                                                contentDescription = "Status Bar Preview",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Interactive Preview Test Input
                        if (selectedTarget == SliderConfigTarget.ROOM_NUMBER) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = settings.previewRoom,
                                    onValueChange = { viewModel.setPreviewRoom(it) },
                                    label = { Text("Preview Room #") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(onClick = { viewModel.resetPreviewRoom() }) {
                                    Text("Reset")
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = previewCountdownMinutes,
                                        onValueChange = { viewModel.setPreviewCountdownMinutes(it) },
                                        label = { Text("Test Minutes Remaining") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(onClick = { viewModel.setPreviewCountdownMinutes("35") }) {
                                        Text("Reset")
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quick presets:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(onClick = { viewModel.setPreviewCountdownMinutes("40") }) {
                                        Text("40m")
                                    }
                                    OutlinedButton(onClick = { viewModel.setPreviewCountdownMinutes("15") }) {
                                        Text("15m")
                                    }
                                    OutlinedButton(onClick = { viewModel.setPreviewCountdownMinutes("5") }) {
                                        Text("5m")
                                    }
                                }
                            }
                        }

                        // Render Sliders according to selected target
                        if (selectedTarget == SliderConfigTarget.ROOM_NUMBER) {
                            ScalingSliderItem(
                                label = "Room Text Size",
                                valueText = String.format(Locale.US, "%.0f px", settings.fontSize),
                                value = settings.fontSize,
                                valueRange = AppPreferences.MIN_FONT_SIZE..AppPreferences.MAX_FONT_SIZE,
                                onValueChange = { viewModel.setFontSize(it) }
                            )

                            ScalingSliderItem(
                                label = "Room Vertical Position (Y Baseline)",
                                valueText = String.format(Locale.US, "%.0f px", settings.yPosition),
                                value = settings.yPosition,
                                valueRange = AppPreferences.MIN_Y_POSITION..AppPreferences.MAX_Y_POSITION,
                                onValueChange = { viewModel.setYPosition(it) }
                            )

                            ScalingSliderItem(
                                label = "Room Width Scale (textScaleX)",
                                valueText = String.format(Locale.US, "%.2fx", settings.textScaleX),
                                value = settings.textScaleX,
                                valueRange = AppPreferences.MIN_TEXT_SCALE_X..AppPreferences.MAX_TEXT_SCALE_X,
                                onValueChange = { viewModel.setTextScaleX(it) }
                            )

                            ScalingSliderItem(
                                label = "Room Boldness / Weight",
                                valueText = String.format(Locale.US, "%.1f", settings.boldness),
                                value = settings.boldness,
                                valueRange = AppPreferences.MIN_BOLDNESS..AppPreferences.MAX_BOLDNESS,
                                onValueChange = { viewModel.setBoldness(it) }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { viewModel.resetRoomDefaults() }) {
                                    Text("Reset Room Sliders")
                                }
                            }
                        } else {
                            ScalingSliderItem(
                                label = "Countdown Text Size",
                                valueText = String.format(Locale.US, "%.0f px", settings.countdownFontSize),
                                value = settings.countdownFontSize,
                                valueRange = AppPreferences.MIN_FONT_SIZE..AppPreferences.MAX_FONT_SIZE,
                                onValueChange = { viewModel.setCountdownFontSize(it) }
                            )

                            ScalingSliderItem(
                                label = "Countdown Vertical Position (Y Baseline)",
                                valueText = String.format(Locale.US, "%.0f px", settings.countdownYPosition),
                                value = settings.countdownYPosition,
                                valueRange = AppPreferences.MIN_Y_POSITION..AppPreferences.MAX_Y_POSITION,
                                onValueChange = { viewModel.setCountdownYPosition(it) }
                            )

                            ScalingSliderItem(
                                label = "Countdown Width Scale (textScaleX)",
                                valueText = String.format(Locale.US, "%.2fx", settings.countdownTextScaleX),
                                value = settings.countdownTextScaleX,
                                valueRange = AppPreferences.MIN_TEXT_SCALE_X..AppPreferences.MAX_TEXT_SCALE_X,
                                onValueChange = { viewModel.setCountdownTextScaleX(it) }
                            )

                            ScalingSliderItem(
                                label = "Countdown Boldness / Weight",
                                valueText = String.format(Locale.US, "%.1f", settings.countdownBoldness),
                                value = settings.countdownBoldness,
                                valueRange = AppPreferences.MIN_BOLDNESS..AppPreferences.MAX_BOLDNESS,
                                onValueChange = { viewModel.setCountdownBoldness(it) }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { viewModel.resetCountdownDefaults() }) {
                                    Text("Reset Countdown Sliders")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Priority Mode Card
        PriorityModeCard(
            currentMode = settings.priorityMode,
            onModeSelected = { viewModel.setPriorityMode(it) }
        )

        // 5. Notification Tap Action Card
        TapActionCard(
            currentAction = settings.tapAction,
            onActionSelected = { viewModel.setTapAction(it) }
        )

        // 6. Launcher Icon Selector Card
        LauncherIconCard(
            currentStyle = settings.launcherIconStyle,
            onStyleSelected = { viewModel.setLauncherIconStyle(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ScalingSliderItem(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PriorityModeCard(
    currentMode: PriorityMode,
    onModeSelected: (PriorityMode) -> Unit
) {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember {
        mutableStateOf(ShadeDetectorService.isAccessibilityServiceEnabled(context))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Priority Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            PriorityMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onModeSelected(mode) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (currentMode == PriorityMode.DYNAMIC && !isAccessibilityEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Accessibility Service Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "To detect shade opening, enable ClassBar Shade Detector in Accessibility settings.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }) {
                            Text("Open Accessibility Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TapActionCard(
    currentAction: TapAction,
    onActionSelected: (TapAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Notification Tap Action",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            TapAction.entries.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onActionSelected(action) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentAction == action,
                        onClick = { onActionSelected(action) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LauncherIconCard(
    currentStyle: LauncherIconStyle,
    onStyleSelected: (LauncherIconStyle) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "App Launcher Icon",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LauncherIconStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStyleSelected(style) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentStyle == style,
                        onClick = { onStyleSelected(style) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
