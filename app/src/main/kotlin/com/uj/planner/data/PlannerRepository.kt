package com.uj.planner.data

import androidx.room.withTransaction
import com.uj.planner.data.entity.DayAvailabilityEntity
import com.uj.planner.data.entity.FixedEventEntity
import com.uj.planner.data.entity.FlexTaskEntity
import com.uj.planner.data.entity.PlacementEntity
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.domain.Cutoff
import com.uj.planner.domain.cutoffFor
import com.uj.planner.domain.minuteOfDay
import com.uj.planner.domain.model.FlexTaskSpec
import com.uj.planner.domain.model.PlannedSlot
import com.uj.planner.domain.model.ScheduleInput
import com.uj.planner.domain.model.ScheduleResult
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
 * 시간표가 바뀌는 경로는 [recomputeWeek] 와 [resolveMissed] 둘뿐이다.
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

    /** @throws IllegalArgumentException 값이 도메인 불변식을 어길 때. 아무것도 저장하지 않는다. */
    suspend fun saveFixedEvent(event: FixedEventEntity) = writeThenRecompute {
        event.toBlock()
        fixedEvents.upsert(event)
    }

    suspend fun deleteFixedEvent(event: FixedEventEntity) = writeThenRecompute { fixedEvents.delete(event) }

    /** @throws IllegalArgumentException 값이 도메인 불변식을 어길 때. 아무것도 저장하지 않는다. */
    suspend fun saveFlexTask(task: FlexTaskEntity) = writeThenRecompute {
        task.toSpec()
        flexTasks.upsert(task)
    }

    suspend fun deleteFlexTask(task: FlexTaskEntity) = writeThenRecompute { flexTasks.delete(task) }

    /** @throws IllegalArgumentException 값이 도메인 불변식을 어길 때. 아무것도 저장하지 않는다. */
    suspend fun saveAvailability(days: List<DayAvailabilityEntity>) = writeThenRecompute {
        days.forEach { it.toSlot() }
        availability.upsertAll(days)
    }

    /**
     * [weekStart] 주의 아직 시작하지 않은 예정 배치를 지우고 남은 횟수를 다시 배치한다.
     * 완료·못함·버림과 이미 지나갔거나 진행 중인 배치는 건드리지 않는다.
     *
     * @return 자리가 없어 못 넣은 일정. 지나간 주라면 아무것도 하지 않고 빈 목록을 돌려준다.
     */
    suspend fun recomputeWeek(weekStart: LocalDate = currentWeekStart()): List<Unplaced> = db.withTransaction {
        val now = LocalDateTime.now(clock)
        val cutoff = cutoffFor(weekStart, now) ?: return@withTransaction emptyList()
        val weekEnd = weekEndOf(weekStart)
        placements.deleteUpcomingPlanned(weekStart, weekEnd, now.toLocalDate(), now.minuteOfDay())
        val kept = placements.getBetween(weekStart, weekEnd)
        val specs = remainingSpecs(flexTasks.getAll().map { it.toSpec() }, kept.fulfilledCounts())
        placeInto(weekStart, cutoff, specs, kept).unplaced
    }

    /**
     * 밀린 일정에 대한 답을 반영한다. `못함` 만, 이번 주 남은 빈칸에, 이미 짜인 배치를 건드리지 않고 다시 넣는다.
     * 지난 주의 `못함` 은 이월하지 않는다 — 새 주는 주당 횟수를 새로 받으므로 이월하면 횟수를 넘긴다.
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
        val result = placeInto(weekStart, cutoff, specs, week)
        ResolveResult(result.planned, result.unplaced)
    }

    private suspend fun writeThenRecompute(write: suspend () -> Unit) {
        db.withTransaction {
            write()
            recomputeWeek()
        }
    }

    /** [specs] 를 배치해 저장한다. 예정·완료 배치가 자리를 차지한 것으로 본다. */
    private suspend fun placeInto(
        weekStart: LocalDate,
        cutoff: Cutoff,
        specs: List<FlexTaskSpec>,
        weekPlacements: List<PlacementEntity>,
    ): ScheduleResult {
        val occupied = weekPlacements
            .filter { it.status == PlacementStatus.PLANNED || it.status == PlacementStatus.DONE }
            .map { it.toPlanned() }
        val input = ScheduleInput(
            availability = availability.getAll().map { it.toSlot() },
            fixed = fixedEvents.getAll().map { it.toBlock() },
            tasks = specs,
            existing = occupied,
            fromDay = cutoff.fromDay,
            fromMin = cutoff.fromMin,
        )
        return schedule(input).also { result ->
            placements.insertAll(result.planned.map { PlacementEntity.from(it, weekStart) })
        }
    }
}

/** 일정 id → 이번 주에 자리를 받은 횟수. `못함` 은 자리를 받지 못한 것으로 친다. */
private fun List<PlacementEntity>.fulfilledCounts(): Map<Long, Int> =
    filter { it.status != PlacementStatus.MISSED }.groupingBy { it.flexTaskId }.eachCount()
