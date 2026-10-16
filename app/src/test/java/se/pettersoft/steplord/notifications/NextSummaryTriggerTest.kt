package se.pettersoft.steplord.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextSummaryTriggerTest {
    private val stockholm = ZoneId.of("Europe/Stockholm")
    private val nine = LocalTime.of(21, 0)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, stockholm)

    @Test
    fun laterTodayWhenBeforeTime() {
        assertEquals(at(2026, 10, 15, 21), nextSummaryTrigger(at(2026, 10, 15, 18), nine))
    }

    @Test
    fun tomorrowWhenAtOrAfterTime() {
        assertEquals(at(2026, 10, 16, 21), nextSummaryTrigger(at(2026, 10, 15, 21), nine))
        assertEquals(at(2026, 10, 16, 21), nextSummaryTrigger(at(2026, 10, 15, 22, 30), nine))
    }

    @Test
    fun keepsLocalTimeAcrossDaylightSavingChange() {
        // Clocks go back on 25 October 2026 in Stockholm
        val trigger = nextSummaryTrigger(at(2026, 10, 24, 22), nine)
        assertEquals(at(2026, 10, 25, 21), trigger)
        assertEquals(21, trigger.hour)
    }
}
