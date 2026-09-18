package com.uj.planner.ui.missed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.ui.formatDuration
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
        onPauseOrDispose {}
    }

    if (state.sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val total = state.pending.size + state.resolved.size
                Text(
                    if (state.pending.isEmpty()) "지난 일정을 모두 확인했어요" else "지난 일정 ${total}개, 어떻게 됐어요?",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "못한 건 이번 주 남은 빈칸에 다시 넣어 드려요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.resolved.forEach { line -> ResolvedRow(line, onUndo = { viewModel.undo(line) }) }
                state.pending.forEach { item -> PendingCard(item, onAnswer = { viewModel.answer(item, it) }) }

                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.pending.isNotEmpty()) {
                        FilledTonalButton(onClick = viewModel::answerAllDone, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("남은 ${state.pending.size}개 모두 했음")
                        }
                    }
                    TextButton(onClick = viewModel::dismiss, modifier = Modifier.height(48.dp)) {
                        Text(if (state.pending.isEmpty()) "닫기" else "나중에")
                    }
                }
            }
        }
    }

    state.noRoom?.let { item ->
        AlertDialog(
            // 바깥을 눌러 닫으면 '못함' 인 채로 남는다. 그리드에 점선 블록으로 보이고 주를 다시 짜면 다시 시도된다.
            onDismissRequest = viewModel::keepNoRoom,
            icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
            title = { Text("이번 주엔 빈칸이 없어요", style = MaterialTheme.typography.titleLarge) },
            text = {
                val duration = formatDuration(item.placement.endMin - item.placement.startMin)
                Text(
                    "${item.title} ${duration}을 넣을 자리가 남지 않았어요. 이미 한 일정은 그대로 두고, 남은 가변 일정을 전부 다시 짤까요?\n\n" +
                        "다시 짜면 예정된 가변 일정의 자리가 바뀔 수 있어요.",
                )
            },
            confirmButton = {
                Button(onClick = viewModel::replanNoRoom) {
                    Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("다시 짜기", Modifier.padding(start = 6.dp))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dropNoRoom) { Text("이번 주는 버림") } },
        )
    }
}

@Composable
private fun ResolvedRow(line: ResolvedLine, onUndo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = PlannerColors.OnDoneContainer, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(line.text, style = MaterialTheme.typography.labelLarge)
            Text(line.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (line.undoable) TextButton(onClick = onUndo) { Text("되돌리기") }
    }
}

@Composable
private fun PendingCard(item: MissedItem, onAnswer: (PlacementStatus) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(12.dp).background(item.color.bg, CircleShape).border(1.dp, item.color.fg, CircleShape))
                Column {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Text(item.whenText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // 했음이 가장 흔한 답이라 가장 넓다 (1.3 : 1 : 1).
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAnswer(PlacementStatus.DONE) }, modifier = Modifier.weight(1.3f).height(48.dp)) {
                    AnswerLabel(Icons.Rounded.Check, "했음")
                }
                FilledTonalButton(onClick = { onAnswer(PlacementStatus.MISSED) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    AnswerLabel(Icons.Rounded.Replay, "못함")
                }
                OutlinedButton(onClick = { onAnswer(PlacementStatus.DROPPED) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    AnswerLabel(Icons.Rounded.Close, "버림")
                }
            }
        }
    }
}

@Composable
private fun AnswerLabel(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Text(text, Modifier.padding(start = 4.dp), maxLines = 1)
}
