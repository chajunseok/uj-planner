package com.uj.planner.domain.model

/** 가변 일정 [taskId] 가 놓인 자리. */
data class PlannedSlot(val taskId: Long, val dayOfWeek: Int, val startMin: Int, val endMin: Int) {
    init {
        // 뒤집힌 구간이 existing 으로 들어오면 빈 구간 계산이 어긋나 이미 찬 시간에 겹쳐 배치된다.
        require(dayOfWeek in 1..7) { "dayOfWeek 는 1..7 이어야 한다: $dayOfWeek" }
        require(startMin in 0 until endMin) { "구간이 비었거나 뒤집혔다: $startMin..$endMin" }
    }
}

/** 자리가 모자라 [missing] 회를 넣지 못한 가변 일정. */
data class Unplaced(val taskId: Long, val missing: Int)

/** 배치 실패는 예외가 아니라 [unplaced] 로 돌려준다 — 화면에 드러내야 하는 정보다. */
data class ScheduleResult(val planned: List<PlannedSlot>, val unplaced: List<Unplaced>)
