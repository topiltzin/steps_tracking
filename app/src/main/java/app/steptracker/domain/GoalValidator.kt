package app.steptracker.domain

object GoalValidator {

    /** Returns a positive whole number, or null for empty, zero, negative, non-numeric or overflowing input. */
    fun parse(input: String): Int? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || !trimmed.all { it in '0'..'9' }) return null
        val value = trimmed.toIntOrNull() ?: return null
        return value.takeIf { it > 0 }
    }
}
