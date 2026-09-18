package com.uj.planner.ui.week

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uj.planner.domain.model.Window
import com.uj.planner.domain.weekEndOf
import com.uj.planner.domain.weekOfMonth
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.edit.EditKind
import com.uj.planner.ui.formatDuration
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.label
import com.uj.planner.ui.missed.MissedSheet
import com.uj.planner.ui.missed.MissedViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

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
                UnplacedBanner(
                    items = state.unplaced,
                    expanded = unplacedExpanded,
                    onToggle = { unplacedExpanded = !unplacedExpanded },
                    onChangeConditions = { onEdit(EditKind.FLEX, it) },
                    onReplan = viewModel::replan,
                )
            }
            DayHeader(state.weekStart, now.toLocalDate())
            if (state.isEmpty) {
                EmptyState(onAddFixed = { onEdit(EditKind.FIXED, null) }, onAddFlex = { onEdit(EditKind.FLEX, null) })
            } else {
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
            Text("${month}월 ${ORDINALS[ordinal - 1]} 주", style = MaterialTheme.typography.titleMedium)
            Text(
                if (isThisWeek) "$range · 이번 주" else range,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "설정") }
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
        Row(
            Modifier.navigationBarsPadding().fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "이전 주") }
            TextButton(onClick = onThisWeek, enabled = !isThisWeek) { Text("이번 주") }
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "다음 주") }
            Spacer(Modifier.weight(1f))
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("일정") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.height(48.dp),
            )
        }
    }
}

/** 블록을 끄는 동안 배너 자리에 뜨는 안내. 지금 가리키는 자리를 글로 알려 준다. */
@Composable
private fun MoveBanner(block: WeekBlock, target: MoveTarget?) {
    BannerSurface(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) {
        Icon(Icons.Rounded.OpenWith, contentDescription = null, modifier = Modifier.size(20.dp))
        Text("${block.title} · 놓을 자리로 끌어 주세요", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        if (target != null) {
            Text(
                if (target.valid) "${DAY_NAMES[target.dayOfWeek - 1]} ${formatRange(target.startMin, target.endMin)}" else "놓을 수 없어요",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** 자리를 못 받은 일정이 있는 동안에는 닫을 수 없다. 접을 수만 있다. */
@Composable
private fun UnplacedBanner(
    items: List<UnplacedItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChangeConditions: (Long) -> Unit,
    onReplan: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        BannerSurface(scheme.tertiaryContainer, scheme.onTertiaryContainer, Modifier.clickable(onClick = onToggle)) {
            Icon(Icons.Rounded.EventBusy, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                "빈 시간이 모자라 ${items.sumOf { it.missing }}회 못 넣었어요",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(if (expanded) "닫기" else "보기", style = MaterialTheme.typography.labelMedium)
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(horizontal = 12.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { (task, missing) ->
                    Surface(shape = RoundedCornerShape(16.dp), color = scheme.surfaceContainerLowest, shadowElevation = 1.dp) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "주 ${task.timesPerWeek}회 중 ${missing}회 못 넣음",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            val window = if (task.window == Window.ANY) "시간대 무관" else "${task.window.label} 선호"
                            val deadline = task.deadlineDay?.let { "${DAY_NAMES[it - 1]}요일까지" } ?: "마감 없음"
                            Text(
                                "${formatDuration(task.durationMin)} · $window · $deadline. " +
                                    "하루에 한 번씩만 넣어서, 남은 요일이나 빈 시간이 모자랐어요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onChangeConditions(task.id) }) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("조건 바꾸기", Modifier.padding(start = 6.dp))
                                }
                                FilledTonalButton(onClick = onReplan) {
                                    Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("다시 짜기", Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerSurface(
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    body: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().heightIn(min = 44.dp).then(modifier),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = body,
        )
    }
}

@Composable
private fun EmptyState(onAddFixed: () -> Unit, onAddFlex: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp).background(scheme.surfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ViewWeek, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        }
        Text(
            "먼저 움직이지 않는 일정을 넣어 주세요",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            "출근, 수업, 식사처럼 매주 같은 시간에 있는 고정 일정이 회색 배경이 돼요. 그 사이 빈 시간에 가변 일정을 넣어 드릴게요.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onAddFixed, modifier = Modifier.padding(top = 24.dp).height(48.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("고정 일정 추가", Modifier.padding(start = 6.dp))
        }
        TextButton(onClick = onAddFlex, modifier = Modifier.padding(top = 4.dp)) { Text("가변 일정부터 넣기") }
    }
}
