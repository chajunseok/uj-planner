package com.uj.planner.domain.model

/**
 * 가변 일정 하나의 배치 조건.
 *
 * @param timesPerWeek 이번 호출에서 배치할 횟수. 같은 일정은 하루 한 번이라 7 이 상한이다.
 * @param priority 클수록 먼저 자리를 잡는다.
 * @param deadlineDay 이 요일까지(포함) 배치해야 한다. 넘기면 배치하지 않고 실패로 돌려준다.
 */
data class FlexTaskSpec(
    val id: Long,
    val durationMin: Int,
    val timesPerWeek: Int,
    val priority: Int,
    val window: Window,
    val deadlineDay: Int? = null,
) {
    init {
        require(durationMin > 0) { "durationMin 은 양수여야 한다: $durationMin" }
        require(timesPerWeek in 1..7) { "timesPerWeek 는 1..7 이어야 한다: $timesPerWeek" }
        require(deadlineDay == null || deadlineDay in 1..7) { "deadlineDay 는 1..7 이어야 한다: $deadlineDay" }
    }
}
