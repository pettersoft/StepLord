package se.pettersoft.steplord.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class UnitSystem { METRIC, IMPERIAL }

/** The user's settings, stored locally as a single row. */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 0,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val summaryEnabled: Boolean = true,
    val summaryTime: LocalTime = DEFAULT_SUMMARY_TIME,
    val goalNotificationEnabled: Boolean = true,
    /** Day the goal-reached notification was last shown, so it is shown at most once per day. */
    val goalNotifiedOn: LocalDate? = null,
    /** Used to estimate stride length, and from that the distance walked. */
    @ColumnInfo(defaultValue = "175") val heightCm: Int = DEFAULT_HEIGHT_CM,
    @ColumnInfo(defaultValue = "METRIC") val unitSystem: UnitSystem = UnitSystem.METRIC,
) {
    companion object {
        const val DEFAULT_DAILY_GOAL = 10_000
        val DEFAULT_SUMMARY_TIME: LocalTime = LocalTime.of(21, 0)
        const val DEFAULT_HEIGHT_CM = 175
    }
}
