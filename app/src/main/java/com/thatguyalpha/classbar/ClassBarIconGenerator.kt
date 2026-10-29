package com.thatguyalpha.classbar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.abs

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
     * Calculates the optimal font size so that [text] touches the left and right edges of the canvas
     * (target width = [targetWidth]), while not exceeding the vertical bounds [maxHeight].
     */
    fun calculateAutosizedFontSize(
        text: String,
        typeface: Typeface,
        textScaleX: Float,
        boldness: Float,
        targetWidth: Float = 94f,
        maxHeight: Float = 86f
    ): Float {
        if (text.isBlank()) return AppPreferences.DEFAULT_FONT_SIZE

        val baseSize = 100f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = baseSize
            this.textScaleX = textScaleX
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val boundsWidth = bounds.width().toFloat()
        val measureWidth = paint.measureText(text)
        val baseWidth = maxOf(boundsWidth, measureWidth).coerceAtLeast(1f)

        // Effective available width and height taking stroke width (boldness) into account
        val availableWidth = (targetWidth - boldness).coerceAtLeast(10f)
        val availableHeight = (maxHeight - boldness).coerceAtLeast(10f)

        val sizeFromWidth = baseSize * (availableWidth / baseWidth)

        val boundsHeight = bounds.height().toFloat()
        val metrics = paint.fontMetrics
        val baseHeight = if (boundsHeight > 0f) boundsHeight else abs(metrics.ascent) * 0.75f
        val sizeFromHeight = baseSize * (availableHeight / baseHeight.coerceAtLeast(1f))

        val optimalSize = minOf(sizeFromWidth, sizeFromHeight)
        return optimalSize.coerceIn(AppPreferences.MIN_FONT_SIZE, 120f)
    }

    /**
     * Renders a 96x96 status bar icon showing the room number (e.g. "307", "24", "102").
     */
    fun renderRoomBitmap(
        roomText: String,
        textSize: Float = AppPreferences.DEFAULT_FONT_SIZE,
        yPosition: Float = AppPreferences.DEFAULT_Y_POSITION,
        textScaleX: Float = AppPreferences.DEFAULT_TEXT_SCALE_X,
        boldness: Float = AppPreferences.DEFAULT_BOLDNESS,
        autoSize: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_WIDTH, ICON_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val typeface = resolvePixelStatusBarTypeface(boldness)

        val effectiveFontSize: Float
        val effectiveYPosition: Float

        if (autoSize && roomText.isNotBlank()) {
            effectiveFontSize = calculateAutosizedFontSize(
                text = roomText,
                typeface = typeface,
                textScaleX = textScaleX,
                boldness = boldness
            )

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                this.typeface = typeface
                this.textSize = effectiveFontSize
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

            val metrics = textPaint.fontMetrics
            val visualCenterOffset = (metrics.ascent + metrics.descent) / 2f
            val yOffset = yPosition - AppPreferences.DEFAULT_Y_POSITION
            effectiveYPosition = (ICON_HEIGHT / 2f - visualCenterOffset) + yOffset
            canvas.drawText(roomText, CENTER_X, effectiveYPosition, textPaint)
        } else {
            effectiveFontSize = textSize
            effectiveYPosition = yPosition

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                this.typeface = typeface
                this.textSize = effectiveFontSize
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

            canvas.drawText(roomText, CENTER_X, effectiveYPosition, textPaint)
        }

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
            boldness = settings.boldness,
            autoSize = settings.autoSize
        )
    }

    /**
     * Renders bitmap directly from [IconSettings] for the remaining countdown minutes.
     */
    fun generateBitmapForCountdown(countdownText: String, settings: IconSettings): Bitmap {
        return renderRoomBitmap(
            roomText = countdownText,
            textSize = settings.countdownFontSize,
            yPosition = settings.countdownYPosition,
            textScaleX = settings.countdownTextScaleX,
            boldness = settings.countdownBoldness,
            autoSize = settings.countdownAutoSize
        )
    }

    /**
     * Generates a preview bitmap using the configured preview room text.
     */
    fun generatePreviewBitmap(settings: IconSettings): Bitmap {
        return generateBitmapForRoom(settings.previewRoom, settings)
    }

    /**
     * Generates a preview bitmap for the countdown icon using the configured countdown settings.
     */
    fun generatePreviewCountdownBitmap(countdownText: String, settings: IconSettings): Bitmap {
        return generateBitmapForCountdown(countdownText, settings)
    }

    /**
     * Wraps the generated room or countdown bitmap into an [IconCompat] suitable for NotificationCompat.
     */
    fun generateIconCompat(text: String, settings: IconSettings, isCountdown: Boolean = false): IconCompat {
        val bitmap = if (isCountdown) {
            generateBitmapForCountdown(text, settings)
        } else {
            generateBitmapForRoom(text, settings)
        }
        return IconCompat.createWithBitmap(bitmap)
    }
}
