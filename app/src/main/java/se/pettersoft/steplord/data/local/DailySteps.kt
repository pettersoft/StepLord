package se.pettersoft.steplord.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey val date: LocalDate,
    val steps: Int,
)
