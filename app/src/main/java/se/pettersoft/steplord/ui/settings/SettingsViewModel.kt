package se.pettersoft.steplord.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.pettersoft.steplord.data.local.UnitSystem
import se.pettersoft.steplord.data.local.UserSettings
import se.pettersoft.steplord.data.repository.SettingsRepository
import se.pettersoft.steplord.notifications.DailySummaryScheduler
import se.pettersoft.steplord.ui.components.MAX_DAILY_GOAL
import se.pettersoft.steplord.ui.components.MIN_DAILY_GOAL
import se.pettersoft.steplord.util.MAX_HEIGHT_CM
import se.pettersoft.steplord.util.MIN_HEIGHT_CM
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dailySummaryScheduler: DailySummaryScheduler,
) : ViewModel() {

    /** Null until loaded from the database. */
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(goal.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)) }
    }

    fun setSummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSummaryEnabled(enabled)
            dailySummaryScheduler.reschedule()
        }
    }

    fun setSummaryTime(time: LocalTime) {
        viewModelScope.launch {
            settingsRepository.setSummaryTime(time)
            dailySummaryScheduler.reschedule()
        }
    }

    fun setGoalNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGoalNotificationEnabled(enabled) }
    }

    fun setHeightCm(heightCm: Int) {
        viewModelScope.launch { settingsRepository.setHeightCm(heightCm.coerceIn(MIN_HEIGHT_CM, MAX_HEIGHT_CM)) }
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch { settingsRepository.setUnitSystem(unitSystem) }
    }
}
