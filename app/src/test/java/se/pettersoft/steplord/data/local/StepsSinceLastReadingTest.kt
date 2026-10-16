package se.pettersoft.steplord.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StepsSinceLastReadingTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val boot = 1_792_000_000_000L
    private val state = StepCounterState(lastCounter = 5_000, lastDate = today, bootTimeMillis = boot)

    @Test
    fun firstReadingEverAddsNothing() {
        assertEquals(0, stepsSinceLastReading(null, 5_000, today, boot))
    }

    @Test
    fun sameDayAndBootAddsDifference() {
        assertEquals(120, stepsSinceLastReading(state, 5_120, today, boot))
    }

    @Test
    fun smallBootTimeJitterIsTheSameBoot() {
        assertEquals(120, stepsSinceLastReading(state, 5_120, today, boot + 2_000))
    }

    @Test
    fun rebootStartsNewBaseline() {
        assertEquals(0, stepsSinceLastReading(state, 40, today, boot + 3_600_000))
    }

    @Test
    fun restoredFromAnotherDeviceStartsNewBaseline() {
        // A larger counter on the new device must not be counted as steps
        assertEquals(0, stepsSinceLastReading(state, 80_000, today, boot - 86_400_000))
    }

    @Test
    fun firstReadingOfNewDayAddsNothing() {
        assertEquals(0, stepsSinceLastReading(state.copy(lastDate = today.minusDays(1)), 7_000, today, boot))
    }
}
