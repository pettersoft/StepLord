package se.pettersoft.steplord.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import se.pettersoft.steplord.data.repository.SettingsRepository
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the daily summary with an inexact alarm. It needs no special permission and also
 * fires while the device is idle, at the cost of possibly arriving a few minutes late.
 */
@Singleton
class DailySummaryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Schedules the next summary according to the current settings, or cancels it if disabled. */
    suspend fun reschedule() {
        val settings = settingsRepository.current()
        if (settings.summaryEnabled) {
            val trigger = nextSummaryTrigger(ZonedDateTime.now(), settings.summaryTime)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.toInstant().toEpochMilli(),
                alarmIntent(),
            )
        } else {
            alarmManager.cancel(alarmIntent())
        }
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, DailySummaryAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** The next occurrence of [time] strictly after [now], in [now]'s time zone. */
internal fun nextSummaryTrigger(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
    val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
    return if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
}
