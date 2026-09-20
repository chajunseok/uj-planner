package com.uj.planner.domain

import com.uj.planner.domain.model.FlexTaskSpec
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** 이 시점 이전은 배치에 쓰지 않는다. [com.uj.planner.domain.model.ScheduleInput] 의 fromDay·fromMin 으로 들어간다. */
data class Cutoff(val fromDay: Int, val fromMin: Int)

/** 자정 기준 분. */
fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute

/**
 * 시스템 시계가 가리키는 지금.
 *
 * kotlinx-datetime 은 시계와 시간대가 따로다. 둘을 엮는 자리를 여기 하나로 모아,
 * 저장소와 화면이 서로 다른 시간대로 "오늘"을 계산하는 일이 없게 한다.
 */
fun nowLocalDateTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/**
 * [date] 가 속한 주의 월요일.
 *
 * kotlinx-datetime 에는 java.time 의 TemporalAdjusters 같은 것이 없어서 직접 센다.
 * isoDayNumber 는 월=1 … 일=7 이라 1 을 빼면 월요일로부터 지난 날수가 된다.
 */
fun weekStartOf(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

/** [weekStart] 주의 일요일. */
fun weekEndOf(weekStart: LocalDate): LocalDate = weekStart.plus(6, DateTimeUnit.DAY)

/**
 * "9월 셋째 주" 의 (월, 몇째 주). 주는 목요일이 속한 달의 것으로 친다 — 달이 바뀌는 주가 두 달에 걸쳐 세어지지 않는다.
 */
fun weekOfMonth(weekStart: LocalDate): Pair<Int, Int> {
    val thursday = weekStart.plus(3, DateTimeUnit.DAY)
    return thursday.month.number to (thursday.day - 1) / 7 + 1
}

/** [weekStart] 주를 [now] 시점에 배치할 때의 절단점. 이미 지나간 주라 배치할 곳이 없으면 null. */
fun cutoffFor(weekStart: LocalDate, now: LocalDateTime): Cutoff? {
    val thisWeek = weekStartOf(now.date)
    return when {
        weekStart > thisWeek -> Cutoff(fromDay = 1, fromMin = 0)
        weekStart < thisWeek -> null
        else -> Cutoff(now.dayOfWeek.isoDayNumber, now.minuteOfDay())
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
