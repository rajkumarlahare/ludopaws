package com.rekixo.ludopaws

/** Pure time-to-progress mapping for the authoritative 15-second turn timer. */
internal object DiceTurnCountdownPolicy {
    const val TURN_DURATION_MILLIS = 15_000L

    fun remainingFraction(
        deadlineElapsedRealtimeMillis: Long?,
        nowElapsedRealtimeMillis: Long,
    ): Float? {
        val deadline = deadlineElapsedRealtimeMillis ?: return null
        val remaining = (deadline - nowElapsedRealtimeMillis).coerceAtLeast(0L)
        return (remaining.toFloat() / TURN_DURATION_MILLIS.toFloat()).coerceIn(0f, 1f)
    }
}
