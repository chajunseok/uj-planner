package com.uj.planner.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.planner.data.Backup
import com.uj.planner.data.BackupException
import com.uj.planner.data.PlannerRepository
import com.uj.planner.data.entity.DayAvailabilityEntity
import com.uj.planner.domain.GRID_MIN
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 종료는 다음 날 06:00 까지 허용한다. 그보다 늦으면 다음 날 아침 일정과 구분이 안 된다. */
const val LATEST_END_MIN = 30 * 60

class SettingsViewModel(private val repository: PlannerRepository, private val backup: Backup) : ViewModel() {
    // 아직 저장되지 않은 변경(요일 → 값). 화면은 저장된 값 위에 이것을 덮어 보여 준다.
    // 버튼을 빠르게 연달아 눌러도 매번 최신 값에서 계산되므로 누른 횟수만큼 정확히 움직인다.
    private val edits = MutableStateFlow<Map<Int, DayAvailabilityEntity>>(emptyMap())

    /** 월요일부터 7줄. */
    val days: StateFlow<List<DayAvailabilityEntity>> =
        combine(repository.observeAvailability(), edits) { saved, local -> saved.map { local[it.dayOfWeek] ?: it } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)

    /** 방금 한 일의 결과나 실패 이유. */
    val message: StateFlow<String?> = _message.asStateFlow()

    // 메인 스레드에서만 읽고 쓴다.
    private var saving = false

    /** 30분 단위로 시작·종료를 옮긴다. 범위를 벗어나거나 시작이 종료를 넘는 변경은 무시한다. */
    fun shift(dayOfWeek: Int, startSteps: Int = 0, endSteps: Int = 0) {
        val day = days.value.find { it.dayOfWeek == dayOfWeek } ?: return
        val changed = day.copy(startMin = day.startMin + startSteps * GRID_MIN, endMin = day.endMin + endSteps * GRID_MIN)
        if (changed.startMin < 0 || changed.endMin > LATEST_END_MIN || changed.startMin >= changed.endMin) return
        stage(listOf(changed))
    }

    /** [from] 요일의 시간대를 [to] 요일들에 그대로 적용한다. */
    fun copy(from: Int, to: IntRange) {
        val source = days.value.find { it.dayOfWeek == from } ?: return
        stage(to.map { source.copy(dayOfWeek = it) })
    }

    /** 완료한 일정은 두고, 손으로 옮긴 자리까지 풀어 이번 주를 처음부터 다시 짠다. */
    fun replan() {
        viewModelScope.launch {
            try {
                val missing = repository.recomputeWeek(resetPins = true).sumOf { it.missing }
                _message.value = if (missing == 0) "이번 주를 다시 짰어요" else "다시 짰어요. ${missing}회는 빈 시간이 모자라 못 넣었어요"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = e.message ?: "다시 짜지 못했어요"
            }
        }
    }

    fun export(target: Uri) {
        viewModelScope.launch {
            try {
                backup.exportTo(target)
                _message.value = "파일로 내보냈어요"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "내보내지 못했어요: ${e.message.orEmpty()}"
            }
        }
    }

    /**
     * 지금 데이터를 파일의 내용으로 바꾼다. 받아들일 수 없는 파일이면 이유만 알리고 아무것도 바꾸지 않는다.
     * 그 밖에는 성공이든 실패든 DB 가 닫혔을 수 있어서 [restart] 로 앱을 다시 시작한다.
     */
    fun import(source: Uri, restart: () -> Unit) {
        // 가져오기는 DB 를 닫는다. 가용 시간 저장이 도는 중이면 그 저장이 깨진다.
        if (saving) {
            _message.value = "시간대를 저장하는 중이에요. 잠시 뒤에 다시 눌러 주세요"
            return
        }
        viewModelScope.launch {
            try {
                backup.importFrom(source)
                restart()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupException) {
                _message.value = e.message
            } catch (e: Exception) {
                restart()
            }
        }
    }

    private fun stage(changed: List<DayAvailabilityEntity>) {
        edits.update { it + changed.associateBy { day -> day.dayOfWeek } }
        _message.value = null
        drain()
    }

    /**
     * 쌓인 변경을 저장한다. 저장은 주 전체를 다시 짜는 트랜잭션이라 한 번에 하나만 돌리고,
     * 그 사이에 들어온 변경은 다음 바퀴에 한꺼번에 저장한다 — 네 번 눌러도 트랜잭션은 한두 번이다.
     * 화면을 떠나도 쌓인 변경은 끝까지 저장한다(NonCancellable).
     */
    private fun drain() {
        if (saving) return
        saving = true
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    while (true) {
                        val batch = edits.value
                        if (batch.isEmpty()) break
                        repository.saveAvailability(batch.values.toList())
                        // 저장된 값이 관찰 쪽에 도착한 뒤에 덮어쓰기를 걷는다. 먼저 걷으면 잠깐 옛 값이 비친다.
                        repository.observeAvailability().first { saved -> saved.containsAll(batch.values) }
                        edits.update { current -> current.filterNot { (day, value) -> batch[day] == value } }
                    }
                } catch (e: Exception) {
                    edits.value = emptyMap()
                    _message.value = e.message ?: "저장하지 못했어요"
                } finally {
                    saving = false
                }
            }
        }
    }
}
