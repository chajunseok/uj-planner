package com.uj.planner.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.planner.data.PlannerRepository
import com.uj.planner.data.entity.FlexTaskEntity
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.domain.minuteOfDay
import com.uj.planner.ui.formatDuration
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.theme.TaskColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** 오늘 타임라인의 한 줄. 고정 일정이면 [color]·[status] 가 null 이다. */
data class TodayItem(
    val title: String,
    val startMin: Int,
    val endMin: Int,
    val color: TaskColor? = null,
    val status: PlacementStatus? = null,
    val isNow: Boolean = false,
)

/** 커버와 플렉스 아래쪽이 보여 주는 카드 하나. 누를 것이 없는 카드는 [placementId] 가 null 이다. */
data class Focus(
    val kind: Kind,
    val kicker: String,
    val title: String,
    val sub: String,
    val color: TaskColor? = null,
    val placementId: Long? = null,
) {
    enum class Kind(val primaryLabel: String?, val hasMissed: Boolean) {
        /** 끝났는데 아직 답을 못 받은 일정. 가장 먼저 묻는다. */
        OVERDUE("했음", true),
        NOW("완료", true),
        NEXT("미리 완료", false),
        ALL_DONE(null, false),
        EMPTY(null, false),
    }
}

data class TodayUiState(val now: LocalDateTime, val items: List<TodayItem>, val remaining: Int, val focus: Focus)

/**
 * "지금 무엇을 할 차례인가". 커버 화면과 플렉스 모드가 같은 판정을 쓰도록 한곳에서 계산한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val repository: PlannerRepository) : ViewModel() {
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000)
        }
    }

    val state: StateFlow<TodayUiState?> = combine(
        clock,
        repository.observeFixedEvents(),
        repository.observeFlexTasks(),
        repository.observeWeek(repository.currentWeekStart()),
    ) { now, fixed, tasks, placements ->
        val today = now.toLocalDate()
        val nowMin = now.minuteOfDay()
        val tasksById = tasks.associateBy { it.id }
        val flexToday = placements.filter { it.date == today }.mapNotNull { p ->
            val task = tasksById[p.flexTaskId] ?: return@mapNotNull null
            val item = TodayItem(
                task.title, p.startMin, p.endMin, TaskColor.of(task.colorIndex), p.status,
                isNow = p.status == PlacementStatus.PLANNED && nowMin in p.startMin until p.endMin,
            )
            p.id to item
        }
        val items = (fixed.filter { it.dayOfWeek == today.dayOfWeek.value }
            .map { TodayItem(it.title, it.startMin, it.startMin + it.durationMin) } + flexToday.map { it.second })
            .sortedBy { it.startMin }
        val tomorrowFirst = placements
            .filter { it.date == today.plusDays(1) && it.status == PlacementStatus.PLANNED }
            .minByOrNull { it.startMin }
            ?.let { p -> tasksById[p.flexTaskId]?.let { "내일 첫 일정: ${formatTime(p.startMin)} ${it.title}" } }
        Snapshot(now, items, flexToday, tomorrowFirst.orEmpty(), tasksById)
    }.mapLatest { s ->
        // 밀린 일정의 판정은 Repository 의 것을 그대로 쓴다. 같은 조건을 여기에 다시 적지 않는다.
        val overdue = repository.overdue().firstOrNull()
        val overdueTask = overdue?.let { s.tasksById[it.flexTaskId] }
        TodayUiState(
            now = s.now,
            items = s.items,
            remaining = s.flexToday.count { it.second.status == PlacementStatus.PLANNED && it.second.endMin > s.now.minuteOfDay() },
            focus = when {
                overdue != null && overdueTask != null -> Focus(
                    Focus.Kind.OVERDUE,
                    kicker = "${formatRange(overdue.startMin, overdue.endMin)} · 지났어요",
                    title = overdueTask.title,
                    sub = "못함을 누르면 이번 주 빈칸에 다시 넣어요",
                    color = TaskColor.of(overdueTask.colorIndex),
                    placementId = overdue.id,
                )
                else -> focusOf(s)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun answer(placementId: Long, status: PlacementStatus) {
        viewModelScope.launch {
            try {
                // 커버에서는 빈칸이 없어도 묻지 않는다. 못함으로 남고, 펼쳐서 주간 화면의 점선 블록으로 보인다.
                repository.resolveMissed(mapOf(placementId to status))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 답이 반영되지 않았을 뿐이고 카드는 그대로 남는다. 다시 누르면 된다.
            }
        }
    }

    private class Snapshot(
        val now: LocalDateTime,
        val items: List<TodayItem>,
        val flexToday: List<Pair<Long, TodayItem>>,
        val tomorrowFirst: String,
        val tasksById: Map<Long, FlexTaskEntity>,
    )

    private fun focusOf(s: Snapshot): Focus {
        val nowMin = s.now.minuteOfDay()
        val planned = s.flexToday.filter { it.second.status == PlacementStatus.PLANNED }
        val current = planned.firstOrNull { it.second.isNow }
        val upcoming = planned.filter { it.second.startMin > nowMin }.sortedBy { it.second.startMin }
        return when {
            current != null -> {
                val (id, item) = current
                val next = upcoming.firstOrNull()?.second
                Focus(
                    Focus.Kind.NOW,
                    kicker = "지금 · ${formatRange(item.startMin, item.endMin)}",
                    title = item.title,
                    sub = next?.let { "다음 ${formatTime(it.startMin)} ${it.title}" }.orEmpty(),
                    color = item.color,
                    placementId = id,
                )
            }
            upcoming.isNotEmpty() -> {
                val (id, item) = upcoming.first()
                Focus(
                    Focus.Kind.NEXT,
                    kicker = "다음 · ${formatDuration(item.startMin - nowMin)} 뒤",
                    title = item.title,
                    sub = "${formatRange(item.startMin, item.endMin)} · ${formatDuration(item.endMin - item.startMin)}",
                    color = item.color,
                    placementId = id,
                )
            }
            s.flexToday.isNotEmpty() -> {
                val done = s.flexToday.count { it.second.status == PlacementStatus.DONE }
                Focus(Focus.Kind.ALL_DONE, kicker = "오늘 ${done}개 했어요", title = "끝!", sub = s.tomorrowFirst)
            }
            else -> Focus(Focus.Kind.EMPTY, kicker = "오늘", title = "비어 있어요", sub = s.tomorrowFirst)
        }
    }
}
