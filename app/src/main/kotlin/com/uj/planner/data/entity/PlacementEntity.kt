package com.uj.planner.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.uj.planner.domain.model.PlannedSlot
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/**
 * 가변 일정이 실제로 놓인 자리. 조회할 때마다 계산하지 않고 저장해 둔다 —
 * 그래야 재배치할 때 이미 짜인 자리를 피해 갈 수 있다.
 *
 * [startMin]·[endMin] 은 24:00 을 넘을 수 있고, 그래도 [date] 는 시작한 요일의 날짜다.
 */
@Entity(
    tableName = "placement",
    foreignKeys = [
        ForeignKey(
            entity = FlexTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["flexTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("flexTaskId"), Index("date")],
)
data class PlacementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flexTaskId: Long,
    val date: LocalDate,
    val startMin: Int,
    val endMin: Int,
    val status: PlacementStatus = PlacementStatus.PLANNED,
    /** 사용자가 손으로 옮긴 자리. 조건이 바뀌어 다시 짤 때도 지우지 않는다. */
    val pinned: Boolean = false,
) {
    /** 다시 짤 때 지우고 새로 놓아도 되는 배치 — 아직 시작하지 않았고 사용자가 손대지 않은 예정. */
    fun isReplaceable(today: LocalDate, nowMin: Int): Boolean =
        status == PlacementStatus.PLANNED && !pinned && (date > today || (date == today && startMin >= nowMin))

    fun toPlanned() = PlannedSlot(flexTaskId, date.dayOfWeek.isoDayNumber, startMin, endMin)

    companion object {
        fun from(slot: PlannedSlot, weekStart: LocalDate) = PlacementEntity(
            flexTaskId = slot.taskId,
            date = weekStart.plus(slot.dayOfWeek - 1L, DateTimeUnit.DAY),
            startMin = slot.startMin,
            endMin = slot.endMin,
        )
    }
}
