package com.rekixo.ludopaws.core.ui.art

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * A plywood surface with a softly carved, recessed pocket.
 *
 * This variant is intended for artwork that should feel seated inside the wood.
 * The pocket uses an inset continuous bevel, curved corners, a darker top/left
 * edge and a warmer lower edge to suggest depth without using a bitmap.
 */
internal class RecessedPlywoodDrawable(
    private val density: Float,
    private val outerCornerRadiusDp: Float = 18f,
) : Drawable() {
    private val plywood = PlywoodTextureDrawable(density, outerCornerRadiusDp)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val drawableBounds = RectF()
    private var drawableAlpha = 255

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        plywood.bounds = bounds
    }

    override fun draw(canvas: Canvas) {
        val b = drawableBounds.apply { set(bounds) }
        if (b.width() <= 0f || b.height() <= 0f) return

        val saveCount = canvas.saveLayerAlpha(b, drawableAlpha)
        plywood.draw(canvas)

        // The inner pocket remains inset from the plywood's continuous outside rim.
        val lip = 6f * density
        val pocket = RectF(b).apply { inset(lip, lip) }
        if (pocket.width() <= 0f || pocket.height() <= 0f) {
            canvas.restoreToCount(saveCount)
            return
        }
        val pocketRadius = (11f * density).coerceAtMost(minOf(pocket.width(), pocket.height()) / 2f)

        // A continuous dark ring acts as the cavity's cast shadow.
        val shadowOuter = RectF(pocket).apply { inset(-2.4f * density, -2.4f * density) }
        val shadowInner = RectF(pocket).apply { inset(0.9f * density, 0.9f * density) }
        edgePaint.style = Paint.Style.FILL
        edgePaint.shader = LinearGradient(
            shadowOuter.left,
            shadowOuter.top,
            shadowOuter.right,
            shadowOuter.bottom,
            intArrayOf(
                0xFF533019.toInt(),
                0xFF754323.toInt(),
                0xFFAD7744.toInt(),
                0xFF4D2D19.toInt(),
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(
            roundedRingPath(
                shadowOuter,
                pocketRadius + 2.4f * density,
                shadowInner,
                pocketRadius - 0.9f * density,
            ),
            edgePaint,
        )
        edgePaint.shader = null

        // Recessed timber face: slightly darker than the surrounding veneer.
        val face = RectF(pocket).apply { inset(1.25f * density, 1.25f * density) }
        val faceRadius = (pocketRadius - 1.25f * density).coerceAtLeast(1f)
        pocketPaint.shader = LinearGradient(
            face.left,
            face.top,
            face.right,
            face.bottom,
            intArrayOf(
                0xFF9D683B.toInt(),
                0xFFD0A071.toInt(),
                0xFFBD8954.toInt(),
                0xFF8D5B32.toInt(),
            ),
            floatArrayOf(0f, 0.38f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(face, faceRadius, faceRadius, pocketPaint)
        pocketPaint.shader = null

        // Faint inset grain carries the plywood texture into the well, rather than
        // letting the center read as a flat brown sticker behind the character.
        val grainClip = Path().apply {
            addRoundRect(face, faceRadius, faceRadius, Path.Direction.CW)
        }
        val grainSave = canvas.save()
        canvas.clipPath(grainClip)
        for (index in 0 until 9) {
            val fraction = (index + 0.4f) / 9f
            val gx = face.left + face.width() * fraction
            val wave = density * (1.6f + (index % 4) * 0.9f)
            val path = Path().apply {
                moveTo(gx, face.top)
                cubicTo(
                    gx - wave,
                    face.top + face.height() * 0.24f,
                    gx + wave * 0.65f,
                    face.top + face.height() * 0.55f,
                    gx - wave * 0.15f,
                    face.bottom,
                )
            }
            grainPaint.color = if (index % 2 == 0) {
                Color.argb(22, 77, 43, 22)
            } else {
                Color.argb(24, 255, 224, 177)
            }
            grainPaint.strokeWidth = (if (index % 3 == 0) 1f else 0.6f) * density
            canvas.drawPath(path, grainPaint)
        }
        canvas.restoreToCount(grainSave)

        // Inner lip: a continuous warm bevel, lightest toward the lower/right edge.
        val lipOuter = RectF(pocket).apply { inset(0.3f * density, 0.3f * density) }
        val lipInner = RectF(face).apply { inset(0.35f * density, 0.35f * density) }
        edgePaint.shader = LinearGradient(
            lipOuter.left,
            lipOuter.top,
            lipOuter.right,
            lipOuter.bottom,
            intArrayOf(
                0xCC4B2C19.toInt(),
                0x996D4327.toInt(),
                0x99F0C58B.toInt(),
                0xD6F2C58B.toInt(),
            ),
            floatArrayOf(0f, 0.38f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(
            roundedRingPath(
                lipOuter,
                pocketRadius - 0.3f * density,
                lipInner,
                (faceRadius - 0.35f * density).coerceAtLeast(1f),
            ),
            edgePaint,
        )
        edgePaint.shader = null

        canvas.restoreToCount(saveCount)
    }

    private fun roundedRingPath(
        outer: RectF,
        outerRadius: Float,
        inner: RectF,
        innerRadius: Float,
    ): Path = Path().apply {
        fillType = Path.FillType.EVEN_ODD
        addRoundRect(outer, outerRadius, outerRadius, Path.Direction.CW)
        addRoundRect(inner, innerRadius, innerRadius, Path.Direction.CW)
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        plywood.setColorFilter(colorFilter)
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
