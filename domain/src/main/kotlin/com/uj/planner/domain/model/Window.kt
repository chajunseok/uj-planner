package com.uj.planner.domain.model

/** 가변 일정의 선호 시간대. 분 단위는 자정 기준이다. 배치된 일정은 시작과 끝이 모두 이 범위 안에 든다. */
enum class Window(val startMin: Int, val endMin: Int) {
    MORNING(0, 12 * 60),
    AFTERNOON(12 * 60, 18 * 60),

    /** 끝을 열어 둔다 — 가용 시간이 자정을 넘기는 날(예: 25:00)의 심야도 저녁으로 친다. */
    EVENING(18 * 60, Int.MAX_VALUE),
    ANY(0, Int.MAX_VALUE),
}
