package se.pettersoft.steplord.data.repository

import kotlinx.coroutines.test.runTest
import se.pettersoft.steplord.data.local.DailySteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.pettersoft.steplord.testing.FakeDailyStepsDao
import se.pettersoft.steplord.testing.FakeStepHistory
import se.pettersoft.steplord.testing.NoStepSensor
import se.pettersoft.steplord.testing.RecordingStepsUpdatedListener
import java.time.LocalDate

class StepsRepositoryTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val dao = FakeDailyStepsDao()
    private val listener = RecordingStepsUpdatedListener()

    @Test
    fun syncSubscribesAndMergesLastTenDaysWithoutLoweringStoredDays() = runTest {
        dao.upsert(DailySteps(today.minusDays(1), 9_000))
        val history = FakeStepHistory(
            isAvailable = true,
            totals = mapOf(today to 3_000, today.minusDays(1) to 8_000, today.minusDays(2) to 5_000),
        )

        StepsRepository(dao, NoStepSensor, history, listener).syncFromHistory(today)

        assertTrue(history.subscribed)
        assertEquals(listOf(today to 3_000), listener.updates)
        assertEquals(today.minusDays(9)..today, history.requestedRange)
        assertEquals(
            mapOf(today to 3_000, today.minusDays(1) to 9_000, today.minusDays(2) to 5_000),
            dao.rows.value,
        )
    }

    @Test
    fun syncDoesNothingWhenHistoryUnavailable() = runTest {
        val history = FakeStepHistory(isAvailable = false, totals = mapOf(today to 3_000))

        StepsRepository(dao, NoStepSensor, history, listener).syncFromHistory(today)

        assertFalse(history.subscribed)
        assertTrue(dao.rows.value.isEmpty())
    }
}
