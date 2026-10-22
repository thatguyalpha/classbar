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
        ClassBarService.isShadeOpen = false
    }

    @Test
    fun classBarIconGenerator_renders96x96Argb8888() {
        val bitmap = ClassBarIconGenerator.renderRoomBitmap(
            roomText = "307",
            textSize = 54f,
            yPosition = 68f,
            textScaleX = 0.95f,
            boldness = 2.5f
        )
        assertEquals(96, bitmap.width)
        assertEquals(96, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)

        val iconCompat = ClassBarIconGenerator.generateIconCompat("307", appPreferences.loadSettings())
        assertNotNull(iconCompat)
    }

    @Test
    fun shkoloApi_extractRoomNumber() {
        assertEquals("307", ShkoloApi.extractRoomNumber("307 (Етаж 3)"))
        assertEquals("24", ShkoloApi.extractRoomNumber("Каб. 24"))
        assertEquals("102", ShkoloApi.extractRoomNumber("102"))
        assertEquals("СЗ", ShkoloApi.extractRoomNumber("Спортна зала"))
        assertEquals("—", ShkoloApi.extractRoomNumber(""))
    }

    @Test
    fun shkoloApi_extractTeacherInitials() {
        assertEquals("ЗН", ShkoloApi.extractTeacherInitials("Зефир Ников"))
        assertEquals("АС", ShkoloApi.extractTeacherInitials("Анелия Стоянова"))
        assertEquals("JD", ShkoloApi.extractTeacherInitials("John Doe"))
        assertEquals("", ShkoloApi.extractTeacherInitials(""))
    }

    @Test
    fun shkoloApi_findUpcomingLesson() {
        val lessons = listOf(
            Lesson(
                hourNumber = 1,
                fromTime = "12:35",
                toTime = "13:15",
                courseName = "Волейбол",
                shortCourseName = "ФВС",
                teacherName = "Зефир Ников",
                roomName = "Спортна зала",
                isFreeHour = false,
                isTaken = false,
                group = null
            ),
            Lesson(
                hourNumber = 2,
                fromTime = "13:25",
                toTime = "14:05",
                courseName = "Операционни системи",
                shortCourseName = "ОС",
                teacherName = "Анелия Стоянова",
                roomName = "307 (Етаж 3)",
                isFreeHour = false,
                isTaken = false,
                group = null
            )
        )

        // At 12:40, hour 1 is active (toTime 13:15 >= 12:40)
        val active1 = ShkoloApi.findUpcomingLesson(lessons, "12:40")
        assertNotNull(active1)
        assertEquals(1, active1?.hourNumber)
        assertEquals("СЗ", active1?.roomNumber)

        // At 13:20 (during break), hour 2 is next (toTime 14:05 >= 13:20)
        val next2 = ShkoloApi.findUpcomingLesson(lessons, "13:20")
        assertNotNull(next2)
        assertEquals(2, next2?.hourNumber)
        assertEquals("307", next2?.roomNumber)

        // At 15:00 (after school), no more upcoming lessons
        val afterSchool = ShkoloApi.findUpcomingLesson(lessons, "15:00")
        assertNull(afterSchool)
    }

    @Test
    fun shkoloApi_parseLessonsJson() {
        val rawJson = """
            {
              "scheduleHours": [
                {
                  "school_hour": 1,
                  "from_time": "12:35",
                  "to_time": "13:15",
                  "course_name": "Волейбол",
                  "short_course_name": "ФВС",
                  "teacher_name": "Зефир Ников",
                  "room_name": "Спортна зала",
                  "is_free_hour": 0
                },
                {
                  "school_hour": 2,
                  "from_time": "13:25",
                  "to_time": "14:05",
                  "course_name": "Операционни системи",
                  "short_course_name": "ОС",
                  "teacher_name": "Анелия Стоянова",
                  "room_name": "307",
                  "is_free_hour": 0
                }
              ]
            }
        """.trimIndent()

        val parsed = ShkoloApi.parseLessonsJson(rawJson)
        assertEquals(2, parsed.size)
        assertEquals("Волейбол", parsed[0].courseName)
        assertEquals("307", parsed[1].roomNumber)
    }

    @Test
    fun preferencesAndViewModel_manageSettings() {
        val viewModel = MainViewModel(context, appPreferences)

        val initial = viewModel.settings.value
        assertEquals(54f, initial.fontSize, 0.001f)
        assertEquals(68f, initial.yPosition, 0.001f)
        assertEquals(0.95f, initial.textScaleX, 0.001f)
        assertEquals(2.5f, initial.boldness, 0.001f)
        assertEquals("307", initial.previewRoom)

        viewModel.setFontSize(60f)
        viewModel.setYPosition(72f)
        viewModel.setTextScaleX(1.05f)
        viewModel.setBoldness(3.0f)
        viewModel.setPreviewRoom("204")

        val updated = viewModel.settings.value
        assertEquals(60f, updated.fontSize, 0.001f)
        assertEquals(72f, updated.yPosition, 0.001f)
        assertEquals(1.05f, updated.textScaleX, 0.001f)
        assertEquals(3.0f, updated.boldness, 0.001f)
        assertEquals("204", updated.previewRoom)

        viewModel.resetScalingDefaults()
        val reset = viewModel.settings.value
        assertEquals(54f, reset.fontSize, 0.001f)
        assertEquals(68f, reset.yPosition, 0.001f)
        assertEquals(0.95f, reset.textScaleX, 0.001f)
        assertEquals(2.5f, reset.boldness, 0.001f)
        assertEquals("307", reset.previewRoom)
    }

    @Test
    fun classBarService_channelsAndNotifications() {
        val manager = context.getSystemService(NotificationManager::class.java)
        ClassBarService.ensureNotificationChannels(context, manager)

        val highChannel = manager.getNotificationChannel(ClassBarService.CHANNEL_FORCED_HIGH)
        val silentChannel = manager.getNotificationChannel(ClassBarService.CHANNEL_FORCED_SILENT)
        assertNotNull(highChannel)
        assertNotNull(silentChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, highChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_LOW, silentChannel.importance)

        val settings = IconSettings(
            isEnabled = true,
            priorityMode = PriorityMode.FORCED_HIGH,
            tapAction = TapAction.OPEN_APP
        )
        val notification = ClassBarService.buildStatusNotification(context, settings, shadeOpen = false)
        assertEquals(ClassBarService.CHANNEL_FORCED_HIGH, notification.channelId)
        assertEquals("0000", notification.sortKey)
        assertEquals(NotificationCompat.PRIORITY_MAX, notification.priority)
        assertNull(notification.extras.getCharSequence(Notification.EXTRA_TEXT))
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun launcherIconAliases_switchComponentEnabledSettings() {
        appPreferences.setLauncherIconStyle(LauncherIconStyle.STYLE_2)
        val pm = context.packageManager

        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            pm.getComponentEnabledSetting(ComponentName(context.packageName, "${context.packageName}.${LauncherIconStyle.STYLE_1.aliasName}"))
        )
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            pm.getComponentEnabledSetting(ComponentName(context.packageName, "${context.packageName}.${LauncherIconStyle.STYLE_2.aliasName}"))
        )
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            pm.getComponentEnabledSetting(ComponentName(context.packageName, "${context.packageName}.${LauncherIconStyle.STYLE_3.aliasName}"))
        )
    }
}
