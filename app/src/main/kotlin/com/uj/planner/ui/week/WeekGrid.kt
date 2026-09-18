package com.uj.planner.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.domain.GRID_MIN
import com.uj.planner.domain.fits
import com.uj.planner.domain.minuteOfDay
import com.uj.planner.domain.model.Slot
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.theme.AxisStyle
import com.uj.planner.ui.theme.PlannerColors
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.floor
import kotlin.math.roundToInt

/** 30분 한 행의 높이. 08–24시가 704dp 로 한 화면에 들어오게 잡은 값이다. */
private val ROW_HEIGHT = 22.dp
private val AXIS_WIDTH = 32.dp
private val GRID_END_PADDING = 8.dp
private val GRID_TOP_PADDING = 8.dp
private val DETAIL_CARD_WIDTH = 196.dp

/** 옮기는 중인 블록이 지금 가리키는 자리. [valid] 가 false 면 거기엔 놓을 수 없다. */
data class MoveTarget(val block: WeekBlock, val dayOfWeek: Int, val startMin: Int, val valid: Boolean) {
    val endMin get() = startMin + block.endMin - block.startMin
}

private class GridGeometry(val columnWidth: Dp, private val gridStartMin: Int) {
    fun x(dayOfWeek: Int): Dp = AXIS_WIDTH + columnWidth * (dayOfWeek - 1)
    fun y(min: Int): Dp = ROW_HEIGHT * ((min - gridStartMin) / GRID_MIN.toFloat())
    fun height(startMin: Int, endMin: Int): Dp = y(endMin) - y(startMin)
}

/** 요일·날짜 머리줄. 아래 [WeekGrid] 와 열이 맞도록 같은 축 폭과 여백을 쓴다. */
@Composable
fun DayHeader(weekStart: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Spacer(Modifier.width(AXIS_WIDTH))
        DAY_NAMES.forEachIndexed { i, name ->
            val date = weekStart.plusDays(i.toLong())
            val isToday = date == today
            val labelColor = when {
                isToday -> MaterialTheme.colorScheme.primary
                i >= 5 -> PlannerColors.Faint
                else -> PlannerColors.Muted
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = labelColor,
                )
                Box(
                    Modifier.padding(top = 2.dp).size(24.dp)
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(Modifier.width(GRID_END_PADDING))
    }
}

/**
 * 7열 시간 그리드.
 *
 * 예정 블록은 길게 눌러 끌 수 있다. 놓을 수 있는지는 [freeSlots] 로 판정하는데, 이는 스케줄러가 쓰는 것과 같은 계산이다.
 *
 * @param selected 탭해서 상세 카드를 띄운 블록.
 * @param detail 상세 카드의 내용.
 */
@Composable
fun WeekGrid(
    state: WeekUiState,
    now: LocalDateTime,
    freeSlots: Map<Int, List<Slot>>,
    selected: WeekBlock?,
    onSelect: (WeekBlock?) -> Unit,
    onMoveStart: (WeekBlock) -> Unit,
    onMoveTarget: (MoveTarget?) -> Unit,
    onMoveEnd: (WeekBlock, MoveTarget?) -> Unit,
    modifier: Modifier = Modifier,
    detail: @Composable (WeekBlock) -> Unit,
) {
    val density = LocalDensity.current
    val gridHeight = ROW_HEIGHT * ((state.gridEndMin - state.gridStartMin) / GRID_MIN)

    BoxWithConstraints(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        val gridWidth = maxWidth
        val geometry = GridGeometry((gridWidth - AXIS_WIDTH - GRID_END_PADDING) / 7, state.gridStartMin)
        // 끄는 블록과 끌린 거리를 따로 둔다. 거리는 픽셀마다 바뀌므로 그리기 단계(graphicsLayer)와 아래 derivedStateOf
        // 안에서만 읽는다 — 그래야 손가락이 움직일 때마다 모든 블록이 다시 구성되지 않는다.
        var dragging by remember { mutableStateOf<WeekBlock?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }

        // 가리키는 자리는 30분·한 열 단위로만 바뀐다. derivedStateOf 라 값이 바뀔 때만 읽는 쪽이 다시 구성된다.
        val target by remember(freeSlots, state.gridStartMin, gridWidth, density) {
            derivedStateOf {
                dragging?.let { block ->
                    with(density) {
                        val column = (geometry.x(block.dayOfWeek).toPx() + dragOffset.x - AXIS_WIDTH.toPx()) / geometry.columnWidth.toPx()
                        val day = (floor(column + 0.5f).toInt() + 1).coerceIn(1, 7)
                        val row = ((geometry.y(block.startMin).toPx() + dragOffset.y) / ROW_HEIGHT.toPx()).roundToInt()
                        val start = state.gridStartMin + row * GRID_MIN
                        val valid = freeSlots[day]?.fits(start, start + block.endMin - block.startMin) == true
                        MoveTarget(block, day, start, valid)
                    }
                }
            }
        }
        LaunchedEffect(target) { onMoveTarget(target) }
        // 끌기 콜백은 블록이 바뀔 때만 다시 만들어지므로, 그 안에서 보는 값은 최신으로 따로 잡아 둔다.
        val latestTarget by rememberUpdatedState(target)
        val latestOnMoveStart by rememberUpdatedState(onMoveStart)
        val latestOnMoveEnd by rememberUpdatedState(onMoveEnd)

        Box(Modifier.padding(top = GRID_TOP_PADDING, bottom = 16.dp).fillMaxWidth().height(gridHeight)) {
            GridBackground(state, now, geometry)

            if (dragging != null) {
                freeSlots.values.flatten().forEach { slot ->
                    val start = slot.startMin.coerceAtLeast(state.gridStartMin)
                    val end = slot.endMin.coerceAtMost(state.gridEndMin)
                    if (end > start) {
                        Box(
                            Modifier.offset(geometry.x(slot.dayOfWeek), geometry.y(start))
                                .size(geometry.columnWidth, geometry.height(start, end))
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), RoundedCornerShape(6.dp)),
                        )
                    }
                }
                target?.takeIf { it.valid }?.let { t ->
                    Box(
                        Modifier.offset(geometry.x(t.dayOfWeek), geometry.y(t.startMin))
                            .size(geometry.columnWidth, geometry.height(t.startMin, t.endMin))
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
                    )
                }
            }

            val today = now.toLocalDate()
            val nowMin = now.minuteOfDay()
            state.blocks.forEach { block ->
                val isDragged = dragging?.key == block.key
                val date = state.weekStart.plusDays(block.dayOfWeek - 1L)
                val inProgress = block.status == PlacementStatus.PLANNED && date == today && nowMin in block.startMin until block.endMin
                val movable = block.status == PlacementStatus.PLANNED && !state.isPast
                val cell = Modifier.offset(geometry.x(block.dayOfWeek), geometry.y(block.startMin))
                    .size(geometry.columnWidth, geometry.height(block.startMin, block.endMin))

                if (isDragged) BlockGhost(block, cell)
                BlockView(
                    block = block,
                    inProgress = inProgress,
                    dragging = isDragged,
                    modifier = cell
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            if (isDragged) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.08f
                                scaleY = 1.08f
                                shadowElevation = 6.dp.toPx()
                            }
                        }
                        .then(
                            if (!movable) Modifier else Modifier.pointerInput(block) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragOffset = Offset.Zero
                                        dragging = block
                                        latestOnMoveStart(block)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                    },
                                    onDragEnd = {
                                        latestOnMoveEnd(block, latestTarget?.takeIf { it.valid })
                                        dragging = null
                                    },
                                    onDragCancel = {
                                        latestOnMoveEnd(block, null)
                                        dragging = null
                                    },
                                )
                            },
                        )
                        .clickable { onSelect(block) },
                )
            }

            NowLine(state, now, geometry)

            if (selected != null) {
                // 카드 바깥을 누르면 닫힌다.
                Box(
                    Modifier.matchParentSize().zIndex(2f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(null) },
                )
                val x = (geometry.x(selected.dayOfWeek) + geometry.columnWidth / 2 - DETAIL_CARD_WIDTH / 2)
                    .coerceIn(8.dp, gridWidth - DETAIL_CARD_WIDTH - 8.dp)
                // 블록 아래에 띄우되, 그리드 아래쪽 블록이면 카드가 잘리지 않게 위로 올린다.
                val below = geometry.y(selected.endMin) + 4.dp
                val y = if (below + 140.dp > gridHeight) (geometry.y(selected.startMin) - 144.dp).coerceAtLeast(0.dp) else below
                Box(Modifier.offset(x, y).width(DETAIL_CARD_WIDTH).zIndex(3f)) { detail(selected) }
            }
        }
    }
}

