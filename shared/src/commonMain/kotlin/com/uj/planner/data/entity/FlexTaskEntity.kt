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
    /** 일정 색 8쌍 중 몇 번째인가. 색 자체는 UI 가 안다. */
    val colorIndex: Int = 0,
) {
    /** @param times 이번에 배치할 횟수. 기본은 주당 횟수 전부다. */
    fun toSpec(times: Int = timesPerWeek) = FlexTaskSpec(id, durationMin, times, priority, window, deadlineDay)
}
