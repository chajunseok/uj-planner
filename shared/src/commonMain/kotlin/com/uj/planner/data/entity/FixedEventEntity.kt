package com.uj.planner.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uj.planner.domain.model.FixedBlock

/** 매주 반복되는 고정 일정. */
@Entity(tableName = "fixed_event")
data class FixedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dayOfWeek: Int,
    val startMin: Int,
    val durationMin: Int,
) {
    fun toBlock() = FixedBlock(dayOfWeek, startMin, durationMin)
}
