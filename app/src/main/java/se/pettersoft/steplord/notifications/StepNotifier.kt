package se.pettersoft.steplord.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import se.pettersoft.steplord.MainActivity
import se.pettersoft.steplord.R
import java.text.NumberFormat
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_DAILY_SUMMARY = "daily_summary"
private const val CHANNEL_GOAL_REACHED = "goal_reached"
private const val ID_DAILY_SUMMARY = 1
private const val ID_GOAL_REACHED = 2

@Singleton
class StepNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    /** False when the user hasn't granted POST_NOTIFICATIONS (API 33+) or blocked the app's notifications. */
    fun canNotify(): Boolean = manager.areNotificationsEnabled()

    /**
     * High importance so notifications show as a heads-up banner. Safe to call repeatedly, and
     * calling it again after a language change updates the channel names shown in system settings.
     */
    fun createChannels() {
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_DAILY_SUMMARY,
                    context.getString(R.string.channel_daily_summary),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = context.getString(R.string.channel_daily_summary_description) },
                NotificationChannel(
                    CHANNEL_GOAL_REACHED,
                    context.getString(R.string.channel_goal_reached),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = context.getString(R.string.channel_goal_reached_description) },
            ),
        )
    }

    fun showDailySummary(steps: Int, goal: Int) {
        val text = if (steps >= goal) {
            context.getString(R.string.notification_summary_text_reached, format(goal))
        } else {
            context.getString(R.string.notification_summary_text, steps * 100 / goal, format(goal))
        }
        notify(
            id = ID_DAILY_SUMMARY,
            channel = CHANNEL_DAILY_SUMMARY,
            title = context.resources.getQuantityString(R.plurals.notification_summary_title, steps, format(steps)),
            text = text,
            category = NotificationCompat.CATEGORY_REMINDER,
        )
    }

    fun showGoalReached(steps: Int) {
        notify(
            id = ID_GOAL_REACHED,
            channel = CHANNEL_GOAL_REACHED,
            title = context.getString(R.string.notification_goal_title),
            text = context.resources.getQuantityString(R.plurals.notification_goal_text, steps, format(steps)),
            category = NotificationCompat.CATEGORY_STATUS,
        )
    }

    @SuppressLint("MissingPermission") // Checked by canNotify()
    private fun notify(id: Int, channel: String, title: String, text: String, category: String) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(category)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun format(value: Int): String = NumberFormat.getIntegerInstance().format(value)
}
