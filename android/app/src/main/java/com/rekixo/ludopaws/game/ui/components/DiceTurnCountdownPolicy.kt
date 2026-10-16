package com.rekixo.ludopaws

/** Pure time-to-progress mapping for the authoritative 15-second turn timer. */
internal object DiceTurnCountdownPolicy {
    const val TURN_DURATION_MILLIS = 15_000L

    fun remainingFraction(
        deadlineElapsedRealtimeMillis: Long?,
        nowElapsedRealtimeMillis: Long,
        timeoutDurationMillis: Long = TURN_DURATION_MILLIS,
    ): Float? {
        val deadline = deadlineElapsedRealtimeMillis ?: return null
        val duration = timeoutDurationMillis.takeIf { it > 0L } ?: return null
        val remaining = (deadline - nowElapsedRealtimeMillis).coerceAtLeast(0L)
        return (remaining.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
}
