package com.uj.planner.data

import androidx.room.TypeConverter
import java.time.LocalDate

/** enum 은 Room 이 이름 문자열로 직접 저장하므로 컨버터가 필요 없다. */
class Converters {
    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
}
