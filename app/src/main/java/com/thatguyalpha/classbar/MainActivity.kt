package com.thatguyalpha.classbar

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (viewModel.settings.value.isEnabled) {
            DateNotificationService.startOrRefresh(this)
        }

        setContent {
            ClassBarTheme {
                val settings by viewModel.settings.collectAsState()
                val previewBitmap by viewModel.previewBitmap.collectAsState()

                ClassBarScreen(
                    settings = settings,
                    previewBitmap = previewBitmap,
                    onToggleMaster = { enabled -> viewModel.setServiceEnabled(enabled) },
                    onToggleStressTest = { enabled -> viewModel.setStressTestWidestDate(enabled) },
                    onLine1SizeChange = { viewModel.setLine1Size(it) },
                    onLine2SizeChange = { viewModel.setLine2Size(it) },
                    onLine1YChange = { viewModel.setLine1Y(it) },
                    onLine2YChange = { viewModel.setLine2Y(it) },
                    onTextScaleXChange = { viewModel.setTextScaleX(it) },
                    onBoldnessChange = { viewModel.setBoldness(it) },
                    onResetDefaults = { viewModel.resetScalingDefaults() },
                    onPriorityModeSelected = { viewModel.setPriorityMode(it) },
                    onTapActionSelected = { viewModel.setTapAction(it) },
                    onLauncherIconSelected = { viewModel.setLauncherIconStyle(it) }
                )
            }
        }
    }
}

