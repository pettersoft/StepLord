package se.pettersoft.steplord.data.repository

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.pettersoft.steplord.data.history.StepHistory
import se.pettersoft.steplord.data.local.DailySteps
import se.pettersoft.steplord.data.local.DailyStepsDao
import se.pettersoft.steplord.data.sensor.StepSensor
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private const val TAG = "StepsRepository"

// The Recording API keeps 10 days of data
private const val HISTORY_DAYS = 10L
private val LIVE_SYNC_INTERVAL = 30.seconds

@Singleton
class StepsRepository @Inject constructor(
    private val dao: DailyStepsDao,
    private val stepSensor: StepSensor,
    private val stepHistory: StepHistory,
    private val stepsUpdatedListener: StepsUpdatedListener,
) {
    val hasStepSensor: Boolean get() = stepSensor.isAvailable

    fun observeStepsOn(date: LocalDate): Flow<Int> = dao.observeStepsOn(date).map { it ?: 0 }

    suspend fun stepsOn(date: LocalDate): Int = dao.stepsOn(date) ?: 0

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DailySteps>> = dao.observeBetween(from, to)

    fun observeFirstRecordedDate(): Flow<LocalDate?> = dao.observeFirstRecordedDate()

    /**
     * Copies the system step history into the database. Does nothing when the history isn't
     * available (no or outdated Google Play services). Throws if reading the history fails.
     */
    suspend fun syncFromHistory(today: LocalDate = LocalDate.now()) {
        if (!stepHistory.isAvailable) return
        stepHistory.subscribe()
        dao.mergeDailyTotals(stepHistory.dailyTotals(today.minusDays(HISTORY_DAYS - 1), today))
        notifyUpdated(today)
    }

    /**
     * Keeps today's total up to date while the app is visible, until cancelled. Uses the system
     * step history when available, otherwise counts directly from the sensor (which only counts
     * while the app is open).
     */
    suspend fun trackSteps() {
        if (stepHistory.isAvailable) {
            while (true) {
                try {
                    syncFromHistory()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Syncing step history failed", e)
                }
                delay(LIVE_SYNC_INTERVAL)
            }
        } else {
            stepSensor.counterReadings().collect { counter ->
                val today = LocalDate.now()
                val bootTimeMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()
                dao.recordCounterReading(counter, today, bootTimeMillis)
                notifyUpdated(today)
            }
        }
    }

    suspend fun setSteps(date: LocalDate, steps: Int) {
        dao.upsert(DailySteps(date, steps))
        notifyUpdated(date)
    }

    private suspend fun notifyUpdated(date: LocalDate) {
        stepsUpdatedListener.onStepsUpdated(date, stepsOn(date))
    }
}
