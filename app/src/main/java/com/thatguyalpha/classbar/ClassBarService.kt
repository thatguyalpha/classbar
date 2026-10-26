package com.thatguyalpha.classbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClassBarService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var appPreferences: AppPreferences
    private lateinit var notificationManager: NotificationManager
    private var receiverRegistered = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ctx = context ?: return
            refreshNotification(ctx)
        }
    }

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        appPreferences.prefs.registerOnSharedPreferenceChangeListener(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timeTickReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(timeTickReceiver, filter)
        }
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = appPreferences.loadSettings()
        if (!settings.isEnabled || !appPreferences.isLoggedIn()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(NOTIFICATION_ID)
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationManager.cancel(NOTIFICATION_ID)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHADE_STATE_CHANGED -> {
                val shadeOpen = intent.getBooleanExtra(EXTRA_SHADE_OPEN, false)
                onShadeVisibilityChanged(this, shadeOpen)
            }
            else -> {
                ensureNotificationChannels(this, notificationManager)
                val notification = buildStatusNotification(this, settings, isShadeOpen)
                val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                if (notification != null) {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
                } else {
                    val fallback = NotificationCompat.Builder(this, CHANNEL_FORCED_SILENT)
                        .setSmallIcon(R.mipmap.ic_launcher_1)
                        .setContentTitle("ClassBar")
                        .build()
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, fallback, foregroundType)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notificationManager.cancel(NOTIFICATION_ID)
                }
                triggerBackgroundScheduleRefreshIfNeeded()
            }
        }

        return START_STICKY
    }

    private fun triggerBackgroundScheduleRefreshIfNeeded() {
        val token = appPreferences.getSavedToken() ?: return
        val pupilId = appPreferences.getSavedPupilId()
        if (pupilId <= 0L) return

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        serviceScope.launch {
            val result = ShkoloApi.getSchedule(token, pupilId, todayStr, appPreferences)
            result.onSuccess { lessons ->
                refreshNotification(this@ClassBarService)
            }.onFailure { err ->
                if (err is UnauthorizedException) {
                    appPreferences.clearSession()
                    stopSelf()
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        refreshNotification(this)
    }

    private fun refreshNotification(context: Context) {
        val settings = appPreferences.loadSettings()
        if (!settings.isEnabled || !appPreferences.isLoggedIn()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(NOTIFICATION_ID)
            stopSelf()
            return
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (appPreferences.loadScheduleCache(todayStr).isNullOrBlank()) {
            triggerBackgroundScheduleRefreshIfNeeded()
        }

        val notification = buildStatusNotification(context, settings, isShadeOpen)
        if (notification == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(timeTickReceiver) }
            receiverRegistered = false
        }
        appPreferences.prefs.unregisterOnSharedPreferenceChangeListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_FORCED_HIGH = "channel_forced_max_v3"
        const val CHANNEL_FORCED_SILENT = "channel_forced_silent"

        const val FUTURE_WHEN_OFFSET_MS = 31_536_000_000L

        const val ACTION_START_OR_REFRESH = "com.thatguyalpha.classbar.ACTION_START_OR_REFRESH"
        const val ACTION_STOP = "com.thatguyalpha.classbar.ACTION_STOP"
        const val ACTION_SHADE_STATE_CHANGED = "com.thatguyalpha.classbar.ACTION_SHADE_STATE_CHANGED"
        const val EXTRA_SHADE_OPEN = "extra_shade_open"

        @Volatile
        var isShadeOpen: Boolean = false

        fun startOrRefresh(context: Context) {
            val intent = Intent(context, ClassBarService::class.java).apply {
                action = ACTION_START_OR_REFRESH
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ClassBarService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.cancel(NOTIFICATION_ID)
        }

        fun ensureNotificationChannels(context: Context, manager: NotificationManager) {
            val highChannel = NotificationChannel(
                CHANNEL_FORCED_HIGH,
                context.getString(R.string.channel_high_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_high_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(highChannel)

            val silentChannel = NotificationChannel(
                CHANNEL_FORCED_SILENT,
                context.getString(R.string.channel_silent_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_silent_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(silentChannel)
        }

        fun resolveEffectiveHighPriority(priorityMode: PriorityMode, shadeOpen: Boolean): Boolean {
            return when (priorityMode) {
                PriorityMode.FORCED_HIGH -> true
                PriorityMode.FORCED_SILENT -> false
                PriorityMode.DYNAMIC -> !shadeOpen
            }
        }

        data class StatusNotificationInfo(
            val iconText: String,
            val title: String,
            val isCountdown: Boolean
        )

        /**
         * Resolves the icon text (room number or countdown minutes), title, and countdown flag.
         * Returns null if all lessons for today have finished or if it is before the appearance lead time.
         */
        fun resolveUpcomingLessonIconAndTitle(context: Context, settings: IconSettings): StatusNotificationInfo? {
            val prefs = AppPreferences(context)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val cachedJson = prefs.loadScheduleCache(todayStr)

            if (!cachedJson.isNullOrBlank()) {
                val lessons = ShkoloApi.parseLessonsJson(cachedJson)
                if (lessons.isEmpty()) {
                    return null
                }
                val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())

                // Check if it is too early in the day (before lead time)
                if (ShkoloApi.isBeforeNotificationLeadTime(lessons, nowTime, settings.notificationLeadTimeMinutes)) {
                    return null
                }

                // Check if all lessons for the day have finished
                if (ShkoloApi.isDayFinished(lessons, nowTime)) {
                    return null
                }

                val (lesson, isActiveNow) = ShkoloApi.findCurrentOrUpcomingLesson(lessons, nowTime)

                if (lesson != null) {
                    val initials = ShkoloApi.extractTeacherInitials(lesson.teacherName)
                    val title = if (initials.isNotBlank()) {
                        "${lesson.roomNumber} • ${lesson.courseName} • $initials"
                    } else {
                        "${lesson.roomNumber} • ${lesson.courseName}"
                    }

                    val isCountdown = isActiveNow && settings.countdownEnabled
                    val iconText = if (isCountdown) {
                        val remainingMinutes = ShkoloApi.computeMinutesRemaining(lesson.toTime, nowTime)
                        remainingMinutes.toString()
                    } else {
                        lesson.roomNumber
                    }

                    return StatusNotificationInfo(iconText, title, isCountdown)
                } else {
                    return null
                }
            }

            // Fallback: if preview room is configured or no schedule
            val preview = settings.previewRoom
            return StatusNotificationInfo(preview, "ClassBar", false)
        }

        fun buildStatusNotification(
            context: Context,
            settings: IconSettings,
            shadeOpen: Boolean
        ): Notification? {
            val notifInfo = resolveUpcomingLessonIconAndTitle(context, settings) ?: return null

            val effectiveHigh = resolveEffectiveHighPriority(settings.priorityMode, shadeOpen)
            val targetChannel = if (effectiveHigh) CHANNEL_FORCED_HIGH else CHANNEL_FORCED_SILENT
            val priorityInt = if (effectiveHigh) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_MIN

            val iconCompat = ClassBarIconGenerator.generateIconCompat(notifInfo.iconText, settings, notifInfo.isCountdown)
            val contentIntent = createContentPendingIntent(context, settings.tapAction)

            return NotificationCompat.Builder(context, targetChannel)
                .setSmallIcon(iconCompat)
                .setContentTitle(notifInfo.title)
                .setContentText(null)
                .setSubText(null)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(priorityInt)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis() + FUTURE_WHEN_OFFSET_MS)
                .setShowWhen(false)
                .setSortKey("0000")
                .setContentIntent(contentIntent)
                .build()
        }

        private fun createContentPendingIntent(context: Context, tapAction: TapAction): PendingIntent? {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return when (tapAction) {
                TapAction.OPEN_APP -> {
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    PendingIntent.getActivity(context, 102, appIntent, flags)
                }
                TapAction.DO_NOTHING -> null
            }
        }

        fun onShadeVisibilityChanged(context: Context, shadeOpen: Boolean) {
            if (isShadeOpen == shadeOpen) return
            isShadeOpen = shadeOpen
            val settings = AppPreferences(context).loadSettings()
            if (!settings.isEnabled || settings.priorityMode != PriorityMode.DYNAMIC) return

            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            ensureNotificationChannels(context, manager)
            val notification = buildStatusNotification(context, settings, shadeOpen)
            if (notification != null) {
                manager.notify(NOTIFICATION_ID, notification)
            } else {
                manager.cancel(NOTIFICATION_ID)
            }
        }

        fun onExternalNotificationPosted(context: Context) {
            if (isShadeOpen) return
            val settings = AppPreferences(context).loadSettings()
            if (!settings.isEnabled) return
            if (!resolveEffectiveHighPriority(settings.priorityMode, isShadeOpen)) return

            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            ensureNotificationChannels(context, manager)
            val notification = buildStatusNotification(context, settings, isShadeOpen)
            if (notification != null) {
                manager.notify(NOTIFICATION_ID, notification)
            } else {
                manager.cancel(NOTIFICATION_ID)
            }
        }
    }
}
