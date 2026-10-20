package com.thatguyalpha.classbar

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager

enum class PriorityMode(val label: String, val description: String) {
    FORCED_HIGH(
        label = "Forced high priority",
        description = "Pinned immediately to the right of the status bar clock (sortKey 0000, IMPORTANCE_HIGH, PRIORITY_MAX, silent channel)."
    ),
    FORCED_SILENT(
        label = "Forced silent priority",
        description = "Sinks the notification card to the bottom Silent section of the notification shade (IMPORTANCE_LOW)."
    ),
    DYNAMIC(
        label = "Dynamic",
        description = "Forced high priority while shade is closed; automatically demotes/hides when SystemUI shade opens."
    )
}

enum class TapAction(val label: String, val description: String) {
    OPEN_CALENDAR(
        label = "Open Google Calendar",
        description = "Launches Google Calendar when tapping the notification card."
    ),
    OPEN_APP(
        label = "Open ClassBar",
        description = "Opens ClassBar settings when tapping the notification card."
    ),
    DO_NOTHING(
        label = "Do Nothing",
        description = "Tapping the notification card performs no action."
    )
}

enum class LauncherIconStyle(
    val id: Int,
    val title: String,
    val description: String,
    val aliasClassName: String
) {
    ICON_1(
        id = 1,
        title = "Watch Aperture",
        description = "Minimalist circle with two stacked horizontal pill cutout windows",
        aliasClassName = "com.thatguyalpha.classbar.IconAlias1"
    ),
    ICON_2(
        id = 2,
        title = "Status Bar Pill",
        description = "Minimalist top status bar corner frame showing 12:00 next to a stacked badge",
        aliasClassName = "com.thatguyalpha.classbar.IconAlias2"
    ),
    ICON_3(
        id = 3,
        title = "Typographic Monogram",
        description = "Clean circular badge containing geometric stacked DAY over 14",
        aliasClassName = "com.thatguyalpha.classbar.IconAlias3"
    );

    companion object {
        fun fromId(id: Int): LauncherIconStyle = entries.find { it.id == id } ?: ICON_1
    }
}

data class IconSettings(
    val isEnabled: Boolean = AppPreferences.DEFAULT_ENABLED,
    val line1Size: Float = AppPreferences.DEFAULT_LINE1_SIZE,
    val line2Size: Float = AppPreferences.DEFAULT_LINE2_SIZE,
    val line1Y: Float = AppPreferences.DEFAULT_LINE1_Y,
    val line2Y: Float = AppPreferences.DEFAULT_LINE2_Y,
    val textScaleX: Float = AppPreferences.DEFAULT_TEXT_SCALE_X,
    val boldness: Float = AppPreferences.DEFAULT_BOLDNESS,
    val stressTestWidestDate: Boolean = false,
    val priorityMode: PriorityMode = PriorityMode.FORCED_HIGH,
    val tapAction: TapAction = TapAction.OPEN_CALENDAR,
    val launcherIconStyle: LauncherIconStyle = LauncherIconStyle.ICON_1
)

