package com.uj.planner.ui.missed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.planner.data.PlannerRepository
import com.uj.planner.data.entity.PlacementEntity
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.formatDuration
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.theme.TaskColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.isoDayNumber

/** 끝났는데 아직 답을 받지 못한 배치 하나. */
data class MissedItem(val placement: PlacementEntity, val title: String, val color: TaskColor) {
    /** "화 20:00 – 21:00 · 1시간" */
    fun whenText(): String = with(placement) {
        "${DAY_NAMES[date.dayOfWeek.isoDayNumber - 1]} ${formatRange(startMin, endMin)} · ${formatDuration(endMin - startMin)}"
    }
}

/**
 * 답한 결과 한 줄. [movedIds] 는 되돌릴 때 지울, 새로 잡힌 자리다.
 * 주 전체를 다시 짠 경우는 다른 일정의 자리까지 바뀌어서 되돌릴 수 없다([undoable] = false).
 */
data class ResolvedLine(
    val placementId: Long,
    val text: String,
    val detail: String,
    val movedIds: List<Long> = emptyList(),
    val undoable: Boolean = true,
)

data class MissedUiState(
    val pending: List<MissedItem> = emptyList(),
    val resolved: List<ResolvedLine> = emptyList(),
    /** `못함` 으로 답했는데 이번 주에 다시 넣을 빈칸이 없는 일정. 어떻게 할지 물어야 한다. */
    val noRoom: MissedItem? = null,
    /** "나중에" 로 닫았다. 앱에 다시 들어오면 풀린다. */
    val dismissed: Boolean = false,
    /** 답을 반영하지 못했다. 시트에 그대로 보여 준다. */
    val error: String? = null,
) {
    val sheetVisible get() = !dismissed && (pending.isNotEmpty() || resolved.isNotEmpty() || error != null)
}

class MissedViewModel(private val repository: PlannerRepository) : ViewModel() {
    private val _state = MutableStateFlow(MissedUiState())
    val state: StateFlow<MissedUiState> = _state.asStateFlow()

    // 답은 하나씩 차례로 반영한다. 버튼을 연달아 눌러도 같은 배치에 두 번 답하지 않는다.
    private val mutex = Mutex()

    /**
     * 주간 화면이 보일 때마다 부른다. 밀린 목록만 새로 읽는다.
     * 결과 줄과 빈칸 없음 다이얼로그는 남긴다 — 화면이 잠깐 꺼졌다 켜져도 이 함수가 불리는데, 그때 다이얼로그를 지우면
     * 이미 `못함` 으로 저장된 그 배치를 어떻게 할지 다시 물을 길이 없어진다.
     */
    fun refresh() = launchLocked {
        val pending = loadPending()
        _state.update { it.copy(pending = pending, error = null) }
    }

    /** 앱(또는 주간 화면)을 실제로 떠났다. "나중에" 를 풀어 다음에 들어올 때 다시 묻는다. */
    fun undismiss() = _state.update { it.copy(dismissed = false) }

    fun answer(item: MissedItem, status: PlacementStatus) = launchLocked {
        if (_state.value.pending.none { it.placement.id == item.placement.id }) return@launchLocked
        val result = repository.resolveMissed(mapOf(item.placement.id to status))
        if (status == PlacementStatus.MISSED && result.unplaced.isNotEmpty()) {
            _state.update { it.copy(pending = it.pending - item, noRoom = item) }
            return@launchLocked
        }
        val moved = result.moved.firstOrNull()
        val text = when {
            moved != null -> "${item.title} → ${DAY_NAMES[moved.dayOfWeek - 1]} ${formatTime(moved.startMin)}으로 옮겼어요"
            // 지난 주의 못함은 이월하지 않는다. 새 주가 주당 횟수를 새로 받았기 때문이다.
            status == PlacementStatus.MISSED -> "${item.title} · 못함 (지난 주라 다시 넣지 않아요)"
            status == PlacementStatus.DONE -> "${item.title} · 했음"
            else -> "${item.title} · 버림"
        }
        addLine(item, ResolvedLine(item.placement.id, text, item.whenText(), result.movedIds))
    }

