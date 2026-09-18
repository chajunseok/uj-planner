package com.uj.planner.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uj.planner.domain.model.FlexTaskSpec
import com.uj.planner.domain.model.Window

/** 앱이 배치해 주는 가변 일정. */
@Entity(tableName = "flex_task")
data class FlexTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val durationMin: Int,
    val timesPerWeek: Int,
    val priority: Int,
    val window: Window,
    val deadlineDay: Int? = null,
) {
    /** @param times 이번에 배치할 횟수. 기본은 주당 횟수 전부다. */
    fun toSpec(times: Int = timesPerWeek) = FlexTaskSpec(id, durationMin, times, priority, window, deadlineDay)
}
