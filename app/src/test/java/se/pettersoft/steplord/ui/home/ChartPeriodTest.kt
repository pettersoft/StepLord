package se.pettersoft.steplord.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ChartPeriodTest {
    // Thursday
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun weekStartsOnLocaleFirstDay() {
        assertEquals(
            PeriodRange(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)),
            periodContaining(ChartPeriod.WEEK, today, DayOfWeek.MONDAY),
        )
        assertEquals(
            PeriodRange(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 17)),
            periodContaining(ChartPeriod.WEEK, today, DayOfWeek.SUNDAY),
        )
    }

    @Test
    fun monthCoversWholeCalendarMonth() {
        assertEquals(
            PeriodRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            periodContaining(ChartPeriod.MONTH, today, DayOfWeek.MONDAY),
        )
        assertEquals(
            PeriodRange(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)),
            periodContaining(ChartPeriod.MONTH, LocalDate.of(2028, 2, 10), DayOfWeek.MONDAY),
        )
    }

    @Test
    fun shiftingMovesOnePeriod() {
        val week = periodContaining(ChartPeriod.WEEK, today, DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 10, 5), shiftedAnchor(ChartPeriod.WEEK, week, -1))
        val month = periodContaining(ChartPeriod.MONTH, today, DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 11, 1), shiftedAnchor(ChartPeriod.MONTH, month, 1))
    }

    @Test
    fun backOnlyWhenOlderStepsExist() {
        val week = periodContaining(ChartPeriod.WEEK, today, DayOfWeek.MONDAY)
        assertFalse(canGoBack(week, null))
        assertFalse(canGoBack(week, week.start))
        assertTrue(canGoBack(week, week.start.minusDays(1)))
    }

    @Test
    fun forwardOnlyUpToTheCurrentPeriod() {
        val current = periodContaining(ChartPeriod.WEEK, today, DayOfWeek.MONDAY)
        assertFalse(canGoForward(current, today))
        val previous = periodContaining(ChartPeriod.WEEK, today.minusWeeks(1), DayOfWeek.MONDAY)
        assertTrue(canGoForward(previous, today))
    }
}
