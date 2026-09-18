package com.uj.planner.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.abbreviate
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.theme.BlockSubStyle
import com.uj.planner.ui.theme.PlannerColors

private val BLOCK_SHAPE = RoundedCornerShape(6.dp)

/** 한 시간 블록의 높이. 이보다 낮으면 2행(시작 시각)을 그리지 않는다. */
private const val TWO_LINE_MIN = 60

val PlacementStatus?.label: String
    get() = when (this) {
        null -> "고정"
        PlacementStatus.PLANNED -> "예정"
        PlacementStatus.DONE -> "완료"
        PlacementStatus.MISSED -> "못함"
        PlacementStatus.DROPPED -> "버림"
    }

/** 블록 바깥 둘레에 [gap] 만큼 띄워 그리는 둥근 테두리. */
private fun DrawScope.outline(color: Color, width: Dp, gap: Dp = 0.dp, dashed: Boolean = false) {
    val inset = width.toPx() / 2 - gap.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = CornerRadius(6.dp.toPx() + gap.toPx()),
        style = Stroke(
            width = width.toPx(),
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
        ),
    )
}

/**
 * 그리드의 블록 하나. 53dp 열에는 이름이 들어가지 않으므로 색과 앞 두 글자로 구분하고, 상태는 아이콘과 테두리로 보인다.
 * 고정 일정은 대개 길어서 한 시간 이상이면 이름을 다 쓴다.
 */
@Composable
fun BlockView(block: WeekBlock, inProgress: Boolean, dragging: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val status = block.status
    val duration = block.endMin - block.startMin
    val fg = when {
        block.color == null -> scheme.onSurfaceVariant
        status == PlacementStatus.DROPPED -> PlannerColors.Faint
        else -> block.color.fg
    }
    val bg = when {
        block.color == null -> scheme.surfaceVariant
        status == PlacementStatus.MISSED -> scheme.surface
        status == PlacementStatus.DROPPED -> Color.Transparent
        else -> block.color.bg
    }
    val icon = when {
        dragging -> Icons.Rounded.OpenWith
        status == PlacementStatus.DONE -> Icons.Rounded.CheckCircle
        status == PlacementStatus.MISSED -> Icons.Rounded.Replay
        status == PlacementStatus.DROPPED -> Icons.Rounded.Close
        else -> null
    }
    val struck = status == PlacementStatus.DONE || status == PlacementStatus.DROPPED
    val description = "${block.title}, ${DAY_NAMES[block.dayOfWeek - 1]}요일 ${formatRange(block.startMin, block.endMin)}, ${status.label}"

    Box(
        modifier
            .semantics { contentDescription = description }
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .alpha(if (status == PlacementStatus.DONE) 0.6f else 1f)
            .drawBehind {
                // 진행 중: 배경색 2dp 를 띄우고 그 바깥에 글자색 2dp 링.
                if (inProgress && !dragging) outline(fg, 2.dp, gap = 4.dp)
            }
            .background(bg, BLOCK_SHAPE)
            .drawBehind {
                when {
                    dragging -> outline(fg, 2.dp)
                    status == PlacementStatus.MISSED -> outline(fg, 2.dp, dashed = true)
                    status == PlacementStatus.DROPPED -> outline(PlannerColors.FaintOutline, 1.dp)
                }
            }
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = if (block.isFixed && duration >= TWO_LINE_MIN) block.title else abbreviate(block.title),
                    style = MaterialTheme.typography.labelSmall,
                    color = fg,
                    textDecoration = if (struck) TextDecoration.LineThrough else null,
                    maxLines = if (block.isFixed && duration >= TWO_LINE_MIN * 2) 2 else 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
                if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
            }
            if (duration >= TWO_LINE_MIN) {
                Text(formatTime(block.startMin), style = BlockSubStyle, color = fg.copy(alpha = 0.8f), maxLines = 1)
            }
        }
    }
}

/** 끌려 나간 블록의 원래 자리. */
@Composable
fun BlockGhost(block: WeekBlock, modifier: Modifier = Modifier) {
    val color = block.color?.fg ?: MaterialTheme.colorScheme.outline
    Box(
        modifier.padding(horizontal = 2.dp, vertical = 1.dp).alpha(0.6f).fillMaxSize()
            .drawBehind { outline(color, 1.dp, dashed = true) },
    )
}

/** 블록을 탭하면 뜨는 카드. 그리드에서는 이름이 두 글자로 줄어 있으므로 여기서 전체를 보여 준다. */
@Composable
fun BlockDetailCard(
    block: WeekBlock,
    canAct: Boolean,
    onDone: () -> Unit,
    onDrop: () -> Unit,
    onEdit: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 3.dp) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val dot = block.color
                Box(
                    Modifier.size(12.dp).background(dot?.bg ?: MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(1.dp, dot?.fg ?: MaterialTheme.colorScheme.outline, CircleShape),
                )
                Text(block.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${DAY_NAMES[block.dayOfWeek - 1]} ${formatRange(block.startMin, block.endMin)} · ${block.status.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (canAct && block.status == PlacementStatus.PLANNED) {
                    TextButton(onClick = onDrop) { Text("버림") }
                    TextButton(onClick = onDone) { Text("완료") }
                }
                TextButton(onClick = onEdit) { Text("편집") }
            }
        }
    }
}
