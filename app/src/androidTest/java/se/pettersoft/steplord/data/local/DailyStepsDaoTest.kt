package se.pettersoft.steplord.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class DailyStepsDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: DailyStepsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = db.dailyStepsDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsertReplacesExistingDayAndFiltersByDateRange() = runTest {
        val today = LocalDate.of(2026, 10, 15)
        dao.upsert(DailySteps(today.minusDays(10), 100))
        dao.upsert(DailySteps(today, 1_000))
        dao.upsert(DailySteps(today, 2_000))
        dao.upsert(DailySteps(today.plusDays(1), 300))

        dao.observeBetween(today.minusDays(6), today).test {
            assertEquals(listOf(DailySteps(today, 2_000)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun firstRecordedDateIgnoresDaysWithoutSteps() = runTest {
        val today = LocalDate.of(2026, 10, 15)
        dao.observeFirstRecordedDate().test {
            assertEquals(null, awaitItem())
            dao.upsert(DailySteps(today.minusDays(20), 0))
            dao.upsert(DailySteps(today.minusDays(5), 1_234))
            // Room emits on its own thread; the 0-step day may first emit null again
            var first = awaitItem()
            while (first == null) first = awaitItem()
            assertEquals(today.minusDays(5), first)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun counterReadingsAccumulateIntoToday() = runTest {
        val today = LocalDate.of(2026, 10, 15)
        dao.recordCounterReading(10_000, today, bootTimeMillis = 1_000L)
        dao.recordCounterReading(10_250, today, bootTimeMillis = 1_000L)
        dao.recordCounterReading(10_300, today, bootTimeMillis = 1_000L)

        assertEquals(300, dao.stepsOn(today))
    }

    @Test
    fun settingsRoundTripIncludingTimeAndNullableDate() = runTest {
        val settingsDao = db.userSettingsDao()
        val today = LocalDate.of(2026, 10, 15)
        // Defaults when nothing is stored yet
        settingsDao.update { it }
        assertEquals(175, settingsDao.get()?.heightCm)
        assertEquals(UnitSystem.METRIC, settingsDao.get()?.unitSystem)

        settingsDao.update {
            it.copy(summaryTime = LocalTime.of(20, 45), dailyGoal = 7_500, heightCm = 182, unitSystem = UnitSystem.IMPERIAL)
        }
        assertEquals(null, settingsDao.get()?.goalNotifiedOn)

        assertTrue(settingsDao.claimGoalNotification(today, 8_000))
        val stored = settingsDao.get()
        assertEquals(LocalTime.of(20, 45), stored?.summaryTime)
        assertEquals(7_500, stored?.dailyGoal)
        assertEquals(today, stored?.goalNotifiedOn)
        assertEquals(182, stored?.heightCm)
        assertEquals(UnitSystem.IMPERIAL, stored?.unitSystem)
    }

    @Test
    fun mergeDailyTotalsKeepsHigherStoredValue() = runTest {
        val today = LocalDate.of(2026, 10, 15)
        dao.upsert(DailySteps(today, 5_000))
        dao.mergeDailyTotals(mapOf(today to 4_000, today.minusDays(1) to 7_000))

        assertEquals(5_000, dao.stepsOn(today))
        assertEquals(7_000, dao.stepsOn(today.minusDays(1)))
    }
}
