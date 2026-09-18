package com.uj.planner.domain

import com.uj.planner.domain.model.FlexTaskSpec
import com.uj.planner.domain.model.PlannedSlot
import com.uj.planner.domain.model.ScheduleInput
import com.uj.planner.domain.model.ScheduleResult
import com.uj.planner.domain.model.Slot
import com.uj.planner.domain.model.Unplaced
import com.uj.planner.domain.model.Window

/** 배치 시작 시각은 이 격자에 맞춘다. */
const val GRID_MIN = 30

/** 마감 빠른 순 → 우선순위 높은 순 → 긴 것 먼저. 긴 일정일수록 들어갈 자리가 적어서 먼저 넣는다. */
private val placementOrder: Comparator<FlexTaskSpec> =
    compareBy<FlexTaskSpec> { it.deadlineDay ?: Int.MAX_VALUE }
        .thenByDescending { it.priority }
        .thenByDescending { it.durationMin }

/**
 * 가변 일정을 빈 시간에 배치한다. 부수효과가 없는 순수 함수다.
 *
 * 1차 패스는 모든 일정을 선호 시간대 안에서만 넣고, 2차 패스에서 남은 것만 시간대 제약을 풀어 다시 넣는다.
 * 패스를 일정별이 아니라 전체로 나눈 것은, 앞 순위 일정의 차선책이 뒷 순위 일정의 선호 자리를 빼앗지 않게 하려는 것이다.
 *
 * ponytail: 그리디 first-fit 이다. 앞 요일부터 채우므로 일정이 주 초반에 몰린다 — 밀린 일정을 다시 넣을
 * 여유가 주 후반에 남는 효과가 있어 그대로 둔다. 요일 분산이 필요해지면 요일 순회 순서만 바꾸면 된다.
 * ponytail: 24:00 을 넘는 구간은 다음 요일의 일정과 겹치는지 검사하지 않는다. 심야 가용 시간과
 * 다음 날 이른 아침 일정이 함께 쓰이기 시작하면 요일 경계를 넘는 뺄셈을 추가한다.
 */
fun schedule(input: ScheduleInput): ScheduleResult {
    val free = freeSlotsByDay(input)
    val usedDays = input.existing
        .groupBy({ it.taskId }, { it.dayOfWeek })
        .mapValuesTo(mutableMapOf()) { it.value.toMutableSet() }
    val remaining = input.tasks.sortedWith(placementOrder).associateWithTo(LinkedHashMap()) { it.timesPerWeek }
    val planned = mutableListOf<PlannedSlot>()

    for (relaxed in listOf(false, true)) {
        for (entry in remaining) {
            val task = entry.key
            val window = if (relaxed) Window.ANY else task.window
            val days = usedDays.getOrPut(task.id) { mutableSetOf() }
            for (day in input.fromDay..(task.deadlineDay ?: 7)) {
                if (entry.value == 0) break
                if (day in days) continue
                val slot = free[day]?.take(task.durationMin, window) ?: continue
                planned += PlannedSlot(task.id, day, slot.startMin, slot.endMin)
                days += day
                entry.setValue(entry.value - 1)
            }
        }
    }
    val unplaced = remaining.filterValues { it > 0 }.map { (task, missing) -> Unplaced(task.id, missing) }
    return ScheduleResult(planned, unplaced)
}

/**
 * 지금 비어 있는 요일별 구간. 스케줄러가 자리를 찾을 때 쓰는 것과 같은 계산이라,
 * 사용자가 블록을 손으로 옮길 때 놓을 수 있는 자리를 이걸로 판정한다. [ScheduleInput.tasks] 는 보지 않는다.
 */
fun freeSlots(input: ScheduleInput): Map<Int, List<Slot>> = freeSlotsByDay(input)

/** `[startMin, endMin)` 이 빈 구간 하나에 통째로 들어가는가. */
fun List<Slot>.fits(startMin: Int, endMin: Int): Boolean = any { it.startMin <= startMin && endMin <= it.endMin }

/** 가용 시간에서 지난 시간·고정 일정·기존 배치를 뺀 요일별 빈 구간. 각 목록은 시작 시각 오름차순이다. */
private fun freeSlotsByDay(input: ScheduleInput): Map<Int, MutableList<Slot>> {
    val free = input.availability
        .filter { it.dayOfWeek >= input.fromDay }
        .sortedBy { it.startMin }
        .groupByTo(mutableMapOf()) { it.dayOfWeek }
    free[input.fromDay]?.subtract(0, input.fromMin)
    input.fixed.forEach { free[it.dayOfWeek]?.subtract(it.startMin, it.endMin) }
    input.existing.forEach { free[it.dayOfWeek]?.subtract(it.startMin, it.endMin) }
    return free
}

/** 목록의 모든 구간에서 `[start, end)` 를 잘라 낸다. */
private fun MutableList<Slot>.subtract(start: Int, end: Int) {
    val cut = flatMap { s ->
        if (end <= s.startMin || start >= s.endMin) {
            listOf(s)
        } else {
            buildList {
                if (start > s.startMin) add(s.copy(endMin = start))
                if (end < s.endMin) add(s.copy(startMin = end))
            }
        }
    }
    clear()
    addAll(cut)
}

/** [window] 안에서 [durationMin] 이 통째로 들어가는 가장 이른 자리를 찾아 차지하고 돌려준다. 없으면 null. */
private fun MutableList<Slot>.take(durationMin: Int, window: Window): Slot? {
    val found = firstNotNullOfOrNull { s ->
        val start = maxOf(s.startMin, window.startMin).roundUpToGrid()
        val end = start + durationMin
        if (end <= minOf(s.endMin, window.endMin)) Slot(s.dayOfWeek, start, end) else null
    } ?: return null
    subtract(found.startMin, found.endMin)
    return found
}

private fun Int.roundUpToGrid(): Int = (this + GRID_MIN - 1) / GRID_MIN * GRID_MIN
