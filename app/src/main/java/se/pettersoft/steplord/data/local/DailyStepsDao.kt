package se.pettersoft.steplord.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.math.abs

@Dao
abstract class DailyStepsDao {
    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    abstract fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DailySteps>>

    /** The first day with any steps, or null if nothing has been recorded yet. */
    @Query("SELECT MIN(date) FROM daily_steps WHERE steps > 0")
    abstract fun observeFirstRecordedDate(): Flow<LocalDate?>

    @Query("SELECT steps FROM daily_steps WHERE date = :date")
    abstract fun observeStepsOn(date: LocalDate): Flow<Int?>

    @Query("SELECT steps FROM daily_steps WHERE date = :date")
    abstract suspend fun stepsOn(date: LocalDate): Int?

    @Upsert
    abstract suspend fun upsert(entry: DailySteps)

    @Query("SELECT * FROM step_counter_state WHERE id = 0")
    abstract suspend fun counterState(): StepCounterState?

    @Upsert
    abstract suspend fun upsertCounterState(state: StepCounterState)

    @Transaction
    open suspend fun recordCounterReading(counter: Long, today: LocalDate, bootTimeMillis: Long) {
        val added = stepsSinceLastReading(counterState(), counter, today, bootTimeMillis)
        if (added > 0) {
            upsert(DailySteps(today, (stepsOn(today) ?: 0) + added))
        }
        upsertCounterState(StepCounterState(lastCounter = counter, lastDate = today, bootTimeMillis = bootTimeMillis))
    }

    /** Stores daily totals from the system step history, never lowering an already stored day. */
    @Transaction
    open suspend fun mergeDailyTotals(totals: Map<LocalDate, Int>) {
        for ((date, steps) in totals) {
            if (steps > (stepsOn(date) ?: 0)) upsert(DailySteps(date, steps))
        }
    }
}

// Boot time is derived from the wall clock, so allow for small differences between readings
private const val BOOT_TIME_TOLERANCE_MS = 60_000L

/**
 * Steps to add to [today] for a new raw sensor reading. The reading only becomes a new baseline
 * (adding nothing) when there is no comparable previous reading:
 * - none stored yet
 * - the previous one is from another boot, where the counter restarted, or from another device
 *   after a backup restore
 * - the previous one is from an earlier day, since the steps in between can't be attributed to a day
 */
internal fun stepsSinceLastReading(
    state: StepCounterState?,
    counter: Long,
    today: LocalDate,
    bootTimeMillis: Long,
): Int = when {
    state == null -> 0
    abs(state.bootTimeMillis - bootTimeMillis) > BOOT_TIME_TOLERANCE_MS -> 0
    state.lastDate != today -> 0
    counter < state.lastCounter -> 0
    else -> (counter - state.lastCounter).toInt()
}
