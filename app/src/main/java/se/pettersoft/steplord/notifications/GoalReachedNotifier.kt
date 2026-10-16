package se.pettersoft.steplord.notifications

import se.pettersoft.steplord.data.local.UserSettingsDao
import se.pettersoft.steplord.data.repository.StepsUpdatedListener
import java.time.LocalDate
import javax.inject.Inject

/** Shows the goal-reached notification the first time today's steps reach the daily goal. */
class GoalReachedNotifier @Inject constructor(
    private val settingsDao: UserSettingsDao,
    private val notifier: StepNotifier,
) : StepsUpdatedListener {
    override suspend fun onStepsUpdated(date: LocalDate, steps: Int) {
        // Check permission first so a blocked notification doesn't use up today's claim
        if (date != LocalDate.now() || !notifier.canNotify()) return
        if (settingsDao.claimGoalNotification(date, steps)) {
            notifier.showGoalReached(steps)
        }
    }
}
