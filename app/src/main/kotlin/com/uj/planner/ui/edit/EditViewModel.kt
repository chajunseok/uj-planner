package com.uj.planner.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.planner.data.PlannerRepository
import com.uj.planner.data.entity.FixedEventEntity
import com.uj.planner.data.entity.FlexTaskEntity
import com.uj.planner.domain.model.PlannedSlot
import com.uj.planner.domain.model.Window
import com.uj.planner.ui.theme.TaskColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EditKind { FIXED, FLEX }

data class FixedForm(
    val title: String = "",
    val days: Set<Int> = emptySet(),
    val startMin: Int = 9 * 60,
    val durationMin: Int = 60,
) {
    val canSave get() = title.isNotBlank() && days.isNotEmpty()
}

data class FlexForm(
    val title: String = "",
    val colorIndex: Int = 0,
    val durationMin: Int = 60,
    val timesPerWeek: Int = 3,
    val priority: Int = 2,
    val window: Window = Window.ANY,
    val deadlineDay: Int? = null,
) {
    val canSave get() = title.isNotBlank()

    fun toEntity(id: Long) = FlexTaskEntity(id, title.trim(), durationMin, timesPerWeek, priority, window, deadlineDay, colorIndex)
}

/** @param id 편집할 일정. null 이면 새 일정이고, 그때만 고정/가변을 바꿀 수 있다. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EditViewModel(
    private val repository: PlannerRepository,
    initialKind: EditKind,
    private val id: Long?,
) : ViewModel() {
    val isNew get() = id == null

    var kind by mutableStateOf(initialKind)
    var fixed by mutableStateOf(FixedForm())
    var flex by mutableStateOf(FlexForm())

    /** 저장이 거부된 이유. 폼의 컨트롤이 범위를 막고 있어 보통은 뜨지 않는다. */
    var error by mutableStateOf<String?>(null)
        private set

    private var originalFixed: FixedEventEntity? = null
    private var originalFlex: FlexTaskEntity? = null

    /** 지금 저장하면 이번 주 어디에 놓일지. 계산 전이거나 조건이 말이 안 되면 null. */
    val preview: StateFlow<List<PlannedSlot>?> = snapshotFlow { flex }
        .debounce(150)
        .mapLatest { form -> runCatching { repository.previewFlexTask(form.toEntity(id ?: 0)) }.getOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            when {
                id == null -> flex = flex.copy(colorIndex = repository.observeFlexTasks().first().size % TaskColor.entries.size)
                initialKind == EditKind.FIXED -> repository.observeFixedEvents().first().find { it.id == id }?.let {
                    originalFixed = it
                    fixed = FixedForm(it.title, setOf(it.dayOfWeek), it.startMin, it.durationMin)
                }
                else -> repository.observeFlexTasks().first().find { it.id == id }?.let {
                    originalFlex = it
                    flex = FlexForm(it.title, it.colorIndex, it.durationMin, it.timesPerWeek, it.priority, it.window, it.deadlineDay)
                }
            }
        }
    }

    val canSave get() = if (kind == EditKind.FIXED) fixed.canSave else flex.canSave

    fun save(onSaved: () -> Unit) = run(onSaved) {
        when (kind) {
            EditKind.FIXED -> repository.saveFixedEvents(
                events = fixed.days.sorted().map { FixedEventEntity(0, fixed.title.trim(), it, fixed.startMin, fixed.durationMin) },
                replacing = originalFixed,
            )
            EditKind.FLEX -> repository.saveFlexTask(flex.toEntity(id ?: 0))
        }
    }

    fun delete(onDeleted: () -> Unit) = run(onDeleted) {
        originalFixed?.let { repository.deleteFixedEvent(it) }
        originalFlex?.let { repository.deleteFlexTask(it) }
    }

    private fun run(onSuccess: () -> Unit, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                onSuccess()
            } catch (e: IllegalArgumentException) {
                error = e.message
            }
        }
    }
}
