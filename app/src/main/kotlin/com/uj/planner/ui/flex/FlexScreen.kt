package com.uj.planner.ui.flex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.icons.UjIcons
import com.uj.planner.ui.theme.PlannerColors
import com.uj.planner.ui.today.Focus
import com.uj.planner.ui.today.FocusPrimaryButton
import com.uj.planner.ui.today.FocusText
import com.uj.planner.ui.today.TodayItem
import com.uj.planner.ui.today.TodayUiState
import com.uj.planner.ui.week.label
import com.uj.planner.ui.week.outline
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

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
            Column(Modifier.height((hingeTop - HINGE_KEEP_OUT).coerceAtLeast(0.dp)).statusBarsPadding()) {
                val date = state.now.date
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${DAY_NAMES[date.dayOfWeek.isoDayNumber - 1]}요일", style = MaterialTheme.typography.titleLarge, modifier = Modifier.alignByBaseline())
                    Text(
                        "${date.month.number}월 ${date.day}일 · " + if (state.remaining > 0) "남은 일정 ${state.remaining}개" else "남은 일정 없음",
                        fontSize = 13.sp,
                        color = PlannerColors.Muted,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp).verticalScroll(rememberScrollState())) {
                    state.items.forEachIndexed { i, item -> TimelineRow(item, last = i == state.items.lastIndex) }
                }
            }
            // 접히는 도중에는 힌지 좌표가 뒤집혀 올 수 있다. 음수 높이는 레이아웃에서 예외가 된다.
            Spacer(Modifier.height((hingeBottom - hingeTop + HINGE_KEEP_OUT * 2).coerceAtLeast(0.dp)))
            Column(
                Modifier.fillMaxSize().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    when (state.focus.kind) {
                        Focus.Kind.OVERDUE -> "지난 일정 · 어떻게 됐어요?"
                        Focus.Kind.NOW -> "지금"
                        Focus.Kind.NEXT -> "다음"
                        Focus.Kind.ALL_DONE, Focus.Kind.EMPTY -> "오늘"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerColors.Muted,
                )
                FocusCard(state.focus, Modifier.weight(1f))
                val id = state.focus.placementId
                if (id != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusPrimaryButton(state.focus, height = 56.dp, weight = 1.6f, onAnswer = onAnswer)
                        if (state.focus.kind.hasMissed) {
                            OutlinedButton(
                                onClick = { onAnswer(id, PlacementStatus.MISSED) },
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, PlannerColors.FaintOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.weight(1f).height(56.dp),
                            ) {
                                Icon(UjIcons.Replay, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("못함", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusCard(focus: Focus, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    // 지금 할 일만 그 일정의 색으로 채운다. 물어보는 카드(지난 일정·다음)는 흰 바탕에 테두리.
    val (container, tint) = when {
        focus.kind == Focus.Kind.ALL_DONE -> PlannerColors.DoneContainer to PlannerColors.OnDoneContainer
        focus.kind == Focus.Kind.NOW && focus.color != null -> focus.color.bg to focus.color.fg
        else -> scheme.surfaceContainerLowest to null
    }
    Box(
        modifier.fillMaxWidth().background(container, shape)
            .border(1.dp, if (tint == null) scheme.surfaceVariant else Color.Transparent, shape).padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        FocusText(focus, titleStyle = MaterialTheme.typography.headlineSmall, tint = tint)
    }
}

/** 시각 · 점과 세로줄 · 카드. 지금 하는 일정은 시각이 빨갛고 점에 고리가 둘린다. */
@Composable
private fun TimelineRow(item: TodayItem, last: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val done = item.status == PlacementStatus.DONE
    val missed = item.status == PlacementStatus.MISSED
    val dropped = item.status == PlacementStatus.DROPPED
    val fg = if (dropped) PlannerColors.Faint else item.color?.fg ?: scheme.onSurfaceVariant
    val shape = RoundedCornerShape(10.dp)
    Row(Modifier.height(IntrinsicSize.Min).heightIn(min = 54.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            formatTime(item.startMin),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (item.isNow) FontWeight.Bold else FontWeight.Medium,
            color = if (item.isNow) scheme.error else PlannerColors.Faint,
            modifier = Modifier.width(38.dp).padding(top = 5.dp),
        )
        Column(Modifier.width(12.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.padding(top = 8.dp).size(8.dp)
                    .then(if (item.isNow) Modifier.drawBehind { drawCircle(scheme.error, radius = 6.5.dp.toPx(), style = Stroke(2.dp.toPx())) } else Modifier)
                    .background(fg, CircleShape),
            )
            if (!last) Box(Modifier.padding(top = 4.dp).width(2.dp).weight(1f).background(scheme.outlineVariant))
        }
        Row(
            Modifier.weight(1f).padding(bottom = 8.dp).alpha(if (done) 0.6f else 1f)
                .background(if (missed || dropped) scheme.surface else item.color?.bg ?: scheme.surfaceVariant, shape)
                .then(
                    when {
                        item.isNow -> Modifier.border(2.dp, fg, shape)
                        missed -> Modifier.drawBehind { outline(fg, 2.dp, dashed = true, radius = 10.dp) }
                        dropped -> Modifier.border(1.dp, PlannerColors.FaintOutline, shape)
                        else -> Modifier
                    },
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    textDecoration = if (done || dropped) TextDecoration.LineThrough else null,
                    maxLines = 1,
                )
                Text(
                    formatRange(item.startMin, item.endMin) + if (item.color == null) " · 고정" else if (missed) " · 지났어요" else "",
                    fontSize = 11.sp,
                    color = fg.copy(alpha = 0.8f),
                )
            }
            val icon = when (item.status) {
                PlacementStatus.DONE -> UjIcons.CheckCircle
                PlacementStatus.MISSED -> UjIcons.Replay
                PlacementStatus.DROPPED -> UjIcons.Close
                else -> null
            }
            if (icon != null) Icon(icon, contentDescription = item.status.label, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}
