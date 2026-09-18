package com.uj.planner.domain.model

/**
 * 스케줄러 입력.
 *
 * @param availability 요일별 배치 가능 구간.
 * @param existing 이미 자리를 차지한 배치. 피해 가며, 같은 일정이 있는 요일에는 그 일정을 또 넣지 않는다.
 * @param fromDay 이 요일 이전은 쓰지 않는다. 주중에 재배치할 때 지난 시간을 막는 용도다.
 * @param fromMin [fromDay] 당일에 이 시각 이전은 쓰지 않는다.
 */
data class ScheduleInput(
    val availability: List<Slot>,
    val fixed: List<FixedBlock>,
    val tasks: List<FlexTaskSpec>,
    val existing: List<PlannedSlot> = emptyList(),
    val fromDay: Int = 1,
    val fromMin: Int = 0,
) {
    init {
        require(fromDay in 1..7) { "fromDay 는 1..7 이어야 한다: $fromDay" }
        require(fromMin >= 0) { "fromMin 은 0 이상이어야 한다: $fromMin" }
        // 같은 id 가 둘이면 한 일정이 서로 다른 길이로 여러 번 배치된다.
        require(tasks.map { it.id }.distinct().size == tasks.size) { "tasks 에 중복 id 가 있다" }
    }
}
