package com.uj.planner.domain

import com.uj.planner.domain.model.FlexTaskSpec
import com.uj.planner.domain.model.Window
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeekMathTest {
    private val monday = LocalDate.of(2026, 9, 14)
    private val fridayAfternoon = LocalDateTime.of(2026, 9, 18, 15, 20)

    private fun task(id: Long, times: Int) = FlexTaskSpec(id, 60, times, 2, Window.ANY)

    @Test
    fun `주의 시작은 월요일이다`() {
        assertEquals(monday, weekStartOf(LocalDate.of(2026, 9, 18)))
        assertEquals(monday, weekStartOf(monday))
        assertEquals(monday, weekStartOf(LocalDate.of(2026, 9, 20)))
    }

    @Test
    fun `이번 주의 절단점은 지금 요일과 시각이다`() {
        assertEquals(Cutoff(fromDay = 5, fromMin = 15 * 60 + 20), cutoffFor(monday, fridayAfternoon))
    }

    @Test
    fun `다음 주는 월요일 0시부터 쓴다`() {
        assertEquals(Cutoff(1, 0), cutoffFor(monday.plusWeeks(1), fridayAfternoon))
    }

    @Test
    fun `지나간 주는 배치할 곳이 없다`() {
        assertNull(cutoffFor(monday.minusWeeks(1), fridayAfternoon))
    }

    @Test
    fun `남은 횟수만 남기고 다 채운 일정은 뺀다`() {
        val tasks = listOf(task(1, times = 3), task(2, times = 2), task(3, times = 1))

        val left = remainingSpecs(tasks, fulfilled = mapOf(1L to 1, 2L to 2, 3L to 5))

        assertEquals(listOf(task(1, times = 2)), left)
    }
}
