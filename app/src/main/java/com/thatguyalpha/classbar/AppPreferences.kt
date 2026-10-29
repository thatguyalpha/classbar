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
    OPEN_APP(
        label = "Open ClassBar",
        description = "Launches ClassBar to view the full timetable when tapped."
    ),
    DO_NOTHING(
        label = "Do nothing",
        description = "Notification has no tap action."
    )
}

enum class LauncherIconStyle(val id: Int, val aliasName: String, val label: String) {
    STYLE_1(1, "MainActivityAlias1", "Style 1 (Standard)"),
    STYLE_2(2, "MainActivityAlias2", "Style 2 (Classic)"),
    STYLE_3(3, "MainActivityAlias3", "Style 3 (Vibrant)");

    companion object {
        fun fromId(id: Int): LauncherIconStyle = entries.firstOrNull { it.id == id } ?: STYLE_1
    }
}

data class IconSettings(
    val isEnabled: Boolean = true,
    // Room number icon configuration
    val fontSize: Float = AppPreferences.DEFAULT_FONT_SIZE,
    val yPosition: Float = AppPreferences.DEFAULT_Y_POSITION,
    val textScaleX: Float = AppPreferences.DEFAULT_TEXT_SCALE_X,
    val boldness: Float = AppPreferences.DEFAULT_BOLDNESS,
    val autoSize: Boolean = false,
    // Minutes left countdown icon configuration
    val countdownFontSize: Float = AppPreferences.DEFAULT_FONT_SIZE,
    val countdownYPosition: Float = AppPreferences.DEFAULT_Y_POSITION,
    val countdownTextScaleX: Float = AppPreferences.DEFAULT_TEXT_SCALE_X,
    val countdownBoldness: Float = AppPreferences.DEFAULT_BOLDNESS,
    val countdownAutoSize: Boolean = false,
    val priorityMode: PriorityMode = PriorityMode.FORCED_HIGH,
    val tapAction: TapAction = TapAction.OPEN_APP,
    val launcherIconStyle: LauncherIconStyle = LauncherIconStyle.STYLE_1,
    val previewRoom: String = AppPreferences.DEFAULT_PREVIEW_ROOM,
    val countdownEnabled: Boolean = false,
    val notificationLeadTimeMinutes: Int = AppPreferences.DEFAULT_NOTIFICATION_LEAD_TIME_MINUTES
)

