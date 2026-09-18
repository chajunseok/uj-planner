package com.uj.planner.ui.flex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.theme.PlannerColors
import com.uj.planner.ui.today.Focus
import com.uj.planner.ui.today.FocusPrimaryButton
import com.uj.planner.ui.today.FocusText
import com.uj.planner.ui.today.TodayItem
import com.uj.planner.ui.today.TodayUiState
import com.uj.planner.ui.week.label

/** 힌지 위아래로 이만큼은 아무것도 두지 않는다. 접힌 자리는 굽어 있어서 글자와 버튼이 뭉개진다. */
private val HINGE_KEEP_OUT = 16.dp

/**
 * 플렉스 모드(반 접어 세움). 위는 보는 영역(오늘 타임라인), 아래는 누르는 영역(지금 카드와 버튼).
 *
 * @param hingeTop 창 위에서 힌지까지.
 * @param hingeBottom 창 위에서 힌지 아래 끝까지.
 */
@Composable
fun FlexScreen(state: TodayUiState, hingeTop: Dp, hingeBottom: Dp, onAnswer: (Long, PlacementStatus) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column {
            Column(Modifier.height((hingeTop - HINGE_KEEP_OUT).coerceAtLeast(0.dp)).statusBarsPadding().padding(horizontal = 20.dp)) {
                val date = state.now.toLocalDate()
                Text("${DAY_NAMES[date.dayOfWeek.value - 1]}요일", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                Text(
                    "${date.monthValue}월 ${date.dayOfMonth}일 · " + if (state.remaining > 0) "남은 일정 ${state.remaining}개" else "남은 일정 없음",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.padding(top = 12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.items.forEach { TimelineRow(it) }
                }
            }
            // 접히는 도중에는 힌지 좌표가 뒤집혀 올 수 있다. 음수 높이는 레이아웃에서 예외가 된다.
            Spacer(Modifier.height((hingeBottom - hingeTop + HINGE_KEEP_OUT * 2).coerceAtLeast(0.dp)))
            Column(Modifier.fillMaxSize().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                FocusCard(state.focus, Modifier.weight(1f))
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusPrimaryButton(state.focus, height = 56.dp, onAnswer = onAnswer)
                    val id = state.focus.placementId
                    if (id != null && state.focus.kind.hasMissed) {
                        FilledTonalButton(
                            onClick = { onAnswer(id, PlacementStatus.MISSED) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(56.dp),
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("못함", Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusCard(focus: Focus, modifier: Modifier = Modifier) {
    val container = when {
        focus.kind == Focus.Kind.ALL_DONE -> PlannerColors.DoneContainer
        focus.kind == Focus.Kind.NOW && focus.color != null -> focus.color.bg
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    Surface(color = container, shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Box(Modifier.padding(20.dp), contentAlignment = Alignment.CenterStart) {
            FocusText(focus, titleStyle = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun TimelineRow(item: TodayItem) {
    val scheme = MaterialTheme.colorScheme
    val done = item.status == PlacementStatus.DONE
    val gone = item.status == PlacementStatus.DROPPED || item.status == PlacementStatus.MISSED
    val fg = if (gone) PlannerColors.Faint else item.color?.fg ?: scheme.onSurfaceVariant
    Row(Modifier.alpha(if (done) 0.6f else 1f), verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatTime(item.startMin),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (item.isNow) FontWeight.Bold else FontWeight.Medium,
            color = if (item.isNow) scheme.error else PlannerColors.Faint,
            modifier = Modifier.width(52.dp),
        )
        Row(
            Modifier.weight(1f).heightIn(min = 44.dp)
                .background(if (gone) scheme.surface else item.color?.bg ?: scheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = fg,
                    textDecoration = if (done || item.status == PlacementStatus.DROPPED) TextDecoration.LineThrough else null,
                    maxLines = 1,
                )
                Text(
                    formatRange(item.startMin, item.endMin) + if (item.color == null) " · 고정" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = fg.copy(alpha = 0.8f),
                )
            }
            val icon = when (item.status) {
                PlacementStatus.DONE -> Icons.Rounded.CheckCircle
                PlacementStatus.MISSED -> Icons.Rounded.Replay
                PlacementStatus.DROPPED -> Icons.Rounded.Close
                else -> null
            }
            if (icon != null) Icon(icon, contentDescription = item.status.label, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}
