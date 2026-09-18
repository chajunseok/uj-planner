package com.uj.planner.data

import androidx.room.withTransaction
import com.uj.planner.data.entity.DayAvailabilityEntity
import com.uj.planner.data.entity.FixedEventEntity
import com.uj.planner.data.entity.FlexTaskEntity
import com.uj.planner.data.entity.PlacementEntity
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.domain.Cutoff
import com.uj.planner.domain.cutoffFor
import com.uj.planner.domain.fits
import com.uj.planner.domain.freeSlots
import com.uj.planner.domain.minuteOfDay
import com.uj.planner.domain.model.FlexTaskSpec
import com.uj.planner.domain.model.PlannedSlot
import com.uj.planner.domain.model.ScheduleInput
import com.uj.planner.domain.model.ScheduleResult
import com.uj.planner.domain.model.Slot
import com.uj.planner.domain.model.Unplaced
import com.uj.planner.domain.remainingSpecs
import com.uj.planner.domain.schedule
import com.uj.planner.domain.weekEndOf
import com.uj.planner.domain.weekStartOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** 밀린 일정을 처리한 결과. [moved] 는 새로 잡힌 자리, [unplaced] 는 빈칸이 없어 못 옮긴 일정이다. */
data class ResolveResult(val moved: List<PlannedSlot>, val unplaced: List<Unplaced>)

/**
 * DAO 를 묶는 곳이자 **스케줄러를 호출하는 유일한 지점**이다.
 * 시간표가 바뀌는 경로는 [recomputeWeek]·[resolveMissed]·[movePlacement] 셋뿐이다.
 */
