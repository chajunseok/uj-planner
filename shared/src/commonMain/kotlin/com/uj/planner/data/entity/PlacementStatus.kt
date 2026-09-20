package com.uj.planner.data.entity

/** 배치 하나의 상태. Room 이 이름 문자열로 저장하므로 상수 이름을 바꾸면 기존 데이터가 깨진다. */
enum class PlacementStatus {
    PLANNED,
    DONE,

    /** 못 했다. 같은 주라면 남은 빈칸에 다시 배치된다. */
    MISSED,

    /** 이번 주는 포기했다. 다시 배치하지 않는다. */
    DROPPED,
}
