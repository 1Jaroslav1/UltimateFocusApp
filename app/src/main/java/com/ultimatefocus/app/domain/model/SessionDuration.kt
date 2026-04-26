/**
 * The user's commitment to a single in-app session. The duration picker
 * (ULT-78) materializes one of these whenever the user agrees to enter a
 * reminder-enabled app. The four presets mirror the AC: 1, 5, 10, 15
 * minutes. Custom values are clamped to the [MIN_MINUTES, MAX_MINUTES]
 * inclusive range — open-ended sessions defeat the whole "commit a
 * duration" friction.
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.domain.model

@JvmInline
value class SessionDurationMinutes(val value: Int) {

    init {
        require(value in MIN_MINUTES..MAX_MINUTES) {
            "Session duration must be between $MIN_MINUTES and $MAX_MINUTES minutes (was $value)"
        }
    }

    fun toMillis(): Long = value.toLong() * 60_000L

    companion object {
        const val MIN_MINUTES: Int = 1
        const val MAX_MINUTES: Int = 240

        val PRESETS: List<Int> = listOf(1, 5, 10, 15)

        fun coerce(minutes: Int): SessionDurationMinutes =
            SessionDurationMinutes(minutes.coerceIn(MIN_MINUTES, MAX_MINUTES))

        fun isValid(minutes: Int): Boolean = minutes in MIN_MINUTES..MAX_MINUTES
    }
}
