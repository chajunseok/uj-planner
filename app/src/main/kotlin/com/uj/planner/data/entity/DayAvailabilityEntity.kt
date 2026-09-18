package com.uj.planner.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uj.planner.domain.model.Slot

/** 요일별 배치 가능 시간대. 요일당 한 줄이다. */
@Entity(tableName = "day_availability")
data class DayAvailabilityEntity(
    @PrimaryKey val dayOfWeek: Int,
    val startMin: Int,
    val endMin: Int,
) {
    fun toSlot() = Slot(dayOfWeek, startMin, endMin)
}
