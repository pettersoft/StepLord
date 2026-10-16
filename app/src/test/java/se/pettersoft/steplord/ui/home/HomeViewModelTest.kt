package se.pettersoft.steplord.ui.home

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.pettersoft.steplord.data.local.DailySteps
import se.pettersoft.steplord.data.local.UnitSystem
import se.pettersoft.steplord.data.local.UserSettings
import se.pettersoft.steplord.data.repository.SettingsRepository
import se.pettersoft.steplord.data.repository.StepsRepository
import se.pettersoft.steplord.testing.FakeDailyStepsDao
import se.pettersoft.steplord.testing.FakeStepHistory
import se.pettersoft.steplord.testing.FakeUserSettingsDao
import se.pettersoft.steplord.testing.NoStepSensor
import se.pettersoft.steplord.testing.RecordingStepsUpdatedListener
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    // Thursday; with Monday as first day the current week is 12-18 October
    private val day1 = LocalDate.of(2026, 10, 15)
    private val currentDate = MutableStateFlow(day1)
    private val dao = FakeDailyStepsDao()
    private val settingsDao = FakeUserSettingsDao(UserSettings(dailyGoal = 8_000))
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = HomeViewModel(
            StepsRepository(dao, NoStepSensor, FakeStepHistory(isAvailable = false), RecordingStepsUpdatedListener()),
            SettingsRepository(settingsDao),
            currentDate = { currentDate },
            weekStart = { DayOfWeek.MONDAY },
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun showsTodayAndCurrentCalendarWeek() = runTest {
        dao.upsert(DailySteps(day1, 4_200))
        dao.upsert(DailySteps(LocalDate.of(2026, 10, 12), 1_800))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(4_200, state.todaySteps)
            assertEquals(8_000, state.goal)
            assertEquals(0.525f, state.progress, 0.0001f)
            assertEquals(PeriodRange(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)), state.chart.range)
            assertEquals(listOf(1_800, 0, 0, 4_200, 0, 0, 0), state.chart.bars.map { it.value })
            assertEquals(6_000, state.chart.total)
            // Monday to Thursday so far
            assertEquals(1_500, state.chart.dailyAverage)
            assertFalse(state.chart.canGoForward)
            assertFalse(state.chart.canGoBack)
        }
    }

    @Test
    fun movesToTheNewDayAfterMidnight() = runTest {
        val day2 = day1.plusDays(1)
        dao.upsert(DailySteps(day1, 9_000))
        dao.upsert(DailySteps(day2, 150))

        viewModel.uiState.test {
            assertEquals(9_000, awaitItem().todaySteps)

            currentDate.value = day2
            val state = awaitItem()
            assertEquals(day2, state.date)
            assertEquals(150, state.todaySteps)
        }
    }

    @Test
    fun navigatesBackAndForwardBetweenWeeks() = runTest {
        dao.upsert(DailySteps(LocalDate.of(2026, 9, 30), 5_000))
        dao.upsert(DailySteps(day1, 3_000))

        viewModel.uiState.test {
            assertTrue(awaitItem().chart.canGoBack)

            viewModel.showPreviousPeriod()
            var chart = expectMostRecentItem().chart
            assertEquals(LocalDate.of(2026, 10, 5), chart.range.start)
            assertTrue(chart.canGoForward)
            // Data from 30 September is still older than this week
            assertTrue(chart.canGoBack)

            viewModel.showPreviousPeriod()
            chart = expectMostRecentItem().chart
            assertEquals(LocalDate.of(2026, 9, 28), chart.range.start)
            assertEquals(5_000, chart.total)
            assertFalse(chart.canGoBack)

            // Going back further is ignored when there is no older data
            viewModel.showPreviousPeriod()
            viewModel.showNextPeriod()
            viewModel.showNextPeriod()
            chart = expectMostRecentItem().chart
            assertEquals(LocalDate.of(2026, 10, 12), chart.range.start)
            assertFalse(chart.canGoForward)
        }
    }

    @Test
    fun switchingToMonthShowsTheMonthOfTheViewedWeek() = runTest {
        dao.upsert(DailySteps(LocalDate.of(2026, 9, 20), 7_000))
        dao.upsert(DailySteps(day1, 1_000))

        viewModel.uiState.test {
            awaitItem()
            // Back to the week of 5-11 October, then to the week of 28 September - 4 October
            viewModel.showPreviousPeriod()
            viewModel.showPreviousPeriod()
            viewModel.selectPeriod(ChartPeriod.MONTH)
            val chart = expectMostRecentItem().chart
            assertEquals(ChartPeriod.MONTH, chart.period)
            assertEquals(PeriodRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)), chart.range)
            assertEquals(30, chart.bars.size)
            assertEquals("20", chart.bars[19].label)
            assertEquals(7_000, chart.bars[19].value)
            // A past month counts all of its days in the average
            assertEquals(7_000 / 30, chart.dailyAverage)
        }
    }

    @Test
    fun estimatesDistanceAndTimeFromSettings() = runTest {
        dao.upsert(DailySteps(day1, 10_000))
        settingsDao.upsert(UserSettings(heightCm = 200, unitSystem = UnitSystem.IMPERIAL))

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(UnitSystem.IMPERIAL, state.unitSystem)
            assertEquals(8_300.0, state.distanceMeters, 0.01)
            assertEquals(100, state.walkingMinutes)
        }
    }

    @Test
    fun editingTheGoalUpdatesStateAndClampsToRange() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.setDailyGoal(12_000)
            assertEquals(12_000, expectMostRecentItem().goal)
            viewModel.setDailyGoal(50)
            assertEquals(1_000, expectMostRecentItem().goal)
        }
    }
}
