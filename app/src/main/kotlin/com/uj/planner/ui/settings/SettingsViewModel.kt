package com.uj.planner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.planner.data.PlannerRepository
import com.uj.planner.data.entity.DayAvailabilityEntity
import com.uj.planner.domain.GRID_MIN
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 종료는 다음 날 06:00 까지 허용한다. 그보다 늦으면 다음 날 아침 일정과 구분이 안 된다. */
const val LATEST_END_MIN = 30 * 60

class SettingsViewModel(private val repository: PlannerRepository) : ViewModel() {
    /** 월요일부터 7줄. */
    val days: StateFlow<List<DayAvailabilityEntity>> =
        repository.observeAvailability().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)

    /** 방금 한 일의 결과나 실패 이유. */
    val message: StateFlow<String?> = _message.asStateFlow()

    /** 30분 단위로 시작·종료를 옮긴다. 범위를 벗어나거나 시작이 종료를 넘는 변경은 무시한다. */
    fun shift(day: DayAvailabilityEntity, startSteps: Int = 0, endSteps: Int = 0) {
        val changed = day.copy(startMin = day.startMin + startSteps * GRID_MIN, endMin = day.endMin + endSteps * GRID_MIN)
        if (changed.startMin < 0 || changed.endMin > LATEST_END_MIN || changed.startMin >= changed.endMin) return
        save(listOf(changed))
    }

    /** [from] 요일의 시간대를 [to] 요일들에 그대로 적용한다. */
    fun copy(from: Int, to: IntRange) {
        val source = days.value.find { it.dayOfWeek == from } ?: return
        save(to.map { source.copy(dayOfWeek = it) })
    }

    /** 완료한 일정은 두고, 손으로 옮긴 자리까지 풀어 이번 주를 처음부터 다시 짠다. */
    fun replan() = run {
        val missing = repository.recomputeWeek(resetPins = true).sumOf { it.missing }
        _message.value = if (missing == 0) "이번 주를 다시 짰어요" else "다시 짰어요. ${missing}회는 빈 시간이 모자라 못 넣었어요"
    }

    private fun save(days: List<DayAvailabilityEntity>) = run {
        repository.saveAvailability(days)
        _message.value = null
    }

    private fun run(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = e.message ?: "저장하지 못했어요"
            }
        }
    }
}
