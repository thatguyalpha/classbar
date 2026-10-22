package com.thatguyalpha.classbar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat

object ClassBarIconGenerator {

    const val ICON_WIDTH = 96
    const val ICON_HEIGHT = 96
    private const val CENTER_X = ICON_WIDTH / 2f

    private const val WEIGHT_MEDIUM = 500
    private const val WEIGHT_BOLD = 700
    private const val BOLD_THRESHOLD = 1.5f

    /**
     * Resolves the Pixel status bar clock typeface:
     * `google-sans-text` -> `google-sans-flex` -> `sans-serif-medium`.
     * When [boldness] >= 1.5f, uses bold weight (700 / [Typeface.BOLD]) so the stroke builds
     * cleanly on a bold glyph base.
     */
    fun resolvePixelStatusBarTypeface(boldness: Float): Typeface {
        val candidates = listOf("google-sans-text", "google-sans-flex", "sans-serif-medium")
        val useBold = boldness >= BOLD_THRESHOLD

        for (family in candidates) {
            val resolved = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val weight = if (useBold) WEIGHT_BOLD else WEIGHT_MEDIUM
                Typeface.create(Typeface.create(family, Typeface.NORMAL), weight, false)
            } else {
                Typeface.create(family, if (useBold) Typeface.BOLD else Typeface.NORMAL)
            }
            if (resolved != Typeface.DEFAULT && resolved != null) {
                return resolved
            }
        }

        return if (useBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    /**
     * Renders a 96x96 status bar icon showing the room number (e.g. "307", "24", "102").
     */
    fun renderRoomBitmap(
        roomText: String,
        textSize: Float = AppPreferences.DEFAULT_FONT_SIZE,
        yPosition: Float = AppPreferences.DEFAULT_Y_POSITION,
        textScaleX: Float = AppPreferences.DEFAULT_TEXT_SCALE_X,
        boldness: Float = AppPreferences.DEFAULT_BOLDNESS
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_WIDTH, ICON_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val typeface = resolvePixelStatusBarTypeface(boldness)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            this.typeface = typeface
            this.textSize = textSize
            this.textScaleX = textScaleX

            if (boldness > 0f) {
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = boldness
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            } else {
                style = Paint.Style.FILL
            }
        }

        canvas.drawText(roomText, CENTER_X, yPosition, textPaint)
        return bitmap
    }

    /**
     * Renders bitmap directly from [IconSettings] for the upcoming room number.
     */
    fun generateBitmapForRoom(roomText: String, settings: IconSettings): Bitmap {
        return renderRoomBitmap(
            roomText = roomText,
            textSize = settings.fontSize,
            yPosition = settings.yPosition,
            textScaleX = settings.textScaleX,
            boldness = settings.boldness
        )
    }

    /**
     * Generates a preview bitmap using the configured preview room text.
     */
    fun generatePreviewBitmap(settings: IconSettings): Bitmap {
        return generateBitmapForRoom(settings.previewRoom, settings)
    }

    /**
     * Wraps the generated room bitmap into an [IconCompat] suitable for NotificationCompat.
     */
    fun generateIconCompat(roomText: String, settings: IconSettings): IconCompat {
        val bitmap = generateBitmapForRoom(roomText, settings)
        return IconCompat.createWithBitmap(bitmap)
    }
}
