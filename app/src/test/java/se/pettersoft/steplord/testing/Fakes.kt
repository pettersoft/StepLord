package se.pettersoft.steplord.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import se.pettersoft.steplord.data.history.StepHistory
import se.pettersoft.steplord.data.local.DailySteps
import se.pettersoft.steplord.data.local.DailyStepsDao
import se.pettersoft.steplord.data.local.StepCounterState
import se.pettersoft.steplord.data.local.UserSettings
import se.pettersoft.steplord.data.local.UserSettingsDao
import se.pettersoft.steplord.data.repository.StepsUpdatedListener
import se.pettersoft.steplord.data.sensor.StepSensor
import java.time.LocalDate

class FakeDailyStepsDao : DailyStepsDao() {
    val rows = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    private var state: StepCounterState? = null

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DailySteps>> =
        rows.map { map -> map.filterKeys { it in from..to }.toSortedMap().map { DailySteps(it.key, it.value) } }

    override fun observeFirstRecordedDate(): Flow<LocalDate?> =
        rows.map { map -> map.filterValues { it > 0 }.keys.minOrNull() }

    override fun observeStepsOn(date: LocalDate): Flow<Int?> = rows.map { it[date] }

    override suspend fun stepsOn(date: LocalDate): Int? = rows.value[date]

    override suspend fun upsert(entry: DailySteps) {
        rows.value = rows.value + (entry.date to entry.steps)
    }

    override suspend fun counterState(): StepCounterState? = state

    override suspend fun upsertCounterState(state: StepCounterState) {
        this.state = state
    }
}

class FakeUserSettingsDao(initial: UserSettings? = null) : UserSettingsDao() {
    val row = MutableStateFlow(initial)

    override fun observe(): Flow<UserSettings?> = row

    override suspend fun get(): UserSettings? = row.value

    override suspend fun upsert(settings: UserSettings) {
        row.value = settings
    }
}

object NoStepSensor : StepSensor {
    override val isAvailable = false
    override fun counterReadings(): Flow<Long> = emptyFlow()
}

class FakeStepHistory(
    override val isAvailable: Boolean,
    var totals: Map<LocalDate, Int> = emptyMap(),
) : StepHistory {
    var subscribed = false
    var requestedRange: ClosedRange<LocalDate>? = null

    override suspend fun subscribe() {
        subscribed = true
    }

    override suspend fun dailyTotals(from: LocalDate, to: LocalDate): Map<LocalDate, Int> {
        requestedRange = from..to
        return totals.filterKeys { it in from..to }
    }
}

class RecordingStepsUpdatedListener : StepsUpdatedListener {
    val updates = mutableListOf<Pair<LocalDate, Int>>()

    override suspend fun onStepsUpdated(date: LocalDate, steps: Int) {
        updates += date to steps
    }
}
