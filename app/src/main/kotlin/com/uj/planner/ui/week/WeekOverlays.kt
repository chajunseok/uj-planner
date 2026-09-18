package com.uj.planner.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.planner.domain.model.Window
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.formatDuration
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.label
import com.uj.planner.ui.theme.PlannerColors
import com.uj.planner.ui.theme.TaskColor

private val BANNER_SHAPE = RoundedCornerShape(12.dp)
private val BANNER_OPEN_SHAPE = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

/** 블록을 끄는 동안 뜨는 안내. 지금 가리키는 자리를 글로 알려 준다. */
@Composable
fun MoveBanner(block: WeekBlock, target: MoveTarget?) {
    BannerRow(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) {
        Icon(Icons.Rounded.OpenWith, contentDescription = null, modifier = Modifier.size(20.dp))
        Text("${block.title} · 놓을 자리로 끌어 주세요", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        if (target != null) {
            Text(
                if (target.valid) "${DAY_NAMES[target.dayOfWeek - 1]} ${formatRange(target.startMin, target.endMin)}" else "놓을 수 없어요",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** 자리를 못 받은 일정이 있는 동안에는 닫을 수 없다. 접을 수만 있다. 상세는 [UnplacedDetails] 가 그리드 위에 덮어 그린다. */
@Composable
fun UnplacedBanner(items: List<UnplacedItem>, expanded: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    BannerRow(
        scheme.tertiaryContainer, scheme.onTertiaryContainer,
        shape = if (expanded) BANNER_OPEN_SHAPE else BANNER_SHAPE,
        modifier = Modifier.clickable(onClickLabel = if (expanded) "상세 닫기" else "상세 보기", onClick = onToggle),
    ) {
        Icon(Icons.Outlined.EventBusy, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            buildAnnotatedString {
                append("빈 시간이 모자라 ")
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("${items.sumOf { it.missing }}회") }
                append(" 못 넣었어요")
            },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
        )
        Text(if (expanded) "닫기" else "보기", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.padding(start = 2.dp).size(16.dp),
        )
    }
}

/** 배너에서 내려오는 상세. 그리드를 밀지 않고 위에 덮는다. */
@Composable
fun UnplacedDetails(items: List<UnplacedItem>, onChangeConditions: (Long) -> Unit, onReplan: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    Surface(
        shape = shape,
        color = scheme.surfaceContainerLowest,
        shadowElevation = 8.dp,
        modifier = modifier.padding(horizontal = 12.dp).fillMaxWidth().border(1.dp, scheme.surfaceVariant, shape),
    ) {
        Column {
            items.forEachIndexed { i, (task, missing) ->
                if (i > 0) HorizontalDivider(color = scheme.surfaceVariant)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val color = TaskColor.of(task.colorIndex)
                        Box(Modifier.size(12.dp).background(color.bg, RoundedCornerShape(3.dp)).border(1.dp, color.fg, RoundedCornerShape(3.dp)))
                        Text(task.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            buildAnnotatedString {
                                append("주 ${task.timesPerWeek}회 중 ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = scheme.onSurface)) { append("${missing}회") }
                                append(" 못 넣음")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = PlannerColors.Muted,
                        )
                    }
                    val window = if (task.window == Window.ANY) "시간대 무관" else "${task.window.label} 선호"
                    val deadline = task.deadlineDay?.let { "${DAY_NAMES[it - 1]}요일까지" } ?: "마감 없음"
                    Text(
                        "${formatDuration(task.durationMin)} · $window · $deadline. 하루에 한 번씩만 넣어서, 남은 요일이나 빈 시간이 모자랐어요.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = PlannerColors.Body,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onChangeConditions(task.id) }, modifier = Modifier.weight(1f).height(40.dp)) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("조건 바꾸기", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
                        }
                        Button(onClick = onReplan, modifier = Modifier.weight(1f).height(40.dp)) {
                            Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("다시 짜기", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerRow(container: Color, content: Color, modifier: Modifier = Modifier, shape: Shape = BANNER_SHAPE, body: @Composable RowScope.() -> Unit) {
    Surface(color = container, contentColor = content, shape = shape, modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(44.dp).then(modifier)) {
        Row(
            Modifier.padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = body,
        )
    }
}

/** 처음 실행. 빈 그리드 위에 카드로 띄운다. */
@Composable
fun EmptyCard(onAddFixed: () -> Unit, onAddFlex: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = scheme.surfaceContainerLowest,
        shadowElevation = 6.dp,
        modifier = modifier.padding(start = 44.dp, end = 12.dp).fillMaxWidth().border(1.dp, scheme.surfaceVariant, shape),
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).background(scheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ViewWeek, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
            Text("먼저 움직이지 않는 일정을 넣어 주세요", fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                buildAnnotatedString {
                    append("출근, 수업, 식사처럼 매주 같은 시간에 있는 ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = scheme.onSurface)) { append("고정 일정") }
                    append("이 회색 배경이 돼요. 그 사이 빈 시간에 가변 일정을 넣어 드릴게요.")
                },
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = PlannerColors.Body,
            )
            Button(onClick = onAddFixed, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 6.dp).fillMaxWidth().height(48.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("고정 일정 추가", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
            }
            TextButton(onClick = onAddFlex, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(40.dp)) {
                Text("가변 일정부터 넣기", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** 하단 바의 "이번 주". 테두리만 있는 알약. */
@Composable
fun ThisWeekPill(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(40.dp).border(1.dp, PlannerColors.FaintOutline, CircleShape)
            .clickable(enabled = enabled, onClickLabel = "이번 주로", onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text("이번 주", style = MaterialTheme.typography.labelMedium) }
}