@Composable
fun ClassBarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassBarScreen(
    settings: IconSettings,
    previewBitmap: Bitmap,
    onToggleMaster: (Boolean) -> Unit,
    onToggleStressTest: (Boolean) -> Unit,
    onLine1SizeChange: (Float) -> Unit,
    onLine2SizeChange: (Float) -> Unit,
    onLine1YChange: (Float) -> Unit,
    onLine2YChange: (Float) -> Unit,
    onTextScaleXChange: (Float) -> Unit,
    onBoldnessChange: (Float) -> Unit,
    onResetDefaults: () -> Unit,
    onPriorityModeSelected: (PriorityMode) -> Unit,
    onTapActionSelected: (TapAction) -> Unit,
    onLauncherIconSelected: (LauncherIconStyle) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAccessibilityEnabled by remember {
        mutableStateOf(ShadeDetectorService.isAccessibilityServiceEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = ShadeDetectorService.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onToggleMaster(true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ClassBar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Status Bar Utility",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Master Enable/Disable Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
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
                                "Active in status bar • Auto-updates at midnight"
                            } else {
                                "Disabled • Turn on to display status bar icon"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { shouldEnable ->
                            if (shouldEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleMaster(true)
                                }
                            } else {
                                onToggleMaster(shouldEnable)
                            }
                        },
                        modifier = Modifier.testTag("master_switch")
                    )
                }
            }

            // 2. Zoomed-in 96x96 Bounding-Box Preview Card + Stress-Test Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "96×96 Canvas Bounding-Box Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (settings.stressTestWidestDate) "Wed / 29" else "Live Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Simulated status bar strip + zoomed 96x96 bounding box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0E1117))
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Zoomed-in 96x96 bounding box with visible border showing exact canvas edges
                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .border(
                                        BorderStroke(1.5.dp, Color(0xFF64B5F6)),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(Color(0xFF161B22))
                                    .testTag("preview_bounding_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Zoomed 96x96 Status Bar Icon Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    filterQuality = FilterQuality.None
                                )
                            }

                            // Simulated 1:1 status bar clock alignment strip
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF1F2631))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "9:30",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Actual Status Bar Scale Preview",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // 3. Stress-Test Widest Date (Wed / 29) toggle switch right below preview box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleStressTest(!settings.stressTestWidestDate) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stress-Test Widest Date (Wed / 29)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Temporarily switches both preview and live status bar icon to \"Wed\" / \"29\" to verify zero clipping",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = settings.stressTestWidestDate,
                            onCheckedChange = onToggleStressTest,
                            modifier = Modifier.testTag("stress_test_switch")
                        )
                    }
                }
            }

            // 4. Fixed Year-Round Scaling Sliders Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fixed Year-Round Scaling",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Exact pixel metrics applied consistently every day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onResetDefaults,
                            modifier = Modifier.testTag("reset_defaults_button")
                        ) {
                            Text("Reset to Defaults")
                        }
                    }

                    HorizontalDivider()

                    // Slider 1: Line 1 Font Size (20f..52f, default 38f)
                    ScalingSliderItem(
                        label = "Line 1 Font Size (Day)",
                        valueText = "${settings.line1Size.roundToInt()} px",
                        value = settings.line1Size,
                        valueRange = AppPreferences.MIN_LINE1_SIZE..AppPreferences.MAX_LINE1_SIZE,
                        onValueChange = onLine1SizeChange,
                        testTag = "slider_line1_size"
                    )

                    // Slider 2: Line 2 Font Size (18f..48f, default 42f)
                    ScalingSliderItem(
                        label = "Line 2 Font Size (Date)",
                        valueText = "${settings.line2Size.roundToInt()} px",
                        value = settings.line2Size,
                        valueRange = AppPreferences.MIN_LINE2_SIZE..AppPreferences.MAX_LINE2_SIZE,
                        onValueChange = onLine2SizeChange,
                        testTag = "slider_line2_size"
                    )

                    // Slider 3a & 3b: Vertical Position & Line Spacing (Top Y default 38f, Bottom Y default 80f)
                    ScalingSliderItem(
                        label = "Vertical Position: Top Line Y Baseline",
                        valueText = "${settings.line1Y.roundToInt()} px",
                        value = settings.line1Y,
                        valueRange = AppPreferences.MIN_LINE1_Y..AppPreferences.MAX_LINE1_Y,
                        onValueChange = onLine1YChange,
                        testTag = "slider_line1_y"
                    )

                    ScalingSliderItem(
                        label = "Vertical Spacing: Bottom Line Y Baseline",
                        valueText = "${settings.line2Y.roundToInt()} px (Gap ${(settings.line2Y - settings.line1Y).roundToInt()} px)",
                        value = settings.line2Y,
                        valueRange = AppPreferences.MIN_LINE2_Y..AppPreferences.MAX_LINE2_Y,
                        onValueChange = onLine2YChange,
                        testTag = "slider_line2_y"
                    )

                    // Slider 4: Horizontal Width Scale (textScaleX, 0.70f..1.10f, default 1.00f)
                    ScalingSliderItem(
                        label = "Horizontal Width Scale (textScaleX)",
                        valueText = String.format(Locale.US, "%.2fx", settings.textScaleX),
                        value = settings.textScaleX,
                        valueRange = AppPreferences.MIN_TEXT_SCALE_X..AppPreferences.MAX_TEXT_SCALE_X,
                        onValueChange = onTextScaleXChange,
                        testTag = "slider_text_scale_x"
                    )

                    // Slider 5: Boldness / Weight (0.0f..3.5f, default 1.2f)
                    ScalingSliderItem(
                        label = "Boldness / Weight",
                        valueText = String.format(Locale.US, "%.1f px", settings.boldness),
                        value = settings.boldness,
                        valueRange = AppPreferences.MIN_BOLDNESS..AppPreferences.MAX_BOLDNESS,
                        onValueChange = onBoldnessChange,
                        testTag = "slider_boldness"
                    )
                }
            }

            // 5. Three Selectable Priority Modes Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Priority Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Controls status bar placement & notification shade behavior",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PriorityMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPriorityModeSelected(mode) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = settings.priorityMode == mode,
                                onClick = { onPriorityModeSelected(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (settings.priorityMode == PriorityMode.DYNAMIC) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isAccessibilityEnabled) {
                                        "ShadeDetectorService is ENABLED in Accessibility Settings."
                                    } else {
                                        "Enable \"ClassBar Shade Detector\" in Accessibility Settings so Dynamic mode can detect when the notification shade opens."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Button(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier.testTag("accessibility_settings_button")
                                ) {
                                    Text("Open Accessibility Settings")
                                }
                            }
                        }
                    }
                }
            }

            // 6. Selectable Notification Tap Action Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Notification Shade Tap Action",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Action when tapping notification in the shade",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TapAction.entries.forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTapActionSelected(action) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.tapAction == action,
                                onClick = { onTapActionSelected(action) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
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

            // 7. Selectable App Launcher Icon (1, 2, or 3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "App Launcher Icon Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Switch the home screen Adaptive Icon via activity-alias",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LauncherIconStyle.entries.forEach { style ->
                        val isSelected = settings.launcherIconStyle == style
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onLauncherIconSelected(style) },
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = if (isSelected) 4.dp else 0.dp,
                            border = if (isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LauncherIconVectorBadge(style = style)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Icon ${style.id}: ${style.title}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = style.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onLauncherIconSelected(style) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScalingSliderItem(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
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
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun LauncherIconVectorBadge(style: LauncherIconStyle) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                when (style) {
                    LauncherIconStyle.ICON_1 -> Color(0xFF161D2B)
                    LauncherIconStyle.ICON_2 -> Color(0xFF112426)
                    LauncherIconStyle.ICON_3 -> Color(0xFF1C192B)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LauncherIconStyle.ICON_1 -> {
                Canvas(modifier = Modifier.size(34.dp)) {
                    drawCircle(color = Color(0xFFD3E3FD), radius = size.minDimension / 2f)
                    drawRoundRect(
                        color = Color(0xFF161D2B),
                        topLeft = Offset(size.width * 0.22f, size.height * 0.28f),
                        size = Size(size.width * 0.56f, size.height * 0.18f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = Color(0xFF161D2B),
                        topLeft = Offset(size.width * 0.16f, size.height * 0.54f),
                        size = Size(size.width * 0.68f, size.height * 0.20f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
            LauncherIconStyle.ICON_2 -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "12:00",
                        color = Color(0xFFE8F5E9),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 3.dp)
                                .background(Color(0xFF6DD58C), RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 10.dp, height = 3.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
            LauncherIconStyle.ICON_3 -> {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFFD0BCFF),
                            style = Stroke(width = 3f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DAY",
                            color = Color(0xFFEADDFF),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 7.sp
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(1.dp)
                                .background(Color(0xFFD0BCFF))
                        )
                        Text(
                            text = "14",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 9.sp
                        )
                    }
                }
            }
        }
    }
}
