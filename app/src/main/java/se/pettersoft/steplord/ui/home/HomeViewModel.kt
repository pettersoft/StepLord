package se.pettersoft.steplord.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.pettersoft.steplord.data.local.UnitSystem
import se.pettersoft.steplord.data.local.UserSettings
import se.pettersoft.steplord.data.repository.SettingsRepository
import se.pettersoft.steplord.data.repository.StepsRepository
import se.pettersoft.steplord.ui.components.BarEntry
import se.pettersoft.steplord.ui.components.MAX_DAILY_GOAL
import se.pettersoft.steplord.ui.components.MIN_DAILY_GOAL
import se.pettersoft.steplord.util.CurrentDate
import se.pettersoft.steplord.util.estimateDistanceMeters
import se.pettersoft.steplord.util.estimateWalkingMinutes
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

private const val SAMPLE_DAYS = 60L
private const val SAMPLE_TODAY_STEPS = 7_559

data class ChartUiState(
    val period: ChartPeriod = ChartPeriod.WEEK,
    val range: PeriodRange = PeriodRange(LocalDate.now(), LocalDate.now()),
    val bars: List<BarEntry> = emptyList(),
    val total: Int = 0,
    /** Average over the days of the period up to today. */
    val dailyAverage: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
)

data class HomeUiState(
    val date: LocalDate = LocalDate.now(),
    val todaySteps: Int = 0,
    val goal: Int = UserSettings.DEFAULT_DAILY_GOAL,
    val heightCm: Int = UserSettings.DEFAULT_HEIGHT_CM,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val chart: ChartUiState = ChartUiState(),
) {
    val progress: Float get() = if (goal > 0) todaySteps.toFloat() / goal else 0f

    /** Approximate distance walked today, from steps and height. */
    val distanceMeters: Double get() = estimateDistanceMeters(todaySteps, heightCm)

    /** Approximate time spent walking today. */
    val walkingMinutes: Int get() = estimateWalkingMinutes(todaySteps)
}

/** What the chart shows. A null anchor follows today, so the current period stays current after midnight. */
private data class ChartSelection(val period: ChartPeriod = ChartPeriod.WEEK, val anchor: LocalDate? = null)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StepsRepository,
    private val settingsRepository: SettingsRepository,
    currentDate: CurrentDate,
    private val weekStart: WeekStart,
) : ViewModel() {

    val hasStepSensor: Boolean get() = repository.hasStepSensor

    private val selection = MutableStateFlow(ChartSelection())

    // Follows the current date, so a screen kept in memory overnight moves on to the new day
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(currentDate.observe(), selection, ::Pair)
        .flatMapLatest { (today, selection) ->
            val range = periodContaining(selection.period, selection.anchor ?: today, weekStart.firstDayOfWeek())
            combine(
                repository.observeStepsOn(today),
                repository.observeBetween(range.start, range.end),
                repository.observeFirstRecordedDate(),
                settingsRepository.settings,
            ) { todaySteps, rows, firstRecordedDate, settings ->
                HomeUiState(
                    date = today,
                    todaySteps = todaySteps,
                    goal = settings.dailyGoal,
                    heightCm = settings.heightCm,
                    unitSystem = settings.unitSystem,
                    chart = chartState(selection.period, range, today, rows.associate { it.date to it.steps }, firstRecordedDate),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectPeriod(period: ChartPeriod) {
        // Keep showing the same point in time, e.g. a week in September becomes September
        selection.update { current ->
            val anchor = current.anchor?.let { periodContaining(current.period, it, weekStart.firstDayOfWeek()).start }
            ChartSelection(period, anchor)
        }
    }

    fun showPreviousPeriod() = shiftPeriod(-1)

    fun showNextPeriod() = shiftPeriod(1)

    private fun shiftPeriod(direction: Int) {
        val chart = uiState.value.chart
        val today = uiState.value.date
        val allowed = if (direction < 0) chart.canGoBack else chart.canGoForward
        if (!allowed) return
        val anchor = shiftedAnchor(chart.period, chart.range, direction)
        val following = periodContaining(chart.period, anchor, weekStart.firstDayOfWeek())
        selection.update { it.copy(anchor = if (today in following) null else anchor) }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(goal.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)) }
    }

    /** Counts steps until cancelled; the screen runs this while it is visible. */
    suspend fun trackSteps() = repository.trackSteps()

    // Debug builds only, for trying the UI without walking around. Today gets a fixed count
    // part-way to the default goal, so screenshots show a partly filled ring.
    fun seedSampleData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            repository.setSteps(today, SAMPLE_TODAY_STEPS)
            for (offset in 1 until SAMPLE_DAYS) {
                repository.setSteps(today.minusDays(offset), Random.nextInt(2_000, 15_000))
            }
        }
    }

    private fun chartState(
        period: ChartPeriod,
        range: PeriodRange,
        today: LocalDate,
        stepsByDate: Map<LocalDate, Int>,
        firstRecordedDate: LocalDate?,
    ): ChartUiState {
        val days = generateSequence(range.start) { it.plusDays(1) }.takeWhile { it <= range.end }.toList()
        val total = days.sumOf { stepsByDate[it] ?: 0 }
        val daysSoFar = ChronoUnit.DAYS.between(range.start, minOf(range.end, today)) + 1
        return ChartUiState(
            period = period,
            range = range,
            bars = days.map { date ->
                BarEntry(
                    label = when (period) {
                        ChartPeriod.WEEK -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        ChartPeriod.MONTH -> date.dayOfMonth.toString()
                    },
                    value = stepsByDate[date] ?: 0,
                )
            },
            total = total,
            dailyAverage = if (daysSoFar > 0) (total / daysSoFar).toInt() else 0,
            canGoBack = canGoBack(range, firstRecordedDate),
            canGoForward = canGoForward(range, today),
        )
    }
}
