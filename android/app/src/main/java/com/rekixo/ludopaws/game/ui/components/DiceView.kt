package com.rekixo.ludopaws

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.rekixo.ludopaws.feature.store.data.local.CosmeticInventoryStore
import com.rekixo.ludopaws.feature.store.domain.model.CosmeticCategory

class DiceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    init {
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription =
            "Dice. No verified outcome yet."
        // Avoid forcing software rasterization on every rolling animation frame.
        setLayerType(
            LAYER_TYPE_NONE,
            null,
        )
    }

    private val facePaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33000000
            setShadowLayer(dp(3f), 0f, dp(1.5f), 0x26000000)
        }
    private var cachedFaceShader: Shader? = null
    private var cachedFaceShaderSize = 0f
    private var cachedFaceShaderColors = intArrayOf()
    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.4f)
            color = 0xFF8A8A8A.toInt()
        }
    private val pipPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3A3A3A.toInt()
            style = Paint.Style.FILL
            setShadowLayer(
                dp(1.2f),
                0f,
                dp(.8f),
                0x26000000,
            )
        }
    private val rollingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(5f)
            color = 0xFFB8B8B8.toInt()
            setShadowLayer(
                dp(4f),
                0f,
                0f,
                0x446B7280,
            )
        }
    // Attention strokes inherit the owning player's team color. The glow is
    // drawn inside the dice bounds, so the pulse stays unclipped while breathing.
    private val attentionPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.8f)
            color = 0xFF8A8A8A.toInt()
            strokeJoin = Paint.Join.ROUND
        }
    private val attentionGlowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(5f)
            color = 0xFF8A8A8A.toInt()
            strokeJoin = Paint.Join.ROUND
        }

    private val turnTimerTrackPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(4.4f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = 0xFF8A8A8A.toInt()
            alpha = 62
        }
    private val turnTimerProgressPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(4.4f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = 0xFF8A8A8A.toInt()
        }

    private var turnDeadlineEpochMillis: Long? = null
    private var turnDeadlineElapsedRealtimeMillis: Long? = null
    private var turnCountdownDurationMillis = DiceTurnCountdownPolicy.TURN_DURATION_MILLIS

    private var diceStyleId =
        "dice_classic"
    private var previewStyleId: String? = null
    private var face = 1
    private var rolling = false
    private var settling = false
    private var rollStartedAtMillis = 0L
    private var settleStartedAtMillis = 0L
    private var settleStartRotationDegrees = 18f
    private var targetOutcome = 1
    private var rotationDegrees = 0f
    private var scale = 1f
    private var translationYFraction = 0f
    private var borderPulse = 0f
    private var playerTeamColor: Int? = null
    private var attentionPulsing = false
    private var attentionPhaseStartedAtMillis = 0L
    private var lastPresentedOutcomeEventKey: String? = null
    private var deferredSettleRunnable: Runnable? = null

    private val animationTicker =
        object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                when {
                    rolling -> {
                        val elapsed =
                            now - rollStartedAtMillis
                        applyFrame(
                            DiceRollAnimationPolicy
                                .rollingFrame(elapsed),
                        )
                    }

                    settling -> {
                        val frame =
                            DiceRollAnimationPolicy
                                .settleFrame(
                                    elapsedMillis =
                                        now - settleStartedAtMillis,
                                    outcome = targetOutcome,
                                    startRotationDegrees =
                                        settleStartRotationDegrees,
                                )
                        applyFrame(frame)
                        if (frame.finished) {
                            settling = false
                            resetTransform()
                        }
                    }

                    attentionPulsing -> {
                        val elapsed =
                            (now - attentionPhaseStartedAtMillis)
                                .coerceAtLeast(0L)
                        applyFrame(
                            DiceAttentionAnimationPolicy.frame(
                                elapsedMillis = elapsed,
                            ),
                        )
                    }

                    turnDeadlineElapsedRealtimeMillis != null -> Unit

                    else -> return
                }

                invalidate()
                // The attention cue is a real animation, not a one-shot callback.
                // Keep scheduling its frames while enabled, just like rolling and
                // settling, and let every phase share this single ticker.
                if (rolling || settling || attentionPulsing || turnCountdownActive()) {
                    postOnAnimation(this)
                }
            }
        }

    /**
     * Binds this dice's outline to the owning player's stable team color.
     * The same color is used by idle, attention-pulse, and rolling strokes.
     */
    fun setPlayerTeamColor(colorName: String?) {
        playerTeamColor =
            LudoPawsTeamIdentityPolicy
                .resolve(colorName)
                ?.argb
        invalidate()
    }

    /**
     * Binds countdown to the server's absolute epoch-millisecond deadline.
     * Repeated snapshots with the same deadline do not restart its progress.
     */
    fun setTurnDeadlineAt(
        deadlineEpochMillis: Long?,
        serverNowEpochMillis: Long? = null,
        timeoutDurationMillis: Long = DiceTurnCountdownPolicy.TURN_DURATION_MILLIS,
    ) {
        val safeDuration = timeoutDurationMillis.takeIf { it > 0L }
            ?: DiceTurnCountdownPolicy.TURN_DURATION_MILLIS
        if (
            turnDeadlineEpochMillis == deadlineEpochMillis &&
            turnCountdownDurationMillis == safeDuration
        ) return
        turnDeadlineEpochMillis = deadlineEpochMillis
        turnCountdownDurationMillis = safeDuration
        turnDeadlineElapsedRealtimeMillis =
            deadlineEpochMillis?.let { deadline ->
                val remaining = serverNowEpochMillis?.let { serverNow ->
                    deadline - serverNow
                } ?: (deadline - System.currentTimeMillis())
                SystemClock.elapsedRealtime() + remaining.coerceAtLeast(0L)
            }
        if (turnCountdownActive()) postOnAnimation(animationTicker)
        else if (!rolling && !settling && !attentionPulsing) removeCallbacks(animationTicker)
        invalidate()
    }

    private fun turnCountdownActive(): Boolean =
        turnDeadlineElapsedRealtimeMillis?.let {
            SystemClock.elapsedRealtime() < it
        } == true

    fun setAttentionEnabled(enabled: Boolean) {
        val shouldPulse =
            enabled &&
                !rolling &&
                !settling
        if (shouldPulse == attentionPulsing) {
            return
        }

        attentionPulsing = shouldPulse
        if (shouldPulse) {
            attentionPhaseStartedAtMillis = SystemClock.uptimeMillis()
            postOnAnimation(animationTicker)
        } else {
            if (!turnCountdownActive() && !rolling && !settling) {
                removeCallbacks(animationTicker)
            }
            resetTransform()
            invalidate()
            if (turnCountdownActive()) postOnAnimation(animationTicker)
        }
    }

    fun startRolling() {
        if (rolling) return
        cancelDeferredSettle()
        attentionPulsing = false
        removeCallbacks(animationTicker)
        rolling = true
        settling = false
        rollStartedAtMillis =
            SystemClock.uptimeMillis()
        contentDescription =
            "Dice verification in progress."
        postOnAnimation(animationTicker)
    }

    fun showOutcome(
        outcome: Int,
        animate: Boolean = true,
        eventKey: String? = null,
        minimumRollDurationMillis: Long = 0L,
    ) {
        if (outcome !in 1..6) return
        if (eventKey != null && eventKey == lastPresentedOutcomeEventKey && targetOutcome == outcome) {
            return
        }
        cancelDeferredSettle()
        attentionPulsing = false

        if (!animate) {
            rolling = false
            settling = false
            removeCallbacks(animationTicker)
            targetOutcome = outcome
            face = outcome
            contentDescription = "Dice outcome $outcome."
            if (eventKey != null) lastPresentedOutcomeEventKey = eventKey
            resetTransform()
            invalidate()
            return
        }

        // Keep offline Robot / Pass & Play transitions intact.
        if (eventKey == null) {
            targetOutcome = outcome
            face = outcome
            contentDescription = "Dice outcome $outcome."
            beginSettle(outcome)
            return
        }

        // A remote poll can skip COMMITTED/RESOLVING snapshots. Show a real
        // spin for the verified event instead of flashing directly to a face.
        if (!rolling) startRolling()
        lastPresentedOutcomeEventKey = eventKey
        targetOutcome = outcome
        contentDescription = "Verified dice outcome $outcome; settling."

        val remainingSpinMillis = DiceRollAnimationPolicy.remainingVisibleRollMillis(
            elapsedMillis = SystemClock.uptimeMillis() - rollStartedAtMillis,
            minimumVisibleRollMillis = minimumRollDurationMillis,
        )
        if (remainingSpinMillis > 0L) {
            val expectedEventKey = eventKey
            deferredSettleRunnable = Runnable {
                deferredSettleRunnable = null
                if (lastPresentedOutcomeEventKey != expectedEventKey || targetOutcome != outcome) {
                    return@Runnable
                }
                beginSettle(outcome)
            }
            postDelayed(requireNotNull(deferredSettleRunnable), remainingSpinMillis)
        } else {
            beginSettle(outcome)
        }
    }

    fun remainingOutcomePresentationMillis(minimumRollDurationMillis: Long): Long {
        val now = SystemClock.uptimeMillis()
        return when {
            rolling ->
                DiceRollAnimationPolicy.remainingVisibleRollMillis(
                    now - rollStartedAtMillis,
                    minimumRollDurationMillis,
                ) + DiceRollAnimationPolicy.SETTLE_DURATION_MILLIS
            settling ->
                (DiceRollAnimationPolicy.SETTLE_DURATION_MILLIS - (now - settleStartedAtMillis))
                    .coerceAtLeast(0L)
            else -> 0L
        }
    }

    private fun beginSettle(outcome: Int) {
        val wasRolling = rolling
        rolling = false
        removeCallbacks(animationTicker)
        targetOutcome = outcome
        face = outcome
        contentDescription = "Dice outcome $outcome."
        settleStartRotationDegrees =
            if (wasRolling && kotlin.math.abs(rotationDegrees) > 1f) {
                rotationDegrees.coerceIn(-20f, 20f)
            } else {
                18f
            }
        settling = true
        settleStartedAtMillis = SystemClock.uptimeMillis()
        postOnAnimation(animationTicker)
    }

    private fun cancelDeferredSettle() {
        deferredSettleRunnable?.let { removeCallbacks(it) }
        deferredSettleRunnable = null
    }

    fun stopRolling() {
        cancelDeferredSettle()
        rolling = false
        settling = false
        attentionPulsing = false
        removeCallbacks(animationTicker)
        resetTransform()
        contentDescription =
            "Dice verification stopped."
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reloadStyle()
        if (turnCountdownActive()) postOnAnimation(animationTicker)
    }

    fun reloadStyle() {
        diceStyleId =
            previewStyleId
                ?: CosmeticInventoryStore(
                    context,
                )
                    .selectedId(
                        CosmeticCategory.DICE,
                    )
        invalidate()
    }

    /** Applies a store-only preview style without changing equipment or ownership. */
    fun setPreviewStyle(styleId: String) {
        previewStyleId = styleId
        diceStyleId = styleId
        invalidate()
    }

    override fun onDetachedFromWindow() {
        cancelDeferredSettle()
        rolling = false
        settling = false
        attentionPulsing = false
        removeCallbacks(animationTicker)
        resetTransform()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desired =
            dp(96f).toInt()
        val width =
            resolveSize(
                desired,
                widthMeasureSpec,
            )
        val height =
            resolveSize(
                desired,
                heightMeasureSpec,
            )
        val size =
            minOf(
                width,
                height,
            )
        setMeasuredDimension(
            size,
            size,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size =
            minOf(
                width,
                height,
            ).toFloat()
        if (size <= 0f) return

        val center = size * .5f
        val saveCount = canvas.save()
        canvas.translate(
            0f,
            size * translationYFraction,
        )
        canvas.scale(
            scale,
            scale,
            center,
            center,
        )
        canvas.rotate(
            rotationDegrees,
            center,
            center,
        )

        val shadow =
            RectF(
                size * .09f,
                size * .11f,
                size * .91f,
                size * .92f,
            )
        facePaint.shader = null
        facePaint.color =
            0x33000000
        facePaint.setShadowLayer(
            dp(3f),
            0f,
            dp(1.5f),
            0x26000000,
        )
        canvas.drawRoundRect(
            shadow,
            size * .16f,
            size * .16f,
            shadowPaint,
        )

        val inset =
            size * .07f
        val rect =
            RectF(
                inset,
                inset,
                size - inset,
                size - inset,
            )
        val palette =
            dicePalette()

        facePaint.alpha = 255
        if (
            cachedFaceShader == null ||
            cachedFaceShaderSize != size ||
            !cachedFaceShaderColors.contentEquals(palette.faceColors)
        ) {
            cachedFaceShader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                palette.faceColors,
                null,
                Shader.TileMode.CLAMP,
            )
            cachedFaceShaderSize = size
            cachedFaceShaderColors = palette.faceColors.copyOf()
        }
        facePaint.shader = cachedFaceShader
        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            facePaint,
        )
        facePaint.shader = null
        facePaint.color =
            0x42FFFFFF
        canvas.drawRoundRect(
            RectF(
                rect.left + size * .035f,
                rect.top + size * .035f,
                rect.right - size * .035f,
                rect.top + rect.height() * .43f,
            ),
            size * .13f,
            size * .13f,
            facePaint,
        )

        borderPaint.color =
            palette.borderColor
        pipPaint.color =
            palette.pipColor
        rollingPaint.color =
            palette.borderColor
        val attentionColor =
            palette.borderColor
        attentionPaint.color = attentionColor
        attentionGlowPaint.color = attentionColor
        rollingPaint.alpha =
            (145f + borderPulse.coerceIn(0f, 1f) * 110f)
                .toInt()
        if (attentionPulsing) {
            // Keep the soft team-colored halo on the inside of the same rounded outline.
            // An external shadow would be clipped by this View's own drawing bounds.
            val glowInset = dp(2.6f)
            val glowRect =
                RectF(
                    rect.left + glowInset,
                    rect.top + glowInset,
                    rect.right - glowInset,
                    rect.bottom - glowInset,
                )
            attentionGlowPaint.alpha =
                (34f + borderPulse.coerceIn(0f, 1f) * 42f)
                    .toInt()
            canvas.drawRoundRect(
                glowRect,
                (size * .17f - glowInset).coerceAtLeast(0f),
                (size * .17f - glowInset).coerceAtLeast(0f),
                attentionGlowPaint,
            )
            attentionPaint.alpha =
                (190f + borderPulse.coerceIn(0f, 1f) * 65f)
                    .toInt()
        }
        val outlinePaint =
            when {
                rolling || settling -> rollingPaint
                attentionPulsing -> attentionPaint
                else -> borderPaint
            }
        if (turnDeadlineElapsedRealtimeMillis != null && !rolling && !settling) {
            outlinePaint.alpha = minOf(outlinePaint.alpha, 72)
        } else if (outlinePaint === borderPaint) {
            borderPaint.alpha = 255
        }
        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            outlinePaint,
        )
        drawTurnCountdown(canvas, rect, size, palette.borderColor)

        val left = size * .31f
        val pipCenter = size * .50f
        val right = size * .69f
        val top = size * .31f
        val middle = size * .50f
        val bottom = size * .69f
        val pipRadius =
            size * .055f

        fun pip(
            x: Float,
            y: Float,
        ) {
            canvas.drawCircle(
                x,
                y,
                pipRadius,
                pipPaint,
            )
        }

        when (face) {
            1 -> pip(pipCenter, middle)
            2 -> {
                pip(left, top)
                pip(right, bottom)
            }
            3 -> {
                pip(left, top)
                pip(pipCenter, middle)
                pip(right, bottom)
            }
            4 -> {
                pip(left, top)
                pip(right, top)
                pip(left, bottom)
                pip(right, bottom)
            }
            5 -> {
                pip(left, top)
                pip(right, top)
                pip(pipCenter, middle)
                pip(left, bottom)
                pip(right, bottom)
            }
            6 -> {
                pip(left, top)
                pip(right, top)
                pip(left, middle)
                pip(right, middle)
                pip(left, bottom)
                pip(right, bottom)
            }
        }

        canvas.restoreToCount(saveCount)
    }

    private fun drawTurnCountdown(
        canvas: Canvas,
        rect: RectF,
        size: Float,
        teamColor: Int,
    ) {
        val deadline = turnDeadlineElapsedRealtimeMillis ?: return
        val fraction = DiceTurnCountdownPolicy.remainingFraction(
            deadlineElapsedRealtimeMillis = deadline,
            nowElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            timeoutDurationMillis = turnCountdownDurationMillis,
        ) ?: return

        val radius = (size * .17f - dp(2.2f)).coerceAtLeast(0f)
        val path = Path().apply {
            addRoundRect(rect, radius, radius, Path.Direction.CW)
        }
        val measure = PathMeasure(path, false)
        val length = measure.length
        if (length <= 0f) return

        turnTimerTrackPaint.color = teamColor
        turnTimerProgressPaint.color = teamColor
        turnTimerTrackPaint.alpha = 62
        turnTimerProgressPaint.alpha = if (fraction <= 0f) 0 else 255
        canvas.drawPath(path, turnTimerTrackPaint)

        if (fraction > 0f) {
            val progressPath = Path()
            measure.getSegment(0f, length * fraction.coerceIn(0f, 1f), progressPath, true)
            canvas.drawPath(progressPath, turnTimerProgressPaint)
        }
    }

    private fun applyFrame(
        frame: DiceRollAnimationPolicy.Frame,
    ) {
        face = frame.face
        rotationDegrees = frame.rotationDegrees
        scale = frame.scale
        translationYFraction = frame.translationYFraction
        borderPulse = frame.borderPulse
    }

    private fun resetTransform() {
        rotationDegrees = 0f
        scale = 1f
        translationYFraction = 0f
        borderPulse = 0f
    }

    /**
     * Gameplay dice keep a high-contrast neutral face and a team-colored outline.
     *
     * The selected cosmetic id is still retained by the inventory/store contract,
     * while the white/light-gray face remains legible against the gameplay HUD.
     */
    private fun dicePalette(): DicePalette =
        DiceStylePalettePolicy.resolve(
            styleId = diceStyleId,
            teamColor = playerTeamColor,
        )

    private fun dp(
        value: Float,
    ): Float =
        value *
            resources
                .displayMetrics
                .density
}
