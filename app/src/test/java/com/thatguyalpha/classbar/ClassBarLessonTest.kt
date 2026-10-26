package com.thatguyalpha.classbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassBarLessonTest {

    private val sampleLessons = listOf(
        Lesson(
            hourNumber = 1,
            fromTime = "08:00",
            toTime = "08:45",
            courseName = "Mathematics",
            shortCourseName = "Math",
            teacherName = "Ivan Ivanov",
            roomName = "101",
            isFreeHour = false,
            isTaken = true,
            group = null
        ),
        Lesson(
            hourNumber = 2,
            fromTime = "09:00",
            toTime = "09:45",
            courseName = "Physics",
            shortCourseName = "Phys",
            teacherName = "Maria Petrova",
            roomName = "202",
            isFreeHour = false,
            isTaken = true,
            group = null
        ),
        Lesson(
            hourNumber = 3,
            fromTime = "10:00",
            toTime = "10:45",
            courseName = "Informatics",
            shortCourseName = "Info",
            teacherName = "Georgi Georgiev",
            roomName = "303",
            isFreeHour = false,
            isTaken = true,
            group = null
        )
    )

    @Test
    fun testFindCurrentOrUpcomingLesson_beforeFirstLesson() {
        val (lesson, isActive) = ShkoloApi.findCurrentOrUpcomingLesson(sampleLessons, "07:30")
        assertNotNull(lesson)
        assertEquals("101", lesson?.roomNumber)
        assertFalse(isActive)
    }

    @Test
    fun testFindCurrentOrUpcomingLesson_duringFirstLesson() {
        val (lesson, isActive) = ShkoloApi.findCurrentOrUpcomingLesson(sampleLessons, "08:15")
        assertNotNull(lesson)
        assertEquals("101", lesson?.roomNumber)
        assertTrue(isActive)
    }

    @Test
    fun testFindCurrentOrUpcomingLesson_duringBreak() {
        val (lesson, isActive) = ShkoloApi.findCurrentOrUpcomingLesson(sampleLessons, "08:50")
        assertNotNull(lesson)
        assertEquals("202", lesson?.roomNumber)
        assertFalse(isActive)
    }

    @Test
    fun testFindCurrentOrUpcomingLesson_duringFinalLesson() {
        val (lesson, isActive) = ShkoloApi.findCurrentOrUpcomingLesson(sampleLessons, "10:30")
        assertNotNull(lesson)
        assertEquals("303", lesson?.roomNumber)
        assertTrue(isActive)
    }

    @Test
    fun testFindCurrentOrUpcomingLesson_afterFinalLessonFinishes() {
        // At 10:45, the final lesson ends
        val (lessonAtEnd, isActiveAtEnd) = ShkoloApi.findCurrentOrUpcomingLesson(sampleLessons, "10:45")
        assertNull("At exact end time of final lesson, it should not be active or upcoming", lessonAtEnd)
        assertFalse(isActiveAtEnd)

        // After final lesson ends
        val (lessonAfter, isActiveAfter) = ShkoloApi.findCurrentOrUpcomingLesson(sampleLessons, "11:00")
        assertNull("After final lesson finishes, no lesson should be active or upcoming", lessonAfter)
        assertFalse(isActiveAfter)
    }

    @Test
    fun testFindUpcomingLesson_returnsNullWhenFinalLessonEnds() {
        assertNotNull(ShkoloApi.findUpcomingLesson(sampleLessons, "07:30"))
        assertNotNull(ShkoloApi.findUpcomingLesson(sampleLessons, "08:15"))
        assertNotNull(ShkoloApi.findUpcomingLesson(sampleLessons, "08:50"))
        assertNotNull(ShkoloApi.findUpcomingLesson(sampleLessons, "10:30"))
        assertNull(ShkoloApi.findUpcomingLesson(sampleLessons, "10:45"))
        assertNull(ShkoloApi.findUpcomingLesson(sampleLessons, "11:00"))
    }

    @Test
    fun testIsDayFinished() {
        assertFalse(ShkoloApi.isDayFinished(emptyList(), "12:00"))
        assertFalse(ShkoloApi.isDayFinished(sampleLessons, "07:00"))
        assertFalse(ShkoloApi.isDayFinished(sampleLessons, "08:30"))
        assertFalse(ShkoloApi.isDayFinished(sampleLessons, "08:50"))
        assertFalse(ShkoloApi.isDayFinished(sampleLessons, "10:44"))
        assertTrue(ShkoloApi.isDayFinished(sampleLessons, "10:45"))
        assertTrue(ShkoloApi.isDayFinished(sampleLessons, "12:00"))
    }

    @Test
    fun testExtractRoomNumber() {
        assertEquals("413", ShkoloApi.extractRoomNumber("413 (Етаж 4)"))
        assertEquals("24", ShkoloApi.extractRoomNumber("Каб. 24"))
        assertEquals("102", ShkoloApi.extractRoomNumber("102"))
        assertEquals("СЗ", ShkoloApi.extractRoomNumber("Спортна зала"))
    }

    @Test
    fun testParseTimeToMinutes() {
        assertEquals(0, ShkoloApi.parseTimeToMinutes("00:00"))
        assertEquals(30, ShkoloApi.parseTimeToMinutes("00:30"))
        assertEquals(480, ShkoloApi.parseTimeToMinutes("08:00"))
        assertEquals(810, ShkoloApi.parseTimeToMinutes("13:30"))
        assertEquals(1100, ShkoloApi.parseTimeToMinutes("18:20"))
        assertEquals(0, ShkoloApi.parseTimeToMinutes("invalid"))
    }

    @Test
    fun testIsBeforeNotificationLeadTime_default30Minutes() {
        // First lesson starts at 08:00. With 30 min lead time, appears at 07:30.
        assertTrue(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "01:00", 30))
        assertTrue(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "07:29", 30))
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "07:30", 30))
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "07:45", 30))
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "08:00", 30))
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "09:00", 30))
    }

    @Test
    fun testIsBeforeNotificationLeadTime_zeroMinutes() {
        // First lesson starts at 08:00. With 0 min lead time, appears exactly at 08:00.
        assertTrue(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "07:59", 0))
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "08:00", 0))
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "08:01", 0))
    }

    @Test
    fun testIsBeforeNotificationLeadTime_max300Minutes() {
        // First lesson starts at 08:00 (480 min). With 300 min (5 hours), appears at 03:00 (180 min).
        assertTrue(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "02:59", 300))
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "03:00", 300))
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(sampleLessons, "07:00", 300))
    }

    @Test
    fun testIsBeforeNotificationLeadTime_emptyLessons() {
        assertFalse(ShkoloApi.isBeforeNotificationLeadTime(emptyList(), "07:00", 30))
    }
}
