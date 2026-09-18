package com.uj.planner.ui

import com.uj.planner.domain.model.Window

/** 월요일부터. `DAY_NAMES[dayOfWeek - 1]`. */
val DAY_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")

val Window.label: String
    get() = when (this) {
        Window.MORNING -> "아침"
        Window.AFTERNOON -> "오후"
        Window.EVENING -> "저녁"
        Window.ANY -> "아무때나"
    }

/** 자정 기준 분 → "19:30". 24:00 을 넘은 값은 다음 날 시각으로 접는다(25:00 → "01:00"). 단 정확히 24:00 은 "24:00". */
fun formatTime(min: Int): String =
    if (min == 24 * 60) "24:00" else "%02d:%02d".format(min / 60 % 24, min % 60)

fun formatRange(startMin: Int, endMin: Int): String = "${formatTime(startMin)} – ${formatTime(endMin)}"

/** 90 → "1시간 30분". */
fun formatDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h == 0 -> "${m}분"
        m == 0 -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}

/** 53dp 열에 들어가는 이름. 공백을 빼고 앞 두 글자. */
fun abbreviate(title: String): String = title.replace(" ", "").take(2)
