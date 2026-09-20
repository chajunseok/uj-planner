package com.uj.planner.domain

import com.uj.planner.domain.model.FixedBlock
import com.uj.planner.domain.model.FlexTaskSpec
import com.uj.planner.domain.model.PlannedSlot
import com.uj.planner.domain.model.ScheduleInput
import com.uj.planner.domain.model.Slot
import com.uj.planner.domain.model.Unplaced
import com.uj.planner.domain.model.Window
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SchedulerTest {
    /** 매일 08:00~24:00. */
    private val fullWeek = (1..7).map { Slot(it, 8 * 60, 24 * 60) }

    private fun task(
        id: Long,
        durationMin: Int = 60,
        times: Int = 1,
        priority: Int = 2,
        window: Window = Window.ANY,
        deadline: Int? = null,
    ) = FlexTaskSpec(id, durationMin, times, priority, window, deadline)

    @Test
    fun `주 3회 일정은 서로 다른 3일에 놓인다`() {
        val result = schedule(ScheduleInput(fullWeek, emptyList(), listOf(task(1, times = 3))))

        assertEquals(3, result.planned.size)
        assertEquals(setOf(1, 2, 3), result.planned.map { it.dayOfWeek }.toSet())
        assertTrue(result.unplaced.isEmpty())
    }

    @Test
    fun `고정 일정이 막은 시간은 피한다`() {
        val fixed = listOf(FixedBlock(dayOfWeek = 1, startMin = 8 * 60, durationMin = 120))

        val result = schedule(ScheduleInput(fullWeek, fixed, listOf(task(1))))

        assertEquals(PlannedSlot(1, 1, 10 * 60, 11 * 60), result.planned.single())
    }

    @Test
    fun `선호 시간대 안에만 들어간다`() {
        val result = schedule(ScheduleInput(fullWeek, emptyList(), listOf(task(1, times = 2, window = Window.EVENING))))

        assertTrue(result.planned.all { it.startMin >= 18 * 60 })
    }

    @Test
    fun `선호 시간대가 꽉 차면 2차 패스에서 다른 시간대에 들어간다`() {
        val monday = listOf(Slot(1, 8 * 60, 24 * 60))
        val eveningFull = listOf(FixedBlock(1, 18 * 60, 6 * 60))

        val result = schedule(ScheduleInput(monday, eveningFull, listOf(task(1, window = Window.EVENING))))

        assertEquals(PlannedSlot(1, 1, 8 * 60, 9 * 60), result.planned.single())
        assertTrue(result.unplaced.isEmpty())
    }

    @Test
    fun `2차 패스는 뒷 순위 일정의 선호 자리를 빼앗지 않는다`() {
        // 월요일 08~10 아침 두 칸뿐. 앞 순위는 저녁 선호(자리 없음), 뒷 순위는 아침 선호 2시간.
        val monday = listOf(Slot(1, 8 * 60, 10 * 60))
        val eveningFirst = task(1, durationMin = 60, priority = 3, window = Window.EVENING)
        val morningLong = task(2, durationMin = 120, priority = 1, window = Window.MORNING)

        val result = schedule(ScheduleInput(monday, emptyList(), listOf(eveningFirst, morningLong)))

        assertEquals(listOf(PlannedSlot(2, 1, 8 * 60, 10 * 60)), result.planned)
        assertEquals(listOf(Unplaced(1, 1)), result.unplaced)
    }

    @Test
    fun `자리가 없으면 예외 없이 unplaced 로 돌려준다`() {
        val oneHour = listOf(Slot(1, 8 * 60, 9 * 60))

        val result = schedule(ScheduleInput(oneHour, emptyList(), listOf(task(1, times = 3))))

        assertEquals(1, result.planned.size)
        assertEquals(listOf(Unplaced(1, 2)), result.unplaced)
    }

    @Test
    fun `마감이 빠른 일정이 먼저 자리를 잡는다`() {
        val oneHour = listOf(Slot(1, 8 * 60, 9 * 60))
        val noDeadlineHighPriority = task(1, priority = 3)
        val dueMonday = task(2, priority = 1, deadline = 1)

        val result = schedule(ScheduleInput(oneHour, emptyList(), listOf(noDeadlineHighPriority, dueMonday)))

        assertEquals(2L, result.planned.single().taskId)
        assertEquals(listOf(Unplaced(1, 1)), result.unplaced)
    }

    @Test
    fun `마감이 같으면 우선순위가 높은 일정이 먼저 자리를 잡는다`() {
        val oneHour = listOf(Slot(1, 8 * 60, 9 * 60))

        val result = schedule(ScheduleInput(oneHour, emptyList(), listOf(task(1, priority = 1), task(2, priority = 3))))

        assertEquals(2L, result.planned.single().taskId)
    }

    @Test
    fun `마감 요일을 넘겨서는 배치하지 않는다`() {
        val result = schedule(ScheduleInput(fullWeek, emptyList(), listOf(task(1, times = 3, deadline = 2))))

        assertEquals(setOf(1, 2), result.planned.map { it.dayOfWeek }.toSet())
        assertEquals(listOf(Unplaced(1, 1)), result.unplaced)
    }

    @Test
    fun `기존 배치를 피하고 같은 일정이 있는 요일은 건너뛴다`() {
        val existing = listOf(
            PlannedSlot(taskId = 1, dayOfWeek = 1, startMin = 8 * 60, endMin = 9 * 60),
            PlannedSlot(taskId = 9, dayOfWeek = 2, startMin = 8 * 60, endMin = 9 * 60),
        )

        val result = schedule(ScheduleInput(fullWeek, emptyList(), listOf(task(1)), existing))

        // 월요일은 이미 같은 일정이 있어 건너뛰고, 화요일은 다른 일정이 08~09 를 차지해 09:00 부터다.
        assertEquals(PlannedSlot(1, 2, 9 * 60, 10 * 60), result.planned.single())
    }

    @Test
    fun `지난 시간에는 배치하지 않는다`() {
        val input = ScheduleInput(fullWeek, emptyList(), listOf(task(1)), fromDay = 3, fromMin = 14 * 60 + 10)

        val result = schedule(input)

        // 수요일 14:10 이후 첫 격자는 14:30 이다.
        assertEquals(PlannedSlot(1, 3, 14 * 60 + 30, 15 * 60 + 30), result.planned.single())
    }

    @Test
    fun `시작 시각은 30분 격자에 맞춘다`() {
        val fixed = listOf(FixedBlock(1, 8 * 60, 75)) // 09:15 에 끝난다

        val result = schedule(ScheduleInput(fullWeek, fixed, listOf(task(1))))

        assertEquals(9 * 60 + 30, result.planned.single().startMin)
    }

    @Test
    fun `자정을 넘기는 가용 시간의 심야도 저녁으로 친다`() {
        val saturdayLate = listOf(Slot(6, 24 * 60, 25 * 60))

        val result = schedule(ScheduleInput(saturdayLate, emptyList(), listOf(task(1, window = Window.EVENING))))

        assertEquals(PlannedSlot(1, 6, 24 * 60, 25 * 60), result.planned.single())
    }

    @Test
    fun `일정이 없으면 빈 결과를 돌려준다`() {
        val result = schedule(ScheduleInput(fullWeek, emptyList(), emptyList()))

        assertTrue(result.planned.isEmpty() && result.unplaced.isEmpty())
    }

    @Test
    fun `재배치 시작 요일이 마감보다 뒤면 예외 없이 unplaced 로 돌려준다`() {
        val input = ScheduleInput(fullWeek, emptyList(), listOf(task(1, times = 2, deadline = 2)), fromDay = 4)

        val result = schedule(input)

        assertTrue(result.planned.isEmpty())
        assertEquals(listOf(Unplaced(1, 2)), result.unplaced)
    }

    @Test
    fun `가용 시간 밖의 기존 배치는 빈 구간에 영향을 주지 않는다`() {
        val monday = listOf(Slot(1, 8 * 60, 10 * 60))
        val outside = listOf(PlannedSlot(taskId = 9, dayOfWeek = 1, startMin = 6 * 60, endMin = 7 * 60))

        val result = schedule(ScheduleInput(monday, emptyList(), listOf(task(1)), outside))

        assertEquals(PlannedSlot(1, 1, 8 * 60, 9 * 60), result.planned.single())
    }

    @Test
    fun `같은 요일의 가용 구간이 여러 개여도 겹쳐 배치하지 않는다`() {
        val split = listOf(Slot(1, 8 * 60, 9 * 60), Slot(1, 13 * 60, 14 * 60))
        val tasks = listOf(task(1), task(2), task(3))

        val result = schedule(ScheduleInput(split, emptyList(), tasks))

        assertEquals(setOf(8 * 60, 13 * 60), result.planned.map { it.startMin }.toSet())
        assertEquals(1, result.unplaced.size)
    }

    @Test
    fun `빈칸은 고정 일정과 기존 배치와 지난 시간을 뺀 나머지다`() {
        val input = ScheduleInput(
            availability = fullWeek,
            fixed = listOf(FixedBlock(dayOfWeek = 3, startMin = 9 * 60, durationMin = 9 * 60)),
            tasks = emptyList(),
            existing = listOf(PlannedSlot(1, 3, 20 * 60, 21 * 60)),
            fromDay = 3,
            fromMin = 8 * 60 + 30,
        )

        val free = freeSlots(input)

        assertEquals(setOf(3, 4, 5, 6, 7), free.keys)
        val wednesday = free.getValue(3)
        assertEquals(listOf(Slot(3, 8 * 60 + 30, 9 * 60), Slot(3, 18 * 60, 20 * 60), Slot(3, 21 * 60, 24 * 60)), wednesday)
        assertTrue(wednesday.fits(18 * 60, 20 * 60))
        assertTrue(!wednesday.fits(19 * 60 + 30, 20 * 60 + 30))
    }

    @Test
    fun `가변 일정의 말이 안 되는 조건은 생성 시점에 거부한다`() {
        assertFailsWith<IllegalArgumentException> { task(1, durationMin = 0) }
        assertFailsWith<IllegalArgumentException> { task(1, times = 8) }
        assertFailsWith<IllegalArgumentException> { task(1, priority = 4) }
        assertFailsWith<IllegalArgumentException> { task(1, deadline = 0) }
    }

    @Test
    fun `비었거나 뒤집힌 구간은 생성 시점에 거부한다`() {
        assertFailsWith<IllegalArgumentException> { Slot(1, 600, 600) }
        assertFailsWith<IllegalArgumentException> { FixedBlock(8, 0, 60) }
        assertFailsWith<IllegalArgumentException> { PlannedSlot(taskId = 1, dayOfWeek = 1, startMin = 700, endMin = 500) }
    }

    @Test
    fun `같은 id 의 일정이 둘 들어오면 거부한다`() {
        val sameId = listOf(task(1, durationMin = 60), task(1, durationMin = 90))

        assertFailsWith<IllegalArgumentException> { ScheduleInput(fullWeek, emptyList(), sameId) }
    }
}
