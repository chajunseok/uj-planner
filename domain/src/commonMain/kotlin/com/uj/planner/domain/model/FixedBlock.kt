package com.uj.planner.domain.model

/** 사용자가 직접 넣은 고정 일정. 매주 반복되며 스케줄러는 이 구간을 피해 간다. */
data class FixedBlock(val dayOfWeek: Int, val startMin: Int, val durationMin: Int) {
    init {
        require(dayOfWeek in 1..7) { "dayOfWeek 는 1..7 이어야 한다: $dayOfWeek" }
        require(startMin >= 0 && durationMin > 0) { "시작은 0 이상, 길이는 양수여야 한다: $startMin, $durationMin" }
    }

    val endMin: Int get() = startMin + durationMin
}
