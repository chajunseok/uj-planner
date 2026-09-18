package com.uj.planner.domain.model

/** 한 요일 안의 시간 구간 `[startMin, endMin)`. 요일은 월=1 … 일=7, 분은 자정 기준이며 24:00 을 넘을 수 있다. */
data class Slot(val dayOfWeek: Int, val startMin: Int, val endMin: Int) {
    init {
        require(dayOfWeek in 1..7) { "dayOfWeek 는 1..7 이어야 한다: $dayOfWeek" }
        require(startMin in 0 until endMin) { "구간이 비었거나 뒤집혔다: $startMin..$endMin" }
    }
}
