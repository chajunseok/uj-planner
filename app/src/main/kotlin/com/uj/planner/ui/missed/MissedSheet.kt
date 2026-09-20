package com.uj.planner.ui.missed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.ui.formatDuration
import com.uj.planner.ui.icons.UjIcons
import com.uj.planner.ui.theme.PlannerColors

/**
 * 앱에 들어올 때마다 끝난 일정을 모아 묻는 시트. 목적지가 아니라 주간 화면 위에 뜬다.
 * 답은 누르는 즉시 반영되고, 결과가 위에 한 줄씩 쌓인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissedSheet(viewModel: MissedViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        // 접어서 커버·플렉스로 가면 이 시트가 컴포지션에서 빠질 뿐 앱을 나간 것이 아니다. 그때는 "나중에" 를 풀지 않는다.
        onPauseOrDispose { if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewModel.undismiss() }
    }

    if (state.sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val total = state.pending.size + state.resolved.size
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (state.pending.isEmpty()) "지난 일정을 모두 확인했어요" else "지난 일정 ${total}개, 어떻게 됐어요?",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("못한 건 이번 주 남은 빈칸에 다시 넣어 드려요.", fontSize = 13.sp, color = PlannerColors.Muted)
                }
                state.error?.let {
                    Text("반영하지 못했어요: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
                state.resolved.forEach { line -> ResolvedRow(line, onUndo = { viewModel.undo(line) }) }
                state.pending.forEach { item -> PendingCard(item, onAnswer = { viewModel.answer(item, it) }) }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (state.pending.isNotEmpty()) {
                        TextButton(onClick = viewModel::answerAllDone, modifier = Modifier.height(44.dp)) {
                            Text("남은 ${state.pending.size}개 모두 했음", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = viewModel::dismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = PlannerColors.Muted),
                        modifier = Modifier.height(44.dp),
                    ) { Text(if (state.pending.isEmpty()) "닫기" else "나중에", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }

    state.noRoom?.let { item ->
        val duration = formatDuration(item.placement.endMin - item.placement.startMin)
        AlertDialog(
            // 바깥을 눌러 닫으면 '못함' 인 채로 남는다. 그리드에 점선 블록으로 보이고 주를 다시 짜면 다시 시도된다.
            onDismissRequest = viewModel::keepNoRoom,
            containerColor = MaterialTheme.colorScheme.surface,
            // 아이콘 칸을 쓰면 아이콘과 제목이 가운데로 간다. 시안은 왼쪽 정렬이라 제목 칸에 같이 넣는다.
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(UjIcons.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer) }
                    Text("이번 주엔 빈칸이 없어요", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) { append("${item.title} $duration") }
                            append("을 넣을 자리가 남지 않았어요. 이미 한 일정은 그대로 두고, 남은 가변 일정을 전부 다시 짤까요?")
                        },
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = PlannerColors.Body,
                    )
                    Text("다시 짜면 예정된 가변 일정의 자리가 바뀔 수 있어요.", style = MaterialTheme.typography.bodySmall, color = PlannerColors.Faint)
                }
            },
            confirmButton = {
                Button(onClick = viewModel::replanNoRoom, modifier = Modifier.height(44.dp)) {
                    Icon(UjIcons.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("다시 짜기", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dropNoRoom, modifier = Modifier.height(44.dp)) { Text("이번 주는 버림", style = MaterialTheme.typography.labelLarge) }
            },
        )
    }
}

@Composable
private fun ResolvedRow(line: ResolvedLine, onUndo: () -> Unit) {
    val fg = PlannerColors.OnDoneContainer
    Row(
        Modifier.fillMaxWidth().background(PlannerColors.DoneContainer, RoundedCornerShape(18.dp)).padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(UjIcons.CheckCircle, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(line.text, style = MaterialTheme.typography.labelLarge, color = fg)
            Text(line.detail, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
        }
        if (line.undoable) {
            TextButton(onClick = onUndo, colors = ButtonDefaults.textButtonColors(contentColor = fg), modifier = Modifier.height(36.dp)) {
                Text("되돌리기", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PendingCard(item: MissedItem, onAnswer: (PlacementStatus) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val button = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest, shape)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, shape).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(6.dp, 36.dp).background(item.color.bg, RoundedCornerShape(3.dp)).border(1.dp, item.color.fg, RoundedCornerShape(3.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(item.whenText(), style = MaterialTheme.typography.bodySmall, color = PlannerColors.Muted)
            }
        }
        // 했음이 가장 흔한 답이라 가장 넓다 (1.3 : 1 : 1).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val pad = PaddingValues(horizontal = 4.dp)
            Button(onClick = { onAnswer(PlacementStatus.DONE) }, shape = button, contentPadding = pad, modifier = Modifier.weight(1.3f).height(48.dp)) {
                AnswerLabel(UjIcons.Check, "했음")
            }
            FilledTonalButton(onClick = { onAnswer(PlacementStatus.MISSED) }, shape = button, contentPadding = pad, modifier = Modifier.weight(1f).height(48.dp)) {
                AnswerLabel(UjIcons.Replay, "못함")
            }
            OutlinedButton(
                onClick = { onAnswer(PlacementStatus.DROPPED) },
                shape = button,
                contentPadding = pad,
                border = BorderStroke(1.dp, PlannerColors.FaintOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.weight(1f).height(48.dp),
            ) { AnswerLabel(UjIcons.Close, "버림") }
        }
    }
}

@Composable
private fun AnswerLabel(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp), maxLines = 1)
}
