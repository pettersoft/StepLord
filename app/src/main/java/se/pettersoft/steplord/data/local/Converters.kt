package se.pettersoft.steplord.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun fromMinuteOfDay(value: Int): LocalTime = LocalTime.ofSecondOfDay(value * 60L)

    @TypeConverter
    fun toMinuteOfDay(time: LocalTime): Int = time.hour * 60 + time.minute
}
