package se.pettersoft.steplord.ui.home

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

enum class ChartPeriod { WEEK, MONTH }

/** First day of a calendar week, which depends on the locale (Monday in Sweden, Sunday in the US). */
fun interface WeekStart {
    fun firstDayOfWeek(): DayOfWeek
}

class LocaleWeekStart @Inject constructor() : WeekStart {
    override fun firstDayOfWeek(): DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
}

/** A calendar week or month shown in the chart. */
data class PeriodRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = date in start..end
}

/** The calendar week (starting on [firstDayOfWeek]) or month that contains [date]. */
internal fun periodContaining(period: ChartPeriod, date: LocalDate, firstDayOfWeek: DayOfWeek): PeriodRange =
    when (period) {
        ChartPeriod.WEEK -> {
            val start = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            PeriodRange(start, start.plusDays(6))
        }
        ChartPeriod.MONTH -> {
            val start = date.withDayOfMonth(1)
            PeriodRange(start, start.plusMonths(1).minusDays(1))
        }
    }

/** A date inside the period before ([direction] -1) or after ([direction] 1) [range]. */
internal fun shiftedAnchor(period: ChartPeriod, range: PeriodRange, direction: Int): LocalDate =
    when (period) {
        ChartPeriod.WEEK -> range.start.plusWeeks(direction.toLong())
        ChartPeriod.MONTH -> range.start.plusMonths(direction.toLong())
    }

/** Older periods are only reachable when there are recorded steps before this one. */
internal fun canGoBack(range: PeriodRange, firstRecordedDate: LocalDate?): Boolean =
    firstRecordedDate != null && firstRecordedDate < range.start

/** Newer periods are reachable up to the one containing today, never into the future. */
internal fun canGoForward(range: PeriodRange, today: LocalDate): Boolean = range.end < today
