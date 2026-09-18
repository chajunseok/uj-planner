package com.uj.planner.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.ui.theme.PlannerColors

/** 색 점 + 한 줄 설명, 큰 제목, 부제. 커버와 플렉스가 글자 크기만 달리해 같이 쓴다. */
@Composable
fun FocusText(focus: Focus, titleStyle: TextStyle, modifier: Modifier = Modifier) {
    val accent = focus.color?.fg ?: if (focus.kind == Focus.Kind.ALL_DONE) PlannerColors.OnDoneContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            focus.color?.let {
                // 지금 할 일은 채운 점, 아직 시작 전이거나 지난 일정은 빈 점.
                val filled = focus.kind == Focus.Kind.NOW
                Box(Modifier.size(12.dp).background(if (filled) it.fg else it.bg, CircleShape).border(1.dp, it.fg, CircleShape))
            }
            Text(focus.kicker, style = MaterialTheme.typography.labelLarge, color = accent, maxLines = 1)
        }
        Text(focus.title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        if (focus.sub.isNotEmpty()) {
            Text(
                focus.sub,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 카드의 주 버튼. 누를 것이 없는 카드(끝·빈 상태)면 아무것도 그리지 않는다.
 * 못함 버튼은 모양이 화면마다 달라 각 화면이 [Focus.Kind.hasMissed] 를 보고 직접 그린다.
 */
@Composable
fun RowScope.FocusPrimaryButton(focus: Focus, height: Dp, onAnswer: (Long, PlacementStatus) -> Unit) {
    val id = focus.placementId ?: return
    val label = focus.kind.primaryLabel ?: return
    Button(
        onClick = { onAnswer(id, PlacementStatus.DONE) },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.weight(1f).height(height),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
    }
}
