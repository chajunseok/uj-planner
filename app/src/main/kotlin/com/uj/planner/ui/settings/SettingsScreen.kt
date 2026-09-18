package com.uj.planner.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uj.planner.data.entity.DayAvailabilityEntity
import com.uj.planner.domain.GRID_MIN
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.components.ScreenHeader
import com.uj.planner.ui.components.Stepper
import com.uj.planner.ui.components.outlinedBox
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.theme.PlannerColors

private const val MIDNIGHT = 24 * 60

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    // 한 번에 한 요일만 펼친다. 0 은 모두 접힘.
    var expandedDay by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(topBar = { ScreenHeader("설정", Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text("배치 가능 시간대", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Text(
                    "가변 일정은 이 시간 안에만 들어가요. 종료가 자정을 넘으면 다음날로 표시돼요.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = PlannerColors.Muted,
                )
                Row(Modifier.padding(top = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CopyButton("월 → 평일 전체", enabled = days.isNotEmpty()) { viewModel.copy(from = 1, to = 1..5) }
                    CopyButton("토 → 주말", enabled = days.isNotEmpty()) { viewModel.copy(from = 6, to = 6..7) }
                }
                days.forEach { day ->
                    DayRow(
                        day = day,
                        expanded = expandedDay == day.dayOfWeek,
                        onToggle = { expandedDay = if (expandedDay == day.dayOfWeek) 0 else day.dayOfWeek },
                        onShift = { start, end -> viewModel.shift(day.dayOfWeek, start, end) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            Text(
                message ?: "완료한 일정은 두고, 이번 주 남은 가변 일정을 처음부터 다시 배치해요.",
                style = MaterialTheme.typography.bodySmall,
                color = PlannerColors.Muted,
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
            )
            OutlinedButton(
                onClick = viewModel::replan,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(22.dp))
                Text("이번 주 다시 짜기", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "U.J planner ${appVersion()} · 데이터는 이 기기에만 저장돼요",
                fontSize = 11.sp,
                color = PlannerColors.Faint,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    // 버전 줄 하나 때문에 설정 화면이 죽지 않게 한다. 못 읽으면 비워 둔다.
    return remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
}

@Composable
private fun CopyButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, PlannerColors.FaintOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        contentPadding = PaddingValues(horizontal = 14.dp),
        modifier = Modifier.height(40.dp),
    ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TimeBox(min: Int, modifier: Modifier) {
    Row(modifier.height(44.dp).outlinedBox(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(formatTime(min), style = MaterialTheme.typography.titleSmall)
        // 자정을 넘긴 종료 시각은 다음날이다.
        if (min > MIDNIGHT) Text("+1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF7A5300), modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun DayRow(day: DayAvailabilityEntity, expanded: Boolean, onToggle: () -> Unit, onShift: (startSteps: Int, endSteps: Int) -> Unit) {
    val weekend = day.dayOfWeek >= 6
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = if (expanded) "접기" else "펼치기", role = Role.Button, onClick = onToggle).height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                DAY_NAMES[day.dayOfWeek - 1],
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (weekend) PlannerColors.Faint else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(36.dp),
            )
            TimeBox(day.startMin, Modifier.weight(1f))
            Text("–", fontSize = 14.sp, color = PlannerColors.Faint)
            TimeBox(day.endMin, Modifier.weight(1f))
            Box(Modifier.size(40.dp, 44.dp), contentAlignment = Alignment.Center) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandMore else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PlannerColors.Faint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 46.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepRow("시작", day.startMin, canMinus = day.startMin > 0, canPlus = day.startMin + GRID_MIN < day.endMin) { onShift(it, 0) }
                StepRow("종료", day.endMin, canMinus = day.endMin - GRID_MIN > day.startMin, canPlus = day.endMin < LATEST_END_MIN) { onShift(0, it) }
            }
        }
    }
}

@Composable
private fun StepRow(label: String, min: Int, canMinus: Boolean, canPlus: Boolean, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
        Stepper(
            value = formatTime(min),
            onMinus = { onStep(-1) },
            onPlus = { onStep(1) },
            modifier = Modifier.weight(1f),
            minusEnabled = canMinus,
            plusEnabled = canPlus,
            stepLabel = "$label 시각 30분",
        )
    }
}
