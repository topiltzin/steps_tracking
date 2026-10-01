package app.steptracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalValidatorTest {

    @Test
    fun `accepts positive whole numbers`() {
        assertEquals(500, GoalValidator.parse("500"))
        assertEquals(10000, GoalValidator.parse("10000"))
    }

    @Test
    fun `rejects empty zero negative and non numeric input`() {
        assertNull(GoalValidator.parse(""))
        assertNull(GoalValidator.parse("0"))
        assertNull(GoalValidator.parse("-5"))
        assertNull(GoalValidator.parse("abc"))
        assertNull(GoalValidator.parse("12.5"))
    }

    @Test
    fun `rejects values that overflow Int`() {
        assertNull(GoalValidator.parse("99999999999"))
    }
}
