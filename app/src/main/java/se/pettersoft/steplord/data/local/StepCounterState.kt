package se.pettersoft.steplord.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Last raw value seen from the step counter sensor. The sensor reports total steps since
 * boot, so daily steps are accumulated from the difference between consecutive readings.
 */
@Entity(tableName = "step_counter_state")
data class StepCounterState(
    @PrimaryKey val id: Int = 0,
    val lastCounter: Long,
    val lastDate: LocalDate,
    /**
     * Wall-clock time of the boot the reading belongs to. A different value means the counter
     * restarted (reboot) or the database was restored onto another device.
     */
    @ColumnInfo(defaultValue = "0") val bootTimeMillis: Long = 0,
)
