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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.planner.data.entity.PlacementStatus
import com.uj.planner.ui.theme.PlannerColors
import com.uj.planner.ui.theme.serif

private val DOT_SHAPE = RoundedCornerShape(4.dp)

/**
 * 색 점 + 한 줄 설명, 큰 제목, 부제. 커버와 플렉스가 글자 크기만 달리해 같이 쓴다.
 *
 * @param tint 색 있는 카드 위에 놓일 때의 글자색. null 이면 기본 글자색을 쓴다.
 */
@Composable
fun FocusText(focus: Focus, titleStyle: TextStyle, modifier: Modifier = Modifier, tint: Color? = null) {
    val accent = focus.color?.fg ?: if (focus.kind == Focus.Kind.ALL_DONE) PlannerColors.OnDoneContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            focus.color?.let {
                // 지금 할 일은 채운 점, 아직 시작 전이거나 지난 일정은 빈 점.
                val filled = focus.kind == Focus.Kind.NOW
                Box(Modifier.size(12.dp).background(if (filled) it.fg else it.bg, DOT_SHAPE).border(1.dp, it.fg, DOT_SHAPE))
            }
            Text(focus.kicker, style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1)
        }
        Text(focus.title, style = titleStyle.serif(), color = tint ?: MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (focus.sub.isNotEmpty()) {
            Text(
                focus.sub,
                fontSize = 15.sp,
                color = tint?.copy(alpha = 0.85f) ?: PlannerColors.Muted,
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
fun RowScope.FocusPrimaryButton(focus: Focus, height: Dp, weight: Float = 1f, onAnswer: (Long, PlacementStatus) -> Unit) {
    val id = focus.placementId ?: return
    val label = focus.kind.primaryLabel ?: return
    Button(
        onClick = { onAnswer(id, PlacementStatus.DONE) },
        shape = RoundedCornerShape(height * 0.32f),
        // 아직 시작 전인 일정의 "미리 완료" 는 주 동작이 아니다. 회색으로 한 단계 낮춘다.
        colors = if (focus.kind == Focus.Kind.NEXT) {
            ButtonDefaults.buttonColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = Modifier.weight(weight).height(height),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(if (height > 56.dp) 26.dp else 22.dp))
        Text(label, fontSize = if (height > 56.dp) 20.sp else 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}