class AppPreferences(context: Context) {
    private val appContext = context.applicationContext
    val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureCalibratedV2Defaults()
    }

    fun ensureCalibratedV2Defaults() {
        if (!prefs.getBoolean(KEY_CALIBRATED_V2, false)) {
            prefs.edit()
                .putFloat(KEY_LINE1_SIZE, DEFAULT_LINE1_SIZE)
                .putFloat(KEY_LINE2_SIZE, DEFAULT_LINE2_SIZE)
                .putFloat(KEY_LINE1_Y, DEFAULT_LINE1_Y)
                .putFloat(KEY_LINE2_Y, DEFAULT_LINE2_Y)
                .putFloat(KEY_TEXT_SCALE_X, DEFAULT_TEXT_SCALE_X)
                .putFloat(KEY_BOLDNESS, DEFAULT_BOLDNESS)
                .putBoolean(KEY_CALIBRATED_V2, true)
                .apply()
        }
    }

    fun loadSettings(): IconSettings {
        ensureCalibratedV2Defaults()
        val priorityName = prefs.getString(KEY_PRIORITY_MODE, PriorityMode.FORCED_HIGH.name)
            ?: PriorityMode.FORCED_HIGH.name
        val tapActionName = prefs.getString(KEY_TAP_ACTION, TapAction.OPEN_CALENDAR.name)
            ?: TapAction.OPEN_CALENDAR.name
        val iconId = prefs.getInt(KEY_LAUNCHER_ICON, LauncherIconStyle.ICON_1.id)

        return IconSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED),
            line1Size = prefs.getFloat(KEY_LINE1_SIZE, DEFAULT_LINE1_SIZE).coerceIn(MIN_LINE1_SIZE, MAX_LINE1_SIZE),
            line2Size = prefs.getFloat(KEY_LINE2_SIZE, DEFAULT_LINE2_SIZE).coerceIn(MIN_LINE2_SIZE, MAX_LINE2_SIZE),
            line1Y = prefs.getFloat(KEY_LINE1_Y, DEFAULT_LINE1_Y).coerceIn(MIN_LINE1_Y, MAX_LINE1_Y),
            line2Y = prefs.getFloat(KEY_LINE2_Y, DEFAULT_LINE2_Y).coerceIn(MIN_LINE2_Y, MAX_LINE2_Y),
            textScaleX = prefs.getFloat(KEY_TEXT_SCALE_X, DEFAULT_TEXT_SCALE_X).coerceIn(MIN_TEXT_SCALE_X, MAX_TEXT_SCALE_X),
            boldness = prefs.getFloat(KEY_BOLDNESS, DEFAULT_BOLDNESS).coerceIn(MIN_BOLDNESS, MAX_BOLDNESS),
            stressTestWidestDate = prefs.getBoolean(KEY_STRESS_TEST_WIDEST, false),
            priorityMode = runCatching { PriorityMode.valueOf(priorityName) }.getOrDefault(PriorityMode.FORCED_HIGH),
            tapAction = runCatching { TapAction.valueOf(tapActionName) }.getOrDefault(TapAction.OPEN_CALENDAR),
            launcherIconStyle = LauncherIconStyle.fromId(iconId)
        )
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setLine1Size(size: Float) {
        prefs.edit().putFloat(KEY_LINE1_SIZE, size.coerceIn(MIN_LINE1_SIZE, MAX_LINE1_SIZE)).apply()
    }

    fun setLine2Size(size: Float) {
        prefs.edit().putFloat(KEY_LINE2_SIZE, size.coerceIn(MIN_LINE2_SIZE, MAX_LINE2_SIZE)).apply()
    }

    fun setLine1Y(y: Float) {
        prefs.edit().putFloat(KEY_LINE1_Y, y.coerceIn(MIN_LINE1_Y, MAX_LINE1_Y)).apply()
    }

    fun setLine2Y(y: Float) {
        prefs.edit().putFloat(KEY_LINE2_Y, y.coerceIn(MIN_LINE2_Y, MAX_LINE2_Y)).apply()
    }

    fun setTextScaleX(scaleX: Float) {
        prefs.edit().putFloat(KEY_TEXT_SCALE_X, scaleX.coerceIn(MIN_TEXT_SCALE_X, MAX_TEXT_SCALE_X)).apply()
    }

    fun setBoldness(boldness: Float) {
        prefs.edit().putFloat(KEY_BOLDNESS, boldness.coerceIn(MIN_BOLDNESS, MAX_BOLDNESS)).apply()
    }

    fun setStressTestWidestDate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRESS_TEST_WIDEST, enabled).apply()
    }

    fun setPriorityMode(mode: PriorityMode) {
        prefs.edit().putString(KEY_PRIORITY_MODE, mode.name).apply()
    }

    fun setTapAction(action: TapAction) {
        prefs.edit().putString(KEY_TAP_ACTION, action.name).apply()
    }

    fun setLauncherIconStyle(style: LauncherIconStyle) {
        prefs.edit().putInt(KEY_LAUNCHER_ICON, style.id).apply()
        applyLauncherIconAlias(appContext, style)
    }

    fun resetScalingDefaults() {
        prefs.edit()
            .putFloat(KEY_LINE1_SIZE, DEFAULT_LINE1_SIZE)
            .putFloat(KEY_LINE2_SIZE, DEFAULT_LINE2_SIZE)
            .putFloat(KEY_LINE1_Y, DEFAULT_LINE1_Y)
            .putFloat(KEY_LINE2_Y, DEFAULT_LINE2_Y)
            .putFloat(KEY_TEXT_SCALE_X, DEFAULT_TEXT_SCALE_X)
            .putFloat(KEY_BOLDNESS, DEFAULT_BOLDNESS)
            .putBoolean(KEY_CALIBRATED_V2, true)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "day_and_date_prefs"

        const val KEY_CALIBRATED_V2 = "prefs_calibrated_v2"
        const val KEY_ENABLED = "pref_service_enabled"
        const val KEY_LINE1_SIZE = "pref_line1_size"
        const val KEY_LINE2_SIZE = "pref_line2_size"
        const val KEY_LINE1_Y = "pref_line1_y"
        const val KEY_LINE2_Y = "pref_line2_y"
        const val KEY_TEXT_SCALE_X = "pref_text_scale_x"
        const val KEY_BOLDNESS = "pref_boldness"
        const val KEY_STRESS_TEST_WIDEST = "pref_stress_test_widest"
        const val KEY_PRIORITY_MODE = "pref_priority_mode"
        const val KEY_TAP_ACTION = "pref_tap_action"
        const val KEY_LAUNCHER_ICON = "pref_launcher_icon"

        const val DEFAULT_ENABLED = false
        const val DEFAULT_LINE1_SIZE = 46f
        const val MIN_LINE1_SIZE = 20f
        const val MAX_LINE1_SIZE = 52f

        const val DEFAULT_LINE2_SIZE = 46f
        const val MIN_LINE2_SIZE = 18f
        const val MAX_LINE2_SIZE = 48f

        const val DEFAULT_LINE1_Y = 40f
        const val MIN_LINE1_Y = 20f
        const val MAX_LINE1_Y = 56f

        const val DEFAULT_LINE2_Y = 83f
        const val MIN_LINE2_Y = 56f
        const val MAX_LINE2_Y = 94f

        const val DEFAULT_TEXT_SCALE_X = 0.97f
        const val MIN_TEXT_SCALE_X = 0.70f
        const val MAX_TEXT_SCALE_X = 1.10f

        const val DEFAULT_BOLDNESS = 2.5f
        const val MIN_BOLDNESS = 0.0f
        const val MAX_BOLDNESS = 3.5f

        fun applyLauncherIconAlias(context: Context, selectedStyle: LauncherIconStyle) {
            val pm = context.packageManager
            LauncherIconStyle.entries.forEach { style ->
                val state = if (style == selectedStyle) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                val componentName = ComponentName(context.packageName, style.aliasClassName)
                runCatching {
                    pm.setComponentEnabledSetting(
                        componentName,
                        state,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }
    }
}
