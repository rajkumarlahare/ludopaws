package com.rekixo.ludopaws.core.ui.icons

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import com.rekixo.ludopaws.core.ui.theme.AppUiTokens
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Names describe the meaning of a reusable action glyph, not a screen or feature. */
internal enum class AppIconName {
    BACK,
    BACK_ARROW,
    CHEVRON_LEFT,
    CLOSE,
    ADD,
    SEARCH,
    COPY,
    SHARE,
    PLAY,
    REFRESH,
    SETTINGS,
    PROFILE,
    LEADERBOARD,
    PEOPLE,
    MESSAGE,
    CLOCK,
    MORE,
}

/**
 * OUTLINE is the default for action icons. FILLED is intentionally interpreted
 * only by glyphs where a filled form is meaningful; brand illustrations remain
 * feature-owned and should not be routed through this view.
 */
internal enum class AppIconVariant {
    OUTLINE,
    FILLED,
}

/**
 * A small, size-independent Canvas icon view shared across native UI features.
 *
 * Geometry is authored on a 100-unit canvas while stroke width is specified in
 * dp and compensated for canvas scaling. This keeps common outlines visually
 * consistent at different view sizes and densities.
 *
 * Keep accessible labels on the clickable parent. This child view is decorative
 * so TalkBack does not announce a second, unlabeled node for the same action.
 */
