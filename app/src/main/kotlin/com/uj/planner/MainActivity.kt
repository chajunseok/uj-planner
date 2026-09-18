package com.uj.planner

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uj.planner.data.PlannerRepository
import com.uj.planner.data.entity.FixedEventEntity
import com.uj.planner.data.entity.FlexTaskEntity
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.domain.model.Window
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as PlannerApp).repository
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        setContent {
            // ponytail: 기본 색 구성표. 디자인 토큰이 오면 Phase 3 의 ui/theme/ 로 교체한다.
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { PersistenceProbe(repository, debuggable) }
            }
        }
    }
}

/**
 * ponytail: Room → Repository → Scheduler 경로를 기기에서 확인하기 위한 임시 화면.
 * Phase 3 에서 AdaptiveHost 로 통째로 교체한다. DB 에 쓰는 버튼은 디버그 빌드에서만 보인다.
 */
@Composable
private fun PersistenceProbe(repository: PlannerRepository, debuggable: Boolean) {
    val scope = rememberCoroutineScope()
    val weekStart = remember { repository.currentWeekStart() }
    val fixed by remember { repository.observeFixedEvents() }.collectAsState(emptyList())
    val tasks by remember { repository.observeFlexTasks() }.collectAsState(emptyList())
    val placements by remember { repository.observeWeek(weekStart) }.collectAsState(emptyList())
    val unplaced by remember { repository.observeUnplaced(weekStart) }.collectAsState(emptyList())
    val titles = tasks.associate { it.id to it.title }
    var log by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("U.J planner — 저장 확인용 임시 화면", style = MaterialTheme.typography.titleMedium)
        Text("주 시작 $weekStart · 고정 ${fixed.size} · 가변 ${tasks.size} · 배치 ${placements.size} · 실패 ${unplaced.size}")
        if (debuggable) {
            Button(onClick = { scope.launch { seedSample(repository) } }, enabled = tasks.isEmpty()) {
                Text(if (tasks.isEmpty()) "샘플 넣고 배치" else "샘플 들어 있음")
            }
            Button(onClick = { scope.launch { log = resolveOverdueTwice(repository) } }) { Text("밀린 일정 못함 처리") }
            if (log.isNotEmpty()) Text(log)
        }
        placements.forEach {
            Text("${"월화수목금토일"[it.date.dayOfWeek.value - 1]} ${hhmm(it.startMin)}–${hhmm(it.endMin)}  ${titles[it.flexTaskId]}  ${it.status}")
        }
        unplaced.forEach { Text("배치 실패: ${titles[it.taskId]} ${it.missing}회", color = MaterialTheme.colorScheme.error) }
    }
}

private fun hhmm(min: Int) = "%02d:%02d".format(min / 60, min % 60)

private suspend fun seedSample(repository: PlannerRepository) {
    (1..5).forEach { repository.saveFixedEvent(FixedEventEntity(title = "회사", dayOfWeek = it, startMin = 9 * 60, durationMin = 9 * 60)) }
    repository.saveFlexTask(FlexTaskEntity(title = "운동", durationMin = 60, timesPerWeek = 3, priority = 3, window = Window.EVENING))
    repository.saveFlexTask(FlexTaskEntity(title = "영어", durationMin = 60, timesPerWeek = 2, priority = 2, window = Window.MORNING, deadlineDay = 3))
    repository.saveFlexTask(FlexTaskEntity(title = "독서", durationMin = 30, timesPerWeek = 5, priority = 1, window = Window.ANY))
}

/** 같은 답을 두 번 넣어 두 번째 호출이 아무것도 하지 않는지 본다. */
private suspend fun resolveOverdueTwice(repository: PlannerRepository): String {
    val decisions = repository.overdue().associate { it.id to PlacementStatus.MISSED }
    val first = repository.resolveMissed(decisions)
    val second = repository.resolveMissed(decisions)
    return "밀린 ${decisions.size}건 · 1차 옮김 ${first.moved.size} 실패 ${first.unplaced.size} · 2차 옮김 ${second.moved.size} 실패 ${second.unplaced.size}"
}
