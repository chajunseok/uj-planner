package com.uj.planner.ui.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uj.planner.domain.weekEndOf
import com.uj.planner.domain.weekOfMonth
import com.uj.planner.ui.edit.EditKind
import com.uj.planner.ui.missed.MissedSheet
import com.uj.planner.ui.missed.MissedViewModel
import com.uj.planner.ui.theme.PlannerColors
import com.uj.planner.ui.theme.serif
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay

private val ORDINALS = listOf("첫째", "둘째", "셋째", "넷째", "다섯째")

/** 현재 시각. 현재 시각선과 "진행 중" 표시가 따라 움직이도록 30초마다 갱신한다. */
@Composable
fun rememberNow(): LocalDateTime {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalDateTime.now()
        }
    }
    return now
}

/**
 * @param onEdit 편집 화면으로. id 가 null 이면 새 일정.
 */
@Composable
fun WeekScreen(
    viewModel: WeekViewModel,
    missedViewModel: MissedViewModel,
    onEdit: (EditKind, Long?) -> Unit,
    onSettings: () -> Unit,
) {
    MissedSheet(missedViewModel)
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    val freeSlots by viewModel.freeSlots.collectAsStateWithLifecycle()
    val now = rememberNow()
    var selected by remember { mutableStateOf<WeekBlock?>(null) }
    var moveTarget by remember { mutableStateOf<MoveTarget?>(null) }
    var moving by remember { mutableStateOf<WeekBlock?>(null) }
    var unplacedExpanded by rememberSaveable { mutableStateOf(false) }
    // 블록이 다시 짜이면 띄워 둔 카드가 가리키던 자리가 사라진다.
    LaunchedEffect(state.blocks) { selected = selected?.takeIf { it in state.blocks } }

    Scaffold(
        topBar = {
            // 끄는 동안의 안내는 제목 자리에 그린다. 그리드 위에 끼워 넣으면 그리드가 밀려 손가락과 블록이 어긋난다.
            Box(Modifier.statusBarsPadding().fillMaxWidth().height(64.dp), contentAlignment = Alignment.CenterStart) {
                val movingBlock = moving
                if (movingBlock != null) MoveBanner(movingBlock, moveTarget) else WeekHeader(state.weekStart, state.isThisWeek, onSettings)
            }
        },
        bottomBar = {
            WeekBottomBar(
                isThisWeek = state.isThisWeek,
                onPrevious = { viewModel.shiftWeek(-1) },
                onThisWeek = viewModel::goToThisWeek,
                onNext = { viewModel.shiftWeek(1) },
                onAdd = { onEdit(EditKind.FLEX, null) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.unplaced.isNotEmpty()) {
                UnplacedBanner(state.unplaced, unplacedExpanded, onToggle = { unplacedExpanded = !unplacedExpanded })
            }
            Box(Modifier.weight(1f)) {
                Column {
                    DayHeader(state.weekStart, now.toLocalDate(), Modifier.padding(top = 8.dp))
                    WeekGrid(
                        state = state,
                        now = now,
                        freeSlots = freeSlots,
                        selected = selected,
                        onSelect = { selected = it },
                        onMoveStart = {
                            selected = null
                            moving = it
                            viewModel.beginMove(checkNotNull(it.placementId))
                        },
                        onMoveTarget = { moveTarget = it },
                        onMoveEnd = { block, target ->
                            moving = null
                            viewModel.endMove(checkNotNull(block.placementId), target?.let { it.dayOfWeek to it.startMin })
                        },
                    ) { block ->
                        BlockDetailCard(
                            block = block,
                            canAct = !state.isPast,
                            onDone = { selected = null; viewModel.markDone(checkNotNull(block.placementId)) },
                            onDrop = { selected = null; viewModel.drop(checkNotNull(block.placementId)) },
                            onEdit = {
                                selected = null
                                if (block.isFixed) onEdit(EditKind.FIXED, block.fixedId) else onEdit(EditKind.FLEX, block.flexTaskId)
                            },
                        )
                    }
                }
                // 상세와 빈 상태 카드는 그리드를 밀지 않고 위에 덮는다.
                if (state.unplaced.isNotEmpty() && unplacedExpanded) {
                    // 패널 바깥을 누르면 닫힌다. 이 막이 없으면 패널 옆으로 아래 그리드의 블록이 눌린다.
                    Box(
                        Modifier.matchParentSize().clickable(interactionSource = null, indication = null, onClickLabel = "상세 닫기") { unplacedExpanded = false },
                    )
                    UnplacedDetails(state.unplaced, onChangeConditions = { onEdit(EditKind.FLEX, it) }, onReplan = viewModel::replan, modifier = Modifier.padding(bottom = 12.dp))
                }
                if (state.isEmpty) {
                    EmptyCard(
                        onAddFixed = { onEdit(EditKind.FIXED, null) },
                        onAddFlex = { onEdit(EditKind.FLEX, null) },
                        modifier = Modifier.padding(top = 120.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(weekStart: LocalDate, isThisWeek: Boolean, onSettings: () -> Unit) {
    val (month, ordinal) = weekOfMonth(weekStart)
    val end = weekEndOf(weekStart)
    val range = "${weekStart.monthValue}.${weekStart.dayOfMonth} – ${end.monthValue}.${end.dayOfMonth}"
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${month}월 ${ORDINALS[ordinal - 1]} 주", style = MaterialTheme.typography.titleMedium.serif())
            Text(
                if (isThisWeek) "$range · 이번 주" else range,
                style = MaterialTheme.typography.bodySmall,
                color = PlannerColors.Muted,
            )
        }
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "설정", tint = PlannerColors.Body) }
    }
}

@Composable
private fun WeekBottomBar(
    isThisWeek: Boolean,
    onPrevious: () -> Unit,
    onThisWeek: () -> Unit,
    onNext: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Row(
                Modifier.navigationBarsPadding().fillMaxWidth().height(72.dp).padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "이전 주", tint = PlannerColors.Body) }
                ThisWeekPill(enabled = !isThisWeek, onClick = onThisWeek)
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "다음 주", tint = PlannerColors.Body) }
                Spacer(Modifier.weight(1f))
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(22.dp)) },
                    text = { Text("일정", style = MaterialTheme.typography.labelLarge) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    // 확장 FAB 의 Text 는 시맨틱스 트리에 오르지 않아 라벨 없는 버튼으로 읽힌다. 직접 붙인다.
                    modifier = Modifier.height(48.dp).semantics { contentDescription = "일정 추가" },
                )
            }
        }
    }
}
