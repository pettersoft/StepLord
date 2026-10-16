package se.pettersoft.steplord.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
abstract class UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 0")
    abstract fun observe(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 0")
    abstract suspend fun get(): UserSettings?

    @Upsert
    abstract suspend fun upsert(settings: UserSettings)

    @Transaction
    open suspend fun update(transform: (UserSettings) -> UserSettings) {
        upsert(transform(get() ?: UserSettings()))
    }

    /**
     * Returns true if the goal-reached notification should be shown for [date] given [steps],
     * and marks it as shown so later calls the same day return false.
     */
    @Transaction
    open suspend fun claimGoalNotification(date: LocalDate, steps: Int): Boolean {
        val settings = get() ?: UserSettings()
        if (!settings.goalNotificationEnabled || steps < settings.dailyGoal || settings.goalNotifiedOn == date) {
            return false
        }
        upsert(settings.copy(goalNotifiedOn = date))
        return true
    }
}
