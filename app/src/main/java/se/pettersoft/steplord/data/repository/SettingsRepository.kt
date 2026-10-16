package se.pettersoft.steplord.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.pettersoft.steplord.data.local.UnitSystem
import se.pettersoft.steplord.data.local.UserSettings
import se.pettersoft.steplord.data.local.UserSettingsDao
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dao: UserSettingsDao,
) {
    val settings: Flow<UserSettings> = dao.observe().map { it ?: UserSettings() }

    suspend fun current(): UserSettings = dao.get() ?: UserSettings()

    suspend fun setDailyGoal(goal: Int) = dao.update { it.copy(dailyGoal = goal) }

    suspend fun setSummaryEnabled(enabled: Boolean) = dao.update { it.copy(summaryEnabled = enabled) }

    suspend fun setSummaryTime(time: LocalTime) = dao.update { it.copy(summaryTime = time) }

    suspend fun setGoalNotificationEnabled(enabled: Boolean) =
        dao.update { it.copy(goalNotificationEnabled = enabled) }

    suspend fun setHeightCm(heightCm: Int) = dao.update { it.copy(heightCm = heightCm) }

    suspend fun setUnitSystem(unitSystem: UnitSystem) = dao.update { it.copy(unitSystem = unitSystem) }
}
