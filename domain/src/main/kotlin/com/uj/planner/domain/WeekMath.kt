package com.uj.planner.domain

import com.uj.planner.domain.model.FlexTaskSpec
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/** 이 시점 이전은 배치에 쓰지 않는다. [com.uj.planner.domain.model.ScheduleInput] 의 fromDay·fromMin 으로 들어간다. */
data class Cutoff(val fromDay: Int, val fromMin: Int)

/** 자정 기준 분. */
fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute

/** [date] 가 속한 주의 월요일. */
fun weekStartOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** [weekStart] 주의 일요일. */
fun weekEndOf(weekStart: LocalDate): LocalDate = weekStart.plusDays(6)

/**
 * "9월 셋째 주" 의 (월, 몇째 주). 주는 목요일이 속한 달의 것으로 친다 — 달이 바뀌는 주가 두 달에 걸쳐 세어지지 않는다.
 */
fun weekOfMonth(weekStart: LocalDate): Pair<Int, Int> {
    val thursday = weekStart.plusDays(3)
    return thursday.monthValue to (thursday.dayOfMonth - 1) / 7 + 1
}

/** [weekStart] 주를 [now] 시점에 배치할 때의 절단점. 이미 지나간 주라 배치할 곳이 없으면 null. */
fun cutoffFor(weekStart: LocalDate, now: LocalDateTime): Cutoff? {
    val thisWeek = weekStartOf(now.toLocalDate())
    return when {
        weekStart > thisWeek -> Cutoff(fromDay = 1, fromMin = 0)
        weekStart < thisWeek -> null
        else -> Cutoff(now.dayOfWeek.value, now.minuteOfDay())
    }
}

/**
 * 아직 자리를 못 받은 횟수만 남긴 배치 조건 목록. 다 채운 일정은 빠진다.
 *
 * @param fulfilled 일정 id → 이번 주에 이미 자리를 받은 횟수.
 */
fun remainingSpecs(tasks: List<FlexTaskSpec>, fulfilled: Map<Long, Int>): List<FlexTaskSpec> =
    tasks.mapNotNull { task ->
        val left = task.timesPerWeek - (fulfilled[task.id] ?: 0)
        if (left > 0) task.copy(timesPerWeek = left) else null
    }
