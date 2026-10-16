package se.pettersoft.steplord.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import se.pettersoft.steplord.di.ApplicationScope
import javax.inject.Inject

/** Fired by AlarmManager at the configured summary time. */
@AndroidEntryPoint
class DailySummaryAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dailySummary: DailySummary

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        launchAsync(scope) { dailySummary.run() }
    }
}

/** Alarms are cleared on reboot and app updates, and should follow time and time zone changes. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {
    @Inject
    lateinit var scheduler: DailySummaryScheduler

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> launchAsync(scope) { scheduler.reschedule() }
        }
    }
}

/** Keeps the receiver alive while [block] runs (the system allows about 10 seconds). */
private fun BroadcastReceiver.launchAsync(scope: CoroutineScope, block: suspend () -> Unit) {
    val pendingResult = goAsync()
    scope.launch {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}
