package com.uj.planner.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uj.planner.data.entity.DayAvailabilityEntity
import com.uj.planner.domain.GRID_MIN
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.components.SectionLabel
import com.uj.planner.ui.components.Stepper
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.theme.PlannerColors

private const val MIDNIGHT = 24 * 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    // 한 번에 한 요일만 펼친다. 0 은 모두 접힘.
    var expandedDay by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionLabel("배치 가능 시간대")
            Text(
                "가변 일정은 이 시간 안에만 들어가요. 종료가 자정을 넘으면 다음날로 표시돼요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopyButton("월 → 평일 전체") { viewModel.copy(from = 1, to = 1..5) }
                CopyButton("토 → 주말") { viewModel.copy(from = 6, to = 6..7) }
            }

            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column {
                    days.forEachIndexed { i, day ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DayRow(
                            day = day,
                            expanded = expandedDay == day.dayOfWeek,
                            onToggle = { expandedDay = if (expandedDay == day.dayOfWeek) 0 else day.dayOfWeek },
                            onShift = { start, end -> viewModel.shift(day, start, end) },
                        )
                    }
                }
            }

            Text(
                "완료한 일정은 두고, 이번 주 남은 가변 일정을 처음부터 다시 배치해요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
            )
            FilledTonalButton(onClick = viewModel::replan, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("이번 주 다시 짜기", Modifier.padding(start = 6.dp))
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }

            Text(
                "U.J planner ${appVersion()} · 데이터는 이 기기에만 저장돼요",
                style = MaterialTheme.typography.bodySmall,
                color = PlannerColors.Faint,
                modifier = Modifier.padding(vertical = 28.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}

@Composable
private fun CopyButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 40.dp)) {
        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(text, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DayRow(day: DayAvailabilityEntity, expanded: Boolean, onToggle: () -> Unit, onShift: (startSteps: Int, endSteps: Int) -> Unit) {
    val weekend = day.dayOfWeek >= 6
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).heightIn(min = 56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                DAY_NAMES[day.dayOfWeek - 1],
                style = MaterialTheme.typography.titleSmall,
                color = if (weekend) PlannerColors.Faint else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(40.dp),
            )
            Text("${formatTime(day.startMin)} – ${formatTime(day.endMin)}", style = MaterialTheme.typography.titleSmall)
            if (day.endMin > MIDNIGHT) {
                Text("+1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandMore else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = if (expanded) "접기" else "펼치기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeStepper("시작", day.startMin, canMinus = day.startMin > 0, canPlus = day.startMin + GRID_MIN < day.endMin) { onShift(it, 0) }
                TimeStepper("종료", day.endMin, canMinus = day.endMin - GRID_MIN > day.startMin, canPlus = day.endMin < LATEST_END_MIN) { onShift(0, it) }
            }
        }
    }
}

@Composable
private fun TimeStepper(label: String, min: Int, canMinus: Boolean, canPlus: Boolean, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(48.dp))
        Stepper(
            value = formatTime(min),
            onMinus = { onStep(-1) },
            onPlus = { onStep(1) },
            minusEnabled = canMinus,
            plusEnabled = canPlus,
            stepLabel = "$label 시각 30분",
        )
    }
}
