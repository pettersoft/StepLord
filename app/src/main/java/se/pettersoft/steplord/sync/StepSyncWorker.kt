package se.pettersoft.steplord.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import se.pettersoft.steplord.data.repository.StepsRepository
import se.pettersoft.steplord.util.hasActivityRecognitionPermission
import java.util.concurrent.TimeUnit

/**
 * Periodically copies the system step history into the app database. Runs every 15 minutes (the
 * minimum) so the goal-reached notification arrives reasonably soon after the goal is reached.
 */
@HiltWorker
class StepSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: StepsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Nothing is recorded until the user grants the permission in the app
        if (!applicationContext.hasActivityRecognitionPermission()) return Result.success()
        return try {
            repository.syncFromHistory()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Step sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "StepSyncWorker"
        private const val WORK_NAME = "step-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES).build()
            // UPDATE so a changed interval also applies to already scheduled work
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