class AppPreferences(private val context: Context) {

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): IconSettings {
        val priorityName = prefs.getString(KEY_PRIORITY_MODE, PriorityMode.FORCED_HIGH.name)
            ?: PriorityMode.FORCED_HIGH.name
        val tapActionName = prefs.getString(KEY_TAP_ACTION, TapAction.OPEN_APP.name)
            ?: TapAction.OPEN_APP.name
        val iconId = prefs.getInt(KEY_LAUNCHER_ICON_STYLE, LauncherIconStyle.STYLE_1.id)

        return IconSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, true),
            fontSize = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE),
            yPosition = prefs.getFloat(KEY_Y_POSITION, DEFAULT_Y_POSITION),
            textScaleX = prefs.getFloat(KEY_TEXT_SCALE_X, DEFAULT_TEXT_SCALE_X),
            boldness = prefs.getFloat(KEY_BOLDNESS, DEFAULT_BOLDNESS),
            autoSize = prefs.getBoolean(KEY_AUTO_SIZE, false),
            countdownFontSize = prefs.getFloat(KEY_COUNTDOWN_FONT_SIZE, DEFAULT_FONT_SIZE),
            countdownYPosition = prefs.getFloat(KEY_COUNTDOWN_Y_POSITION, DEFAULT_Y_POSITION),
            countdownTextScaleX = prefs.getFloat(KEY_COUNTDOWN_TEXT_SCALE_X, DEFAULT_TEXT_SCALE_X),
            countdownBoldness = prefs.getFloat(KEY_COUNTDOWN_BOLDNESS, DEFAULT_BOLDNESS),
            countdownAutoSize = prefs.getBoolean(KEY_COUNTDOWN_AUTO_SIZE, false),
            priorityMode = runCatching { PriorityMode.valueOf(priorityName) }.getOrDefault(PriorityMode.FORCED_HIGH),
            tapAction = runCatching { TapAction.valueOf(tapActionName) }.getOrDefault(TapAction.OPEN_APP),
            launcherIconStyle = LauncherIconStyle.fromId(iconId),
            previewRoom = prefs.getString(KEY_PREVIEW_ROOM, DEFAULT_PREVIEW_ROOM) ?: DEFAULT_PREVIEW_ROOM,
            countdownEnabled = prefs.getBoolean(KEY_COUNTDOWN_ENABLED, false),
            notificationLeadTimeMinutes = prefs.getInt(
                KEY_NOTIFICATION_LEAD_TIME_MINUTES,
                DEFAULT_NOTIFICATION_LEAD_TIME_MINUTES
            ).coerceIn(MIN_NOTIFICATION_LEAD_TIME_MINUTES, MAX_NOTIFICATION_LEAD_TIME_MINUTES)
        )
    }

    fun setNotificationLeadTimeMinutes(minutes: Int) {
        prefs.edit().putInt(
            KEY_NOTIFICATION_LEAD_TIME_MINUTES,
            minutes.coerceIn(MIN_NOTIFICATION_LEAD_TIME_MINUTES, MAX_NOTIFICATION_LEAD_TIME_MINUTES)
        ).apply()
    }

    fun setCountdownEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COUNTDOWN_ENABLED, enabled).apply()
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setFontSize(size: Float) {
        prefs.edit().putFloat(KEY_FONT_SIZE, size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)).apply()
    }

    fun setYPosition(y: Float) {
        prefs.edit().putFloat(KEY_Y_POSITION, y.coerceIn(MIN_Y_POSITION, MAX_Y_POSITION)).apply()
    }

    fun setTextScaleX(scale: Float) {
        prefs.edit().putFloat(KEY_TEXT_SCALE_X, scale.coerceIn(MIN_TEXT_SCALE_X, MAX_TEXT_SCALE_X)).apply()
    }

    fun setBoldness(boldness: Float) {
        prefs.edit().putFloat(KEY_BOLDNESS, boldness.coerceIn(MIN_BOLDNESS, MAX_BOLDNESS)).apply()
    }

    fun setAutoSize(autoSize: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SIZE, autoSize).apply()
    }

    fun setCountdownFontSize(size: Float) {
        prefs.edit().putFloat(KEY_COUNTDOWN_FONT_SIZE, size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)).apply()
    }

    fun setCountdownYPosition(y: Float) {
        prefs.edit().putFloat(KEY_COUNTDOWN_Y_POSITION, y.coerceIn(MIN_Y_POSITION, MAX_Y_POSITION)).apply()
    }

    fun setCountdownTextScaleX(scale: Float) {
        prefs.edit().putFloat(KEY_COUNTDOWN_TEXT_SCALE_X, scale.coerceIn(MIN_TEXT_SCALE_X, MAX_TEXT_SCALE_X)).apply()
    }

    fun setCountdownBoldness(boldness: Float) {
        prefs.edit().putFloat(KEY_COUNTDOWN_BOLDNESS, boldness.coerceIn(MIN_BOLDNESS, MAX_BOLDNESS)).apply()
    }

    fun setCountdownAutoSize(autoSize: Boolean) {
        prefs.edit().putBoolean(KEY_COUNTDOWN_AUTO_SIZE, autoSize).apply()
    }

    fun setPreviewRoom(room: String) {
        prefs.edit().putString(KEY_PREVIEW_ROOM, room.trim().take(6)).apply()
    }

    fun setPriorityMode(mode: PriorityMode) {
        prefs.edit().putString(KEY_PRIORITY_MODE, mode.name).apply()
    }

    fun setTapAction(action: TapAction) {
        prefs.edit().putString(KEY_TAP_ACTION, action.name).apply()
    }

    fun resetRoomDefaults() {
        prefs.edit()
            .putFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
            .putFloat(KEY_Y_POSITION, DEFAULT_Y_POSITION)
            .putFloat(KEY_TEXT_SCALE_X, DEFAULT_TEXT_SCALE_X)
            .putFloat(KEY_BOLDNESS, DEFAULT_BOLDNESS)
            .putBoolean(KEY_AUTO_SIZE, false)
            .apply()
    }

    fun resetCountdownDefaults() {
        prefs.edit()
            .putFloat(KEY_COUNTDOWN_FONT_SIZE, DEFAULT_FONT_SIZE)
            .putFloat(KEY_COUNTDOWN_Y_POSITION, DEFAULT_Y_POSITION)
            .putFloat(KEY_COUNTDOWN_TEXT_SCALE_X, DEFAULT_TEXT_SCALE_X)
            .putFloat(KEY_COUNTDOWN_BOLDNESS, DEFAULT_BOLDNESS)
            .putBoolean(KEY_COUNTDOWN_AUTO_SIZE, false)
            .apply()
    }

    fun resetDefaults() {
        resetRoomDefaults()
        resetCountdownDefaults()
        prefs.edit()
            .putString(KEY_PREVIEW_ROOM, DEFAULT_PREVIEW_ROOM)
            .apply()
    }

    fun setLauncherIconStyle(style: LauncherIconStyle) {
        prefs.edit().putInt(KEY_LAUNCHER_ICON_STYLE, style.id).apply()
        applyLauncherIcon(style)
    }

    fun applyLauncherIcon(activeStyle: LauncherIconStyle) {
        val pm = context.packageManager
        val pkg = context.packageName

        LauncherIconStyle.entries.forEach { style ->
            val comp = ComponentName(pkg, "$pkg.${style.aliasName}")
            val newState = if (style == activeStyle) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                pm.setComponentEnabledSetting(comp, newState, PackageManager.DONT_KILL_APP)
            }
        }
    }

    // --- Shkolo Session Persistence ---

    fun saveSession(token: String, pupilId: Long, pupilName: String, className: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_PUPIL_ID, pupilId)
            .putString(KEY_PUPIL_NAME, pupilName)
            .putString(KEY_CLASS_NAME, className)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_PUPIL_ID)
            .remove(KEY_PUPIL_NAME)
            .remove(KEY_CLASS_NAME)
            .apply()
    }

    fun isLoggedIn(): Boolean = !prefs.getString(KEY_TOKEN, null).isNullOrBlank()

    fun getSavedToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getSavedPupilId(): Long = prefs.getLong(KEY_PUPIL_ID, 0L)

    fun getSavedPupilName(): String = prefs.getString(KEY_PUPIL_NAME, "") ?: ""

    fun getSavedClassName(): String = prefs.getString(KEY_CLASS_NAME, "") ?: ""

    fun saveScheduleCache(date: String, json: String) {
        prefs.edit().putString(KEY_SCHEDULE_CACHE_PREFIX + date, json).apply()
    }

    fun loadScheduleCache(date: String): String? {
        return prefs.getString(KEY_SCHEDULE_CACHE_PREFIX + date, null)
    }

    companion object {
        const val PREFS_NAME = "classbar_prefs"

        const val KEY_ENABLED = "key_enabled"
        const val KEY_FONT_SIZE = "key_font_size"
        const val KEY_Y_POSITION = "key_y_position"
        const val KEY_TEXT_SCALE_X = "key_text_scale_x"
        const val KEY_BOLDNESS = "key_boldness"
        const val KEY_AUTO_SIZE = "key_auto_size"
        const val KEY_PRIORITY_MODE = "key_priority_mode"
        const val KEY_TAP_ACTION = "key_tap_action"
        const val KEY_LAUNCHER_ICON_STYLE = "key_launcher_icon_style"
        const val KEY_PREVIEW_ROOM = "key_preview_room"
        const val KEY_COUNTDOWN_ENABLED = "key_countdown_enabled"

        const val KEY_COUNTDOWN_FONT_SIZE = "key_countdown_font_size"
        const val KEY_COUNTDOWN_Y_POSITION = "key_countdown_y_position"
        const val KEY_COUNTDOWN_TEXT_SCALE_X = "key_countdown_text_scale_x"
        const val KEY_COUNTDOWN_BOLDNESS = "key_countdown_boldness"
        const val KEY_COUNTDOWN_AUTO_SIZE = "key_countdown_auto_size"
        const val KEY_NOTIFICATION_LEAD_TIME_MINUTES = "key_notification_lead_time_minutes"

        const val DEFAULT_NOTIFICATION_LEAD_TIME_MINUTES = 30
        const val MIN_NOTIFICATION_LEAD_TIME_MINUTES = 0
        const val MAX_NOTIFICATION_LEAD_TIME_MINUTES = 300

        const val KEY_TOKEN = "key_token"
        const val KEY_PUPIL_ID = "key_pupil_id"
        const val KEY_PUPIL_NAME = "key_pupil_name"
        const val KEY_CLASS_NAME = "key_class_name"
        const val KEY_SCHEDULE_CACHE_PREFIX = "schedule_cache_"

        // Sliders config
        const val DEFAULT_FONT_SIZE = 54f
        const val MIN_FONT_SIZE = 24f
        const val MAX_FONT_SIZE = 72f

        const val DEFAULT_Y_POSITION = 68f
        const val MIN_Y_POSITION = 20f
        const val MAX_Y_POSITION = 90f

        const val DEFAULT_TEXT_SCALE_X = 0.95f
        const val MIN_TEXT_SCALE_X = 0.70f
        const val MAX_TEXT_SCALE_X = 1.15f

        const val DEFAULT_BOLDNESS = 2.5f
        const val MIN_BOLDNESS = 0.0f
        const val MAX_BOLDNESS = 3.5f

        const val DEFAULT_PREVIEW_ROOM = "307"
    }
}