    fun answerAllDone() = launchLocked {
        val items = _state.value.pending
        repository.resolveMissed(items.associate { it.placement.id to PlacementStatus.DONE })
        _state.update { s ->
            s.copy(pending = emptyList(), resolved = items.map { ResolvedLine(it.placement.id, "${it.title} · 했음", it.whenText()) } + s.resolved)
        }
    }

    fun undo(line: ResolvedLine) = launchLocked {
        if (!line.undoable) return@launchLocked
        val undone = repository.undoResolve(line.placementId, line.movedIds)
        val pending = loadPending()
        _state.update { s ->
            // 무를 수 없는 상태였다면 줄은 남기고 되돌리기만 닫는다.
            val resolved = if (undone) s.resolved - line else s.resolved.map { if (it == line) it.copy(undoable = false) else it }
            s.copy(resolved = resolved, pending = pending)
        }
    }

    /** 빈칸 없음 다이얼로그를 답 없이 닫았다. `못함` 인 채로 둔다. */
    fun keepNoRoom() = launchLocked {
        val item = _state.value.noRoom ?: return@launchLocked
        addLine(item, ResolvedLine(item.placement.id, "${item.title} · 못함 (빈칸이 없어 못 옮겼어요)", item.whenText()))
    }

    /** 빈칸 없음 다이얼로그: 이번 주에서 포기한다. */
    fun dropNoRoom() = launchLocked {
        val item = _state.value.noRoom ?: return@launchLocked
        repository.dropMissed(item.placement.id)
        addLine(item, ResolvedLine(item.placement.id, "${item.title} · 이번 주는 버림", item.whenText()))
    }

    /** 빈칸 없음 다이얼로그: 한 일정은 두고 남은 가변 일정을 처음부터 다시 짠다. */
    fun replanNoRoom() = launchLocked {
        val item = _state.value.noRoom ?: return@launchLocked
        val stillUnplaced = repository.recomputeWeek(resetPins = true).any { it.taskId == item.placement.flexTaskId }
        val text = if (stillUnplaced) "${item.title} · 다시 짰지만 자리가 없었어요" else "${item.title} · 다시 짜서 넣었어요"
        // 다시 짜면서 앞서 옮겨 둔 자리들도 새로 놓였다. 이제 앞의 답을 되돌리면 그 자리가 남아 횟수를 넘기므로 되돌리기를 닫는다.
        _state.update { s -> s.copy(resolved = s.resolved.map { it.copy(undoable = false) }) }
        addLine(item, ResolvedLine(item.placement.id, text, item.whenText(), undoable = false))
    }

    /** 시트를 닫는다. 결과 줄은 여기서 비운다 — 되돌릴 기회는 시트가 열려 있는 동안이다. */
    fun dismiss() = _state.update { it.copy(dismissed = true, resolved = emptyList(), error = null) }

    private fun addLine(item: MissedItem, line: ResolvedLine) = _state.update {
        it.copy(pending = it.pending - item, noRoom = null, resolved = listOf(line) + it.resolved)
    }

    private suspend fun loadPending(): List<MissedItem> {
        val tasks = repository.observeFlexTasks().first().associateBy { it.id }
        return repository.overdue().mapNotNull { p ->
            tasks[p.flexTaskId]?.let { MissedItem(p, it.title, TaskColor.of(it.colorIndex)) }
        }
    }

    private fun launchLocked(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // 이 시트는 앱에 들어올 때마다 저절로 뜬다. 여기서 죽으면 앱을 열 때마다 죽는다.
                    // 답은 트랜잭션이라 반쯤 저장되지 않는다 — 알리고 목록은 그대로 둔다.
                    _state.update { it.copy(error = e.message ?: "반영하지 못했어요") }
                }
            }
        }
    }
}