internal class AppIconView(
    context: Context,
    iconName: AppIconName,
    iconTint: Int = Color.WHITE,
    iconVariant: AppIconVariant = AppIconVariant.OUTLINE,
    strokeWidthDp: Float = AppUiTokens.Icon.NORMAL_STROKE_DP,
) : View(context) {

    var iconName: AppIconName = iconName
        set(value) {
            field = value
            invalidate()
        }

    var iconTint: Int = iconTint
        set(value) {
            field = value
            invalidate()
        }

    var iconVariant: AppIconVariant = iconVariant
        set(value) {
            field = value
            invalidate()
        }

    var strokeWidthDp: Float = strokeWidthDp.coerceIn(0.5f, 8f)
        set(value) {
            field = value.coerceIn(0.5f, 8f)
            invalidate()
        }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val scale = size / 100f
        val left = (width - size) / 2f
        val top = (height - size) / 2f

        stroke.color = iconTint
        fill.color = iconTint
        // Counteract canvas scaling so the requested stroke remains dp-based.
        stroke.strokeWidth = strokeWidthDp * resources.displayMetrics.density / scale

        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)

        when (iconName) {
            AppIconName.BACK,
            AppIconName.CHEVRON_LEFT -> drawChevronLeft(canvas)

            AppIconName.BACK_ARROW -> drawBackArrow(canvas)

            AppIconName.CLOSE -> drawClose(canvas)
            AppIconName.ADD -> drawAdd(canvas)
            AppIconName.SEARCH -> drawSearch(canvas)
            AppIconName.COPY -> drawCopy(canvas)
            AppIconName.SHARE -> drawShare(canvas)
            AppIconName.PLAY -> drawPlay(canvas)
            AppIconName.REFRESH -> drawRefresh(canvas)
            AppIconName.SETTINGS -> drawSettings(canvas)
            AppIconName.PROFILE -> drawProfile(canvas)
            AppIconName.LEADERBOARD -> drawLeaderboard(canvas)
            AppIconName.PEOPLE -> drawPeople(canvas)
            AppIconName.MESSAGE -> drawMessage(canvas)
            AppIconName.CLOCK -> drawClock(canvas)
            AppIconName.MORE -> drawMore(canvas)
        }

        canvas.restore()
    }

    private fun drawBackArrow(canvas: Canvas) {
        // A true arrow with a shaft distinguishes navigation from a plain left chevron.
        canvas.drawLine(35f, 50f, 76f, 50f, stroke)
        canvas.drawLine(35f, 50f, 55f, 31f, stroke)
        canvas.drawLine(35f, 50f, 55f, 69f, stroke)
    }

    private fun drawChevronLeft(canvas: Canvas) {
        val span = 24f
        canvas.drawLine(50f + span * 0.55f, 50f - span, 50f - span * 0.55f, 50f, stroke)
        canvas.drawLine(50f - span * 0.55f, 50f, 50f + span * 0.55f, 50f + span, stroke)
    }

    private fun drawClose(canvas: Canvas) {
        canvas.drawLine(34f, 34f, 66f, 66f, stroke)
        canvas.drawLine(66f, 34f, 34f, 66f, stroke)
    }

    private fun drawAdd(canvas: Canvas) {
        canvas.drawLine(50f, 27f, 50f, 73f, stroke)
        canvas.drawLine(27f, 50f, 73f, 50f, stroke)
    }

    private fun drawSearch(canvas: Canvas) {
        canvas.drawCircle(43f, 43f, 20f, stroke)
        canvas.drawLine(58f, 58f, 76f, 76f, stroke)
    }

    private fun drawCopy(canvas: Canvas) {
        canvas.drawRoundRect(RectF(27f, 22f, 61f, 64f), 5f, 5f, stroke)
        canvas.drawRoundRect(RectF(39f, 36f, 73f, 78f), 5f, 5f, stroke)
    }

    private fun drawShare(canvas: Canvas) {
        canvas.drawLine(37f, 48f, 61f, 35f, stroke)
        canvas.drawLine(37f, 53f, 61f, 66f, stroke)
        canvas.drawCircle(30f, 50f, 7f, fill)
        canvas.drawCircle(68f, 31f, 7f, fill)
        canvas.drawCircle(68f, 69f, 7f, fill)
    }

    private fun drawPlay(canvas: Canvas) {
        val path = Path().apply {
            moveTo(38f, 26f)
            lineTo(72f, 50f)
            lineTo(38f, 74f)
            close()
        }
        canvas.drawPath(path, if (iconVariant == AppIconVariant.FILLED) fill else stroke)
    }

    private fun drawRefresh(canvas: Canvas) {
        val arc = RectF(26f, 26f, 74f, 74f)
        canvas.drawArc(arc, 42f, 278f, false, stroke)
        val arrow = Path().apply {
            moveTo(61f, 24f)
            lineTo(76f, 25f)
            lineTo(72f, 39f)
        }
        canvas.drawPath(arrow, stroke)
    }

    private fun drawSettings(canvas: Canvas) {
        val path = Path()
        for (index in 0 until 16) {
            val angle = (Math.PI * 2.0 * index / 16.0) - Math.PI / 2.0
            val radius = if (index % 2 == 0) 31f else 25f
            val x = 50f + (cos(angle) * radius.toDouble()).toFloat()
            val y = 50f + (sin(angle) * radius.toDouble()).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, stroke)
        canvas.drawCircle(50f, 50f, 9f, stroke)
    }

    private fun drawProfile(canvas: Canvas) {
        if (iconVariant == AppIconVariant.FILLED) {
            canvas.drawCircle(50f, 34f, 13f, fill)
            canvas.drawRoundRect(RectF(23f, 52f, 77f, 82f), 18f, 18f, fill)
        } else {
            canvas.drawCircle(50f, 34f, 13f, stroke)
            canvas.drawArc(RectF(23f, 48f, 77f, 88f), 180f, 180f, false, stroke)
        }
    }

    private fun drawLeaderboard(canvas: Canvas) {
        val bars = arrayOf(
            RectF(24f, 54f, 38f, 76f),
            RectF(43f, 34f, 57f, 76f),
            RectF(62f, 20f, 76f, 76f),
        )
        bars.forEach { bar ->
            if (iconVariant == AppIconVariant.FILLED) {
                canvas.drawRoundRect(bar, 3f, 3f, fill)
            } else {
                canvas.drawRoundRect(bar, 3f, 3f, stroke)
            }
        }
        canvas.drawLine(20f, 79f, 80f, 79f, stroke)
    }

    private fun drawPeople(canvas: Canvas) {
        if (iconVariant == AppIconVariant.FILLED) {
            canvas.drawCircle(37f, 36f, 10f, fill)
            canvas.drawCircle(66f, 39f, 8f, fill)
            canvas.drawRoundRect(RectF(18f, 50f, 54f, 78f), 12f, 12f, fill)
            canvas.drawRoundRect(RectF(54f, 53f, 81f, 77f), 10f, 10f, fill)
        } else {
            canvas.drawCircle(37f, 36f, 10f, stroke)
            canvas.drawCircle(66f, 39f, 8f, stroke)
            canvas.drawArc(RectF(17f, 48f, 57f, 83f), 180f, 180f, false, stroke)
            canvas.drawArc(RectF(52f, 50f, 82f, 80f), 180f, 180f, false, stroke)
        }
    }

    private fun drawMessage(canvas: Canvas) {
        val bubble = RectF(20f, 24f, 80f, 68f)
        if (iconVariant == AppIconVariant.FILLED) {
            canvas.drawRoundRect(bubble, 10f, 10f, fill)
            val tail = Path().apply {
                moveTo(34f, 66f)
                lineTo(29f, 80f)
                lineTo(47f, 68f)
                close()
            }
            canvas.drawPath(tail, fill)
        } else {
            canvas.drawRoundRect(bubble, 10f, 10f, stroke)
            val tail = Path().apply {
                moveTo(34f, 67f)
                lineTo(29f, 80f)
                lineTo(47f, 68f)
            }
            canvas.drawPath(tail, stroke)
            canvas.drawCircle(38f, 46f, 2.5f, fill)
            canvas.drawCircle(50f, 46f, 2.5f, fill)
            canvas.drawCircle(62f, 46f, 2.5f, fill)
        }
    }

    private fun drawClock(canvas: Canvas) {
        canvas.drawCircle(50f, 50f, 28f, stroke)
        canvas.drawLine(50f, 50f, 50f, 32f, stroke)
        canvas.drawLine(50f, 50f, 65f, 58f, stroke)
    }

    private fun drawMore(canvas: Canvas) {
        val radius = if (iconVariant == AppIconVariant.FILLED) 4.5f else 3.5f
        val paint = if (iconVariant == AppIconVariant.FILLED) fill else stroke
        canvas.drawCircle(28f, 50f, radius, paint)
        canvas.drawCircle(50f, 50f, radius, paint)
        canvas.drawCircle(72f, 50f, radius, paint)
    }
}
