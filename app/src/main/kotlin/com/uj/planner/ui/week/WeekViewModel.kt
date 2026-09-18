package com.uj.planner.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.planner.data.PlannerRepository
import com.uj.planner.data.entity.FlexTaskEntity
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.domain.model.Slot
import com.uj.planner.ui.theme.TaskColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 그리드에 그릴 블록 하나. 고정 일정이면 [status]·[color] 가 null 이다. */
data class WeekBlock(
    val title: String,
    val dayOfWeek: Int,
    val startMin: Int,
    val endMin: Int,
    val fixedId: Long? = null,
    val placementId: Long? = null,
    val flexTaskId: Long? = null,
    val color: TaskColor? = null,
    val status: PlacementStatus? = null,
) {
    val isFixed get() = fixedId != null
    val key get() = if (isFixed) "f$fixedId" else "p$placementId"
}

data class UnplacedItem(val task: FlexTaskEntity, val missing: Int)

data class WeekUiState(
    val weekStart: LocalDate,
    val thisWeekStart: LocalDate,
    val blocks: List<WeekBlock>,
    val unplaced: List<UnplacedItem>,
    /** 고정·가변 일정이 하나도 없다. 처음 실행한 상태. */
    val isEmpty: Boolean,
    val gridStartMin: Int,
    val gridEndMin: Int,
) {
    val isThisWeek get() = weekStart == thisWeekStart
    val isPast get() = weekStart < thisWeekStart
}

private const val DEFAULT_GRID_START = 8 * 60
private const val DEFAULT_GRID_END = 24 * 60

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModel(private val repository: PlannerRepository) : ViewModel() {
    private val weekStart = MutableStateFlow(repository.currentWeekStart())

    val state: StateFlow<WeekUiState?> = weekStart.flatMapLatest { start ->
        combine(
            repository.observeFixedEvents(),
            repository.observeFlexTasks(),
            repository.observeWeek(start),
            repository.observeUnplaced(start),
            repository.observeAvailability(),
        ) { fixed, tasks, placements, unplaced, availability ->
            val tasksById = tasks.associateBy { it.id }
            val blocks = fixed.map {
                WeekBlock(it.title, it.dayOfWeek, it.startMin, it.startMin + it.durationMin, fixedId = it.id)
            } + placements.mapNotNull { p ->
                val task = tasksById[p.flexTaskId] ?: return@mapNotNull null
                WeekBlock(
                    task.title, p.date.dayOfWeek.value, p.startMin, p.endMin,
                    placementId = p.id, flexTaskId = task.id, color = TaskColor.of(task.colorIndex), status = p.status,
                )
            }
            val thisWeek = repository.currentWeekStart()
            val starts = availability.map { it.startMin } + blocks.map { it.startMin } + DEFAULT_GRID_START
            val ends = availability.map { it.endMin } + blocks.map { it.endMin } + DEFAULT_GRID_END
            WeekUiState(
                weekStart = start,
                thisWeekStart = thisWeek,
                // 못함·버림은 자리를 비운 것이라 그 위에 새 배치가 놓일 수 있다. 먼저 그려서 살아 있는 블록이 위로 오게 한다.
                blocks = blocks.sortedBy { it.status != PlacementStatus.MISSED && it.status != PlacementStatus.DROPPED },
                // 지나간 주는 다시 짤 수 없으니 못 넣은 횟수를 알려도 할 수 있는 일이 없다.
                unplaced = if (start < thisWeek) emptyList() else unplaced.mapNotNull { u ->
                    tasksById[u.taskId]?.let { UnplacedItem(it, u.missing) }
                },
                isEmpty = fixed.isEmpty() && tasks.isEmpty(),
                gridStartMin = starts.min() / 60 * 60,
                gridEndMin = (ends.max() + 59) / 60 * 60,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _freeSlots = MutableStateFlow<Map<Int, List<Slot>>>(emptyMap())

    /** 옮기는 중인 블록이 들어갈 수 있는 빈칸. 옮기는 중이 아니면 비어 있다. */
    val freeSlots: StateFlow<Map<Int, List<Slot>>> = _freeSlots.asStateFlow()

    init {
        viewModelScope.launch { weekStart.collect { repository.ensureWeekPlanned(it) } }
    }

    fun shiftWeek(delta: Long) {
        weekStart.value = weekStart.value.plusWeeks(delta)
    }

    fun goToThisWeek() {
        weekStart.value = repository.currentWeekStart()
    }

    fun markDone(placementId: Long) = resolve(placementId, PlacementStatus.DONE)

    fun drop(placementId: Long) = resolve(placementId, PlacementStatus.DROPPED)

    private fun resolve(placementId: Long, status: PlacementStatus) {
        viewModelScope.launch { repository.resolveMissed(mapOf(placementId to status)) }
    }

    /** 손으로 옮긴 자리까지 풀고 처음부터 다시 짠다. */
    fun replan() {
        viewModelScope.launch { repository.recomputeWeek(weekStart.value, resetPins = true) }
    }

    fun beginMove(placementId: Long) {
        viewModelScope.launch { _freeSlots.value = repository.freeSlotsForMove(placementId) }
    }

    /** @param target 놓은 자리(요일, 시작 분). 놓을 수 없는 곳이라 취소됐으면 null. */
    fun endMove(placementId: Long, target: Pair<Int, Int>?) {
        _freeSlots.value = emptyMap()
        if (target == null) return
        viewModelScope.launch { repository.movePlacement(placementId, target.first, target.second) }
    }
}