/** 시간선, 시간축 숫자, 오늘 열. */
@Composable
private fun GridBackground(state: WeekUiState, now: LocalDateTime, geometry: GridGeometry) {
    val scheme = MaterialTheme.colorScheme
    val todayColumn = now.dayOfWeek.value.takeIf { state.isThisWeek }

    if (todayColumn != null) {
        Box(
            Modifier.offset(x = geometry.x(todayColumn))
                .size(geometry.columnWidth, geometry.height(state.gridStartMin, state.gridEndMin))
                .background(PlannerColors.TodayColumn, RoundedCornerShape(8.dp)),
        )
    }
    for (min in state.gridStartMin..state.gridEndMin step 60) {
        Box(
            Modifier.offset(AXIS_WIDTH, geometry.y(min)).fillMaxWidth().padding(end = AXIS_WIDTH + GRID_END_PADDING)
                .height(1.dp).background(scheme.surfaceVariant),
        )
        if (min < state.gridEndMin) {
            Text(
                "%02d".format(min / 60 % 24),
                style = AxisStyle,
                color = PlannerColors.Faint,
                textAlign = TextAlign.End,
                modifier = Modifier.offset(y = geometry.y(min) - 6.dp).width(AXIS_WIDTH - 4.dp),
            )
        }
    }
}

/** 현재 시각선. 블록에 가려지지 않도록 블록 다음에 그린다. */
@Composable
private fun NowLine(state: WeekUiState, now: LocalDateTime, geometry: GridGeometry) {
    val nowMin = now.minuteOfDay()
    if (!state.isThisWeek || nowMin !in state.gridStartMin..state.gridEndMin) return
    val x = geometry.x(now.dayOfWeek.value)
    val color = MaterialTheme.colorScheme.error
    Box(Modifier.offset(x, geometry.y(nowMin) - 1.dp).size(geometry.columnWidth, 2.dp).background(color))
    Box(Modifier.offset(x - 4.dp, geometry.y(nowMin) - 4.dp).size(8.dp).background(color, CircleShape))
}
