package com.thatguyalpha.classbar

import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClassBarIconGeneratorTest {

    @Test
    fun testCalculateAutosizedFontSize_proportionalScaling() {
        val typeface = Typeface.DEFAULT_BOLD
        val textScaleX = 0.94f
        val boldness = 1.4f

        val size3Digits = ClassBarIconGenerator.calculateAutosizedFontSize("413", typeface, textScaleX, boldness)
        val size2Digits = ClassBarIconGenerator.calculateAutosizedFontSize("24", typeface, textScaleX, boldness)
        val size1Digit = ClassBarIconGenerator.calculateAutosizedFontSize("4", typeface, textScaleX, boldness)

        // Shorter strings should size up larger than longer strings to touch edges
        assertTrue("2-digit size ($size2Digits) should be >= 3-digit size ($size3Digits)", size2Digits >= size3Digits)
        assertTrue("1-digit size ($size1Digit) should be >= 2-digit size ($size2Digits)", size1Digit >= size2Digits)

        // Sizes should be within valid bounds
        assertTrue("Size should be >= MIN_FONT_SIZE", size3Digits >= AppPreferences.MIN_FONT_SIZE)
        assertTrue("Size should be <= 120f", size1Digit <= 120f)
    }

    @Test
    fun testCalculateAutosizedFontSize_blankReturnsDefault() {
        val typeface = Typeface.DEFAULT
        val size = ClassBarIconGenerator.calculateAutosizedFontSize("", typeface, 1f, 1f)
        assertEquals(AppPreferences.DEFAULT_FONT_SIZE, size, 0.01f)
    }

    @Test
    fun testGenerateBitmapForRoom_withAndWithoutAutoSize() {
        val defaultSettings = IconSettings(
            fontSize = 54f,
            autoSize = false
        )
        val bitmapManual = ClassBarIconGenerator.generateBitmapForRoom("413", defaultSettings)
        assertNotNull(bitmapManual)
        assertEquals(96, bitmapManual.width)
        assertEquals(96, bitmapManual.height)

        val autoSizeSettings = IconSettings(
            fontSize = 54f,
            autoSize = true
        )
        val bitmapAuto = ClassBarIconGenerator.generateBitmapForRoom("413", autoSizeSettings)
        assertNotNull(bitmapAuto)
        assertEquals(96, bitmapAuto.width)
        assertEquals(96, bitmapAuto.height)
    }

    @Test
    fun testGenerateBitmapForCountdown_withAndWithoutAutoSize() {
        val autoSizeSettings = IconSettings(
            countdownFontSize = 54f,
            countdownAutoSize = true
        )
        val bitmapAuto = ClassBarIconGenerator.generateBitmapForCountdown("35", autoSizeSettings)
        assertNotNull(bitmapAuto)
        assertEquals(96, bitmapAuto.width)
        assertEquals(96, bitmapAuto.height)
    }

    @Test
    fun testAppPreferences_autoSizePersistence() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = AppPreferences(context)

        // Defaults
        prefs.resetDefaults()
        var settings = prefs.loadSettings()
        assertFalse(settings.autoSize)
        assertFalse(settings.countdownAutoSize)

        // Enable autoSize
        prefs.setAutoSize(true)
        prefs.setCountdownAutoSize(true)
        settings = prefs.loadSettings()
        assertTrue(settings.autoSize)
        assertTrue(settings.countdownAutoSize)

        // Reset room defaults
        prefs.resetRoomDefaults()
        settings = prefs.loadSettings()
        assertFalse(settings.autoSize)
        assertTrue(settings.countdownAutoSize)

        // Reset countdown defaults
        prefs.resetCountdownDefaults()
        settings = prefs.loadSettings()
        assertFalse(settings.countdownAutoSize)
    }
}
