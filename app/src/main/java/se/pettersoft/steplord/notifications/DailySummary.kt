package se.pettersoft.steplord.notifications

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import se.pettersoft.steplord.data.repository.SettingsRepository
import se.pettersoft.steplord.data.repository.StepsRepository
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "DailySummary"
private const val SYNC_TIMEOUT_MS = 5_000L

/** Runs when the daily summary alarm fires. */
class DailySummary @Inject constructor(
    private val stepsRepository: StepsRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: StepNotifier,
    private val scheduler: DailySummaryScheduler,
) {
    suspend fun run() {
        // Schedule tomorrow's summary first, so a failure below doesn't stop future summaries
        scheduler.reschedule()

        val settings = settingsRepository.current()
        if (!settings.summaryEnabled || !notifier.canNotify()) return

        // Get the latest count, but show the stored one rather than nothing if syncing is slow
        try {
            withTimeoutOrNull(SYNC_TIMEOUT_MS) { stepsRepository.syncFromHistory() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Syncing before the daily summary failed", e)
        }
        notifier.showDailySummary(stepsRepository.stepsOn(LocalDate.now()), settings.dailyGoal)
    }
}
