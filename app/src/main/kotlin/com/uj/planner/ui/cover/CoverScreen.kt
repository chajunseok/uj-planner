package com.uj.planner.ui.cover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.domain.minuteOfDay
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.theme.PlannerColors
import com.uj.planner.ui.today.FocusPrimaryButton
import com.uj.planner.ui.today.FocusText
import com.uj.planner.ui.today.TodayUiState

/**
 * 커버 화면(약 399×361dp). 카드 하나와 큰 버튼 하나 — 보지 않고 엄지로 눌러도 되게 한다.
 * 네 모서리는 카메라와 둥근 모서리에 가려지므로 28dp 안쪽에만 그린다. 내비게이션 스택을 갖지 않는다.
 */
@Composable
fun CoverScreen(state: TodayUiState, onAnswer: (Long, PlacementStatus) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        // 안전 여백 28dp 와 시스템 바 중 큰 쪽만큼 띄운다. 361dp 높이라 둘을 더하면 카드가 눌린다.
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets(28.dp, 28.dp, 28.dp, 28.dp)))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.now.minuteOfDay()), style = MaterialTheme.typography.labelMedium, color = PlannerColors.Muted)
                if (state.remaining > 0) {
                    Text("오늘 남은 ${state.remaining}개", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = PlannerColors.Muted)
                }
            }
            Spacer(Modifier.weight(1f))
            FocusText(
                state.focus,
                // 버튼이 없는 카드는 제목이 주인공이 아니라 한 단계 작게 쓴다.
                titleStyle = if (state.focus.placementId != null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusPrimaryButton(state.focus, height = 64.dp, onAnswer = onAnswer)
                val id = state.focus.placementId
                // 못함이 없는 카드에서도 자리는 남긴다. 주 버튼의 폭이 카드마다 달라지지 않게 한다.
                val canMiss = id != null && state.focus.kind.hasMissed
                TextButton(
                    onClick = { if (id != null) onAnswer(id, PlacementStatus.MISSED) },
                    enabled = canMiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = PlannerColors.Muted),
                    // 자리만 차지할 때는 화면 읽기에도 없는 것으로 친다.
                    modifier = Modifier.height(48.dp).alpha(if (canMiss) 1f else 0f).then(if (canMiss) Modifier else Modifier.clearAndSetSemantics {}),
                ) { Text("못함", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
