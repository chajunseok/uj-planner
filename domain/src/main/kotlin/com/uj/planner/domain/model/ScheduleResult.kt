package com.uj.planner.domain.model

/** 가변 일정 [taskId] 가 놓인 자리. */
data class PlannedSlot(val taskId: Long, val dayOfWeek: Int, val startMin: Int, val endMin: Int)

/** 자리가 모자라 [missing] 회를 넣지 못한 가변 일정. */
data class Unplaced(val taskId: Long, val missing: Int)

/** 배치 실패는 예외가 아니라 [unplaced] 로 돌려준다 — 화면에 드러내야 하는 정보다. */
data class ScheduleResult(val planned: List<PlannedSlot>, val unplaced: List<Unplaced>)