class PlannerRepository(
    private val db: PlannerDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val fixedEvents = db.fixedEvents()
    private val flexTasks = db.flexTasks()
    private val placements = db.placements()
    private val availability = db.availability()

    fun currentWeekStart(): LocalDate = weekStartOf(LocalDate.now(clock))

    fun observeFixedEvents(): Flow<List<FixedEventEntity>> = fixedEvents.observeAll()

    fun observeFlexTasks(): Flow<List<FlexTaskEntity>> = flexTasks.observeAll()

    fun observeAvailability(): Flow<List<DayAvailabilityEntity>> = availability.observeAll()

    fun observeWeek(weekStart: LocalDate): Flow<List<PlacementEntity>> =
        placements.observeBetween(weekStart, weekEndOf(weekStart))

    /** 자리를 못 받은 가변 일정. 저장하지 않고 그때그때 계산하므로 앱을 다시 열어도 사라지지 않는다. */
    fun observeUnplaced(weekStart: LocalDate): Flow<List<Unplaced>> =
        combine(flexTasks.observeAll(), observeWeek(weekStart)) { tasks, placed ->
            remainingSpecs(tasks.map { it.toSpec() }, placed.fulfilledCounts()).map { Unplaced(it.id, it.timesPerWeek) }
        }

    /** 끝났는데 아직 했음·못함·버림을 확인받지 못한 배치. */
    suspend fun overdue(): List<PlacementEntity> {
        val now = LocalDateTime.now(clock)
        return placements.getOverdue(now.toLocalDate(), now.minuteOfDay())
    }

    // 아래 쓰기는 배치 조건을 바꾸므로 같은 트랜잭션 안에서 이번 주를 다시 짠다.
    // 저장 전에 도메인 모델로 한 번 변환해 불변식을 검사한다. 이 클래스가 DB 의 유일한 작성자이므로
    // 여기서 막으면 잘못된 행이 생기지 않는다 — 한 행이라도 잘못되면 이후 모든 재계산이 실패하기 때문이다.

    /**
     * 고정 일정은 요일당 한 줄이라, 여러 요일을 고른 등록은 여러 줄이 된다.
     *
     * @param replacing 편집 중이던 줄들([fixedEventSeries]). 지우고 [events] 로 바꾼다.
     * @throws IllegalArgumentException 값이 도메인 불변식을 어길 때. 아무것도 저장하지 않는다.
     */
    suspend fun saveFixedEvents(events: List<FixedEventEntity>, replacing: List<FixedEventEntity> = emptyList()) =
        writeThenRecompute {
            events.forEach { it.toBlock() }
            replacing.forEach { fixedEvents.delete(it) }
            events.forEach { fixedEvents.upsert(it.copy(id = 0)) }
        }

    suspend fun deleteFixedEvents(events: List<FixedEventEntity>) =
        writeThenRecompute { events.forEach { fixedEvents.delete(it) } }

    /**
     * [id] 와 함께 등록된 줄들 — 이름·시작·길이가 같고 요일만 다른 것. 사용자에게는 이것이 일정 하나다.
     * 한 줄만 편집 대상으로 삼으면, 나머지 요일을 다시 고르는 순간 같은 줄이 두 번 생긴다.
     */
    suspend fun fixedEventSeries(id: Long): List<FixedEventEntity> {
        val all = fixedEvents.getAll()
        val one = all.find { it.id == id } ?: return emptyList()
        return all.filter { it.title == one.title && it.startMin == one.startMin && it.durationMin == one.durationMin }
    }

    /** @throws IllegalArgumentException 값이 도메인 불변식을 어길 때. 아무것도 저장하지 않는다. */
    suspend fun saveFlexTask(task: FlexTaskEntity) = writeThenRecompute {
        task.toSpec()
        // 길이나 조건이 바뀌었을 수 있으므로 손으로 옮겨 둔 자리도 풀고 다시 짠다.
        placements.unpinTask(task.id)
        flexTasks.upsert(task)
    }

    suspend fun deleteFlexTask(task: FlexTaskEntity) = writeThenRecompute { flexTasks.delete(task) }

    /** @throws IllegalArgumentException 값이 도메인 불변식을 어길 때. 아무것도 저장하지 않는다. */
    suspend fun saveAvailability(days: List<DayAvailabilityEntity>) = writeThenRecompute {
        days.forEach { it.toSlot() }
        availability.upsertAll(days)
    }

    /**
     * [task] 를 지금 저장하면 이번 주 어디에 놓일지. 저장할 때와 같은 계산을 쓰되 아무것도 쓰지 않는다.
     *
     * @throws IllegalArgumentException 값이 도메인 불변식을 어길 때.
     */
    suspend fun previewFlexTask(task: FlexTaskEntity): List<PlannedSlot> = db.withTransaction {
        val tasks = flexTasks.getAll().filter { it.id != task.id } + task
        val plan = planWeek(currentWeekStart(), LocalDateTime.now(clock), tasks) { it.flexTaskId == task.id }
        plan?.result?.planned.orEmpty().filter { it.taskId == task.id }
    }

    /**
     * [weekStart] 주의 아직 시작하지 않은 예정 배치를 지우고 남은 횟수를 다시 배치한다.
     * 완료·못함·버림, 이미 지나갔거나 진행 중인 배치, 손으로 옮긴 배치는 건드리지 않는다.
     * 단 손으로 옮긴 자리가 나중에 생긴 고정 일정이나 줄어든 가용 시간과 부딪히면 그 고정은 풀린다.
     *
     * @param resetPins true 면 손으로 옮긴 배치도 풀고 처음부터 다시 짠다.
     * @return 자리가 없어 못 넣은 일정. 지나간 주라면 아무것도 하지 않고 빈 목록을 돌려준다.
     */
    suspend fun recomputeWeek(
        weekStart: LocalDate = currentWeekStart(),
        resetPins: Boolean = false,
    ): List<Unplaced> = db.withTransaction {
        val plan = planWeek(weekStart, LocalDateTime.now(clock), flexTasks.getAll()) { resetPins }
            ?: return@withTransaction emptyList()
        placements.deleteByIds(plan.stale)
        placements.insertAll(plan.result.planned.map { PlacementEntity.from(it, weekStart) })
        plan.result.unplaced
    }

    /** 아직 한 번도 짜지 않은 주를 처음 볼 때 짠다. 새 주에 들어섰을 때와 다음 주를 미리 볼 때 쓴다. */
    suspend fun ensureWeekPlanned(weekStart: LocalDate) = db.withTransaction {
        if (placements.getBetween(weekStart, weekEndOf(weekStart)).isEmpty()) recomputeWeek(weekStart)
    }

    /** 배치 [placementId] 를 옮길 수 있는 요일별 빈칸. 옮길 수 없는 배치면 빈 맵. */
    suspend fun freeSlotsForMove(placementId: Long): Map<Int, List<Slot>> =
        db.withTransaction { moveContext(placementId)?.second.orEmpty() }

    /**
     * 사용자가 블록을 손으로 옮긴다. 옮긴 자리는 고정되어 이후 재계산에서도 유지된다.
     *
     * @return 그 자리가 비어 있지 않아 옮기지 못했으면 false.
     */
    suspend fun movePlacement(placementId: Long, dayOfWeek: Int, startMin: Int): Boolean = db.withTransaction {
        val (placement, free) = moveContext(placementId) ?: return@withTransaction false
        val endMin = startMin + placement.endMin - placement.startMin
        if (free[dayOfWeek]?.fits(startMin, endMin) != true) return@withTransaction false
        val date = weekStartOf(placement.date).plusDays(dayOfWeek - 1L)
        placements.update(placement.copy(date = date, startMin = startMin, endMin = endMin, pinned = true))
        true
    }

    /**
     * 밀린 일정에 대한 답을 반영한다. `못함` 만, 이번 주 남은 빈칸에, 이미 짜인 배치를 건드리지 않고 다시 넣는다.
     * 지난 주의 `못함` 은 이월하지 않는다 — 새 주는 주당 횟수를 새로 받으므로 이월하면 횟수를 넘긴다.
     * 아직 끝나지 않은 예정 배치에도 쓸 수 있다(미리 완료, 이번 주 버림).
     *
     * @param decisions 배치 id → [PlacementStatus.DONE]·[PlacementStatus.MISSED]·[PlacementStatus.DROPPED] 중 하나.
     */
    suspend fun resolveMissed(decisions: Map<Long, PlacementStatus>): ResolveResult = db.withTransaction {
        require(PlacementStatus.PLANNED !in decisions.values) { "PLANNED 는 밀린 일정에 대한 답이 아니다" }
        // 아직 답을 받지 않은 배치에만 적용한다. 같은 답이 두 번 들어와도 재배치가 두 번 생기지 않고,
        // 이미 완료·버림으로 확정된 배치를 덮어쓰지도 않는다.
        val pending = placements.getByIds(decisions.keys).filter { it.status == PlacementStatus.PLANNED }
        pending.forEach { placements.setStatus(it.id, decisions.getValue(it.id)) }

        // 시계는 한 번만 읽는다. 두 번 읽으면 주가 바뀌는 순간에 주 시작일과 절단점이 어긋난다.
        val now = LocalDateTime.now(clock)
        val weekStart = weekStartOf(now.toLocalDate())
        val missedCounts = pending
            .filter { decisions[it.id] == PlacementStatus.MISSED && it.date >= weekStart }
            .groupingBy { it.flexTaskId }
            .eachCount()
        if (missedCounts.isEmpty()) return@withTransaction ResolveResult(emptyList(), emptyList())

        val cutoff = checkNotNull(cutoffFor(weekStart, now)) { "이번 주에는 항상 절단점이 있다" }
        val specs = flexTasks.getAll().filter { it.id in missedCounts }.map { it.toSpec(times = missedCounts.getValue(it.id)) }
        val week = placements.getBetween(weekStart, weekEndOf(weekStart))
        val result = schedule(scheduleInput(cutoff, specs, week))
        placements.insertAll(result.planned.map { PlacementEntity.from(it, weekStart) })
        ResolveResult(result.planned, result.unplaced)
    }

    private suspend fun writeThenRecompute(write: suspend () -> Unit) {
        db.withTransaction {
            write()
            // 아직 오지 않은 주는 옛 조건으로 짜여 있다. 지워 두면 볼 때 새 조건으로 다시 짠다.
            placements.deletePlannedAfter(weekEndOf(currentWeekStart()))
            recomputeWeek()
        }
    }

    /** 다시 짠 결과. [stale] 은 새 배치로 바꿔 넣을 기존 배치의 id. */
    private class WeekPlan(val stale: List<Long>, val result: ScheduleResult)

    /**
     * [weekStart] 주를 [tasks] 로 다시 짜면 어떻게 되는지 계산만 한다. 지나간 주면 null.
     *
     * @param resetPins 이 배치의 고정을 풀고 다시 놓을 것인가.
     */
    private suspend fun planWeek(
        weekStart: LocalDate,
        now: LocalDateTime,
        tasks: List<FlexTaskEntity>,
        resetPins: (PlacementEntity) -> Boolean,
    ): WeekPlan? {
        val cutoff = cutoffFor(weekStart, now) ?: return null
        // 가용 시간에서 고정 일정만 뺀 것. 손으로 옮긴 자리가 아직 유효한지는 여기에 들어가는지로 본다.
        val open = freeSlots(scheduleInput(cutoff, emptyList(), emptyList()))
        val (stale, kept) = placements.getBetween(weekStart, weekEndOf(weekStart)).partition {
            val stillValid = open[it.date.dayOfWeek.value]?.fits(it.startMin, it.endMin) == true
            it.copy(pinned = it.pinned && stillValid && !resetPins(it)).isReplaceable(now.toLocalDate(), now.minuteOfDay())
        }
        val specs = remainingSpecs(tasks.map { it.toSpec() }, kept.fulfilledCounts())
        return WeekPlan(stale.map { it.id }, schedule(scheduleInput(cutoff, specs, kept)))
    }

    /** 옮길 배치와, 그 배치만 들어낸 상태의 빈칸. 예정 상태가 아니거나 지나간 주의 배치면 null. */
    private suspend fun moveContext(placementId: Long): Pair<PlacementEntity, Map<Int, List<Slot>>>? {
        val placement = placements.getByIds(listOf(placementId)).singleOrNull()
            ?.takeIf { it.status == PlacementStatus.PLANNED } ?: return null
        val weekStart = weekStartOf(placement.date)
        val cutoff = cutoffFor(weekStart, LocalDateTime.now(clock)) ?: return null
        val others = placements.getBetween(weekStart, weekEndOf(weekStart)).filter { it.id != placementId }
        return placement to freeSlots(scheduleInput(cutoff, emptyList(), others))
    }

    /** 예정·완료 배치가 자리를 차지한 것으로 본다. */
    private suspend fun scheduleInput(
        cutoff: Cutoff,
        specs: List<FlexTaskSpec>,
        weekPlacements: List<PlacementEntity>,
    ) = ScheduleInput(
        availability = availability.getAll().map { it.toSlot() },
        fixed = fixedEvents.getAll().map { it.toBlock() },
        tasks = specs,
        existing = weekPlacements
            .filter { it.status == PlacementStatus.PLANNED || it.status == PlacementStatus.DONE }
            .map { it.toPlanned() },
        fromDay = cutoff.fromDay,
        fromMin = cutoff.fromMin,
    )
}

/** 일정 id → 이번 주에 자리를 받은 횟수. `못함` 은 자리를 받지 못한 것으로 친다. */
private fun List<PlacementEntity>.fulfilledCounts(): Map<Long, Int> =
    filter { it.status != PlacementStatus.MISSED }.groupingBy { it.flexTaskId }.eachCount()
