package com.thatguyalpha.classbar

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class ClassBarTest {

    private lateinit var context: Context
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        appPreferences = AppPreferences(context)
        DateNotificationService.isShadeOpen = false
    }

    @Test
    fun dateIconGenerator_renders96x96Argb8888AndFormatsDayNumberOnly() {
        val calSingle = Calendar.getInstance(TimeZone.getDefault(), Locale.US).apply {
            set(2026, Calendar.OCTOBER, 4, 12, 0, 0)
        }
        assertEquals("Sun", DateIconGenerator.formatDayOfWeek(calSingle.time))
        assertEquals("4", DateIconGenerator.formatMonthAndDay(calSingle.time))

        val calDouble = Calendar.getInstance(TimeZone.getDefault(), Locale.US).apply {
            set(2026, Calendar.OCTOBER, 14, 12, 0, 0)
        }
        assertEquals("Wed", DateIconGenerator.formatDayOfWeek(calDouble.time))
        assertEquals("14", DateIconGenerator.formatMonthAndDay(calDouble.time))

        assertEquals("Wed", DateIconGenerator.STRESS_TEST_DAY)
        assertEquals("29", DateIconGenerator.STRESS_TEST_DATE)

        val bitmap = DateIconGenerator.generateBitmap(context, calDouble.time)
        assertEquals(96, bitmap.width)
        assertEquals(96, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)

        val iconCompat = DateIconGenerator.generateIconCompat(context, calDouble.time)
        assertNotNull(iconCompat)
    }

    @Test
    fun preferencesAndViewModel_applyCalibratedV2DefaultsAndOneTimeMigration() {
        // Simulate legacy pre-v2 preferences without prefs_calibrated_v2 flag
        val rawPrefs = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        rawPrefs.edit()
            .putFloat(AppPreferences.KEY_LINE1_SIZE, 36f)
            .putFloat(AppPreferences.KEY_LINE2_SIZE, 30f)
            .putFloat(AppPreferences.KEY_LINE1_Y, 38f)
            .putFloat(AppPreferences.KEY_LINE2_Y, 80f)
            .putFloat(AppPreferences.KEY_TEXT_SCALE_X, 0.90f)
            .putFloat(AppPreferences.KEY_BOLDNESS, 1.2f)
            .putBoolean(AppPreferences.KEY_CALIBRATED_V2, false)
            .commit()

        val migratedPreferences = AppPreferences(context)
        val viewModel = MainViewModel(context, migratedPreferences)

        val initial = viewModel.settings.value
        assertTrue(rawPrefs.getBoolean(AppPreferences.KEY_CALIBRATED_V2, false))
        assertEquals(46f, initial.line1Size, 0.001f)
        assertEquals(46f, initial.line2Size, 0.001f)
        assertEquals(40f, initial.line1Y, 0.001f)
        assertEquals(83f, initial.line2Y, 0.001f)
        assertEquals(0.97f, initial.textScaleX, 0.001f)
        assertEquals(2.5f, initial.boldness, 0.001f)
        assertFalse(initial.stressTestWidestDate)

        viewModel.setLine1Size(48f)
        viewModel.setLine2Size(44f)
        viewModel.setLine1Y(42f)
        viewModel.setLine2Y(85f)
        viewModel.setTextScaleX(1.02f)
        viewModel.setBoldness(3.0f)
        viewModel.setStressTestWidestDate(true)

        val updated = viewModel.settings.value
        assertEquals(48f, updated.line1Size, 0.001f)
        assertEquals(44f, updated.line2Size, 0.001f)
        assertEquals(42f, updated.line1Y, 0.001f)
        assertEquals(85f, updated.line2Y, 0.001f)
        assertEquals(1.02f, updated.textScaleX, 0.001f)
        assertEquals(3.0f, updated.boldness, 0.001f)
        assertTrue(updated.stressTestWidestDate)

        viewModel.resetScalingDefaults()
        val reset = viewModel.settings.value
        assertEquals(46f, reset.line1Size, 0.001f)
        assertEquals(46f, reset.line2Size, 0.001f)
        assertEquals(40f, reset.line1Y, 0.001f)
        assertEquals(83f, reset.line2Y, 0.001f)
        assertEquals(0.97f, reset.textScaleX, 0.001f)
        assertEquals(2.5f, reset.boldness, 0.001f)
    }

    @Test
    fun notificationService_usesChannelForcedMaxV3AndFutureWhenInHighPriority() {
        val manager = context.getSystemService(NotificationManager::class.java)
        DateNotificationService.ensureNotificationChannels(context, manager)

        assertNull(manager.getNotificationChannel("channel_forced_high"))
        val highChannel = manager.getNotificationChannel("channel_forced_max_v3")
        val silentChannel = manager.getNotificationChannel(DateNotificationService.CHANNEL_FORCED_SILENT)
        assertNotNull(highChannel)
        assertNotNull(silentChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, highChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_LOW, silentChannel.importance)
        assertNull(highChannel.sound)
        assertFalse(highChannel.shouldVibrate())
        assertFalse(highChannel.canShowBadge())
        assertTrue(highChannel.canBypassDnd())
        assertTrue(silentChannel.canBypassDnd())
        assertEquals(Notification.VISIBILITY_SECRET, highChannel.lockscreenVisibility)
        assertEquals(Notification.VISIBILITY_SECRET, silentChannel.lockscreenVisibility)

        val fixedNow = 1_700_000_000_000L
        val highSettings = IconSettings(
            isEnabled = true,
            priorityMode = PriorityMode.FORCED_HIGH,
            tapAction = TapAction.DO_NOTHING
        )
        val highNotification = DateNotificationService.buildStatusNotification(
            context = context,
            settings = highSettings,
            shadeOpen = false,
            nowMillis = fixedNow
        )
        assertEquals("channel_forced_max_v3", highNotification.channelId)
        assertEquals("0000", highNotification.sortKey)
        assertEquals(NotificationCompat.PRIORITY_MAX, highNotification.priority)
        assertEquals(fixedNow + 31_536_000_000L, highNotification.`when`)
        assertFalse(highNotification.extras.getBoolean(Notification.EXTRA_SHOW_WHEN, true))
        assertEquals(Notification.VISIBILITY_SECRET, highNotification.visibility)
        assertEquals("Day & date icon", highNotification.extras.getString(Notification.EXTRA_TITLE))
        assertNull(highNotification.extras.getCharSequence(Notification.EXTRA_TEXT))
        assertNull(highNotification.contentIntent)

        val dynamicSettings = IconSettings(
            isEnabled = true,
            priorityMode = PriorityMode.DYNAMIC,
            tapAction = TapAction.OPEN_APP
        )
        val closedShadeNotif = DateNotificationService.buildStatusNotification(
            context = context,
            settings = dynamicSettings,
            shadeOpen = false,
            nowMillis = fixedNow
        )
        assertEquals("channel_forced_max_v3", closedShadeNotif.channelId)
        assertEquals("0000", closedShadeNotif.sortKey)
        assertEquals(NotificationCompat.PRIORITY_MAX, closedShadeNotif.priority)
        assertEquals(fixedNow + 31_536_000_000L, closedShadeNotif.`when`)
        assertNotNull(closedShadeNotif.contentIntent)

        val openShadeNotif = DateNotificationService.buildStatusNotification(
            context = context,
            settings = dynamicSettings,
            shadeOpen = true,
            nowMillis = fixedNow
        )
        assertEquals(DateNotificationService.CHANNEL_FORCED_SILENT, openShadeNotif.channelId)
        assertEquals("zzz", openShadeNotif.sortKey)
        assertEquals(NotificationCompat.PRIORITY_LOW, openShadeNotif.priority)
    }

    @Test
    fun launcherIconAliases_switchComponentEnabledSettingsAndMidnightCalculationIs000001() {
        appPreferences.setLauncherIconStyle(LauncherIconStyle.ICON_2)
        val pm = context.packageManager

        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            pm.getComponentEnabledSetting(ComponentName(context.packageName, LauncherIconStyle.ICON_1.aliasClassName))
        )
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            pm.getComponentEnabledSetting(ComponentName(context.packageName, LauncherIconStyle.ICON_2.aliasClassName))
        )
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            pm.getComponentEnabledSetting(ComponentName(context.packageName, LauncherIconStyle.ICON_3.aliasClassName))
        )

        val triggerMillis = DateNotificationService.computeNextMidnightTriggerMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(1, cal.get(Calendar.SECOND))
    }
}
