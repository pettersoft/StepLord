package se.pettersoft.steplord

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import se.pettersoft.steplord.di.ApplicationScope
import se.pettersoft.steplord.notifications.DailySummaryScheduler
import se.pettersoft.steplord.notifications.StepNotifier
import se.pettersoft.steplord.sync.StepSyncWorker
import javax.inject.Inject

@HiltAndroidApp
class StepLordApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var stepNotifier: StepNotifier

    @Inject
    lateinit var dailySummaryScheduler: DailySummaryScheduler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        stepNotifier.createChannels()
        StepSyncWorker.schedule(this)
        // Alarms are also cleared when the app is force stopped, so make sure one is set
        applicationScope.launch { dailySummaryScheduler.reschedule() }
    }
}
