package com.uj.planner.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uj.planner.domain.GRID_MIN
import com.uj.planner.domain.model.PlannedSlot
import com.uj.planner.domain.model.Window
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.components.ChoiceChips
import com.uj.planner.ui.components.DurationPicker
import com.uj.planner.ui.components.SectionLabel
import com.uj.planner.ui.components.Segmented
import com.uj.planner.ui.components.Stepper
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.label
import com.uj.planner.ui.theme.PlannerColors
import com.uj.planner.ui.theme.TaskColor

private const val LAST_START = 23 * 60 + 30
private val PRIORITY_LABELS = mapOf(1 to "낮음", 2 to "보통", 3 to "높음")

private val Window.icon
    get() = when (this) {
        Window.MORNING -> Icons.Rounded.WbTwilight
        Window.AFTERNOON -> Icons.Rounded.LightMode
        Window.EVENING -> Icons.Rounded.Bedtime
        Window.ANY -> Icons.Rounded.AllInclusive
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(viewModel: EditViewModel, onClose: () -> Unit) {
    val isFixed = viewModel.kind == EditKind.FIXED
    val editingTitle = if (isFixed) viewModel.fixed.title else viewModel.flex.title

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "새 일정" else "$editingTitle 편집", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "닫기") } },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    viewModel.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { viewModel.save(onClose) },
                        enabled = viewModel.canSave,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text(if (isFixed) "저장" else "저장하고 배치") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (viewModel.isNew) {
                Segmented(
                    options = EditKind.entries,
                    selected = viewModel.kind,
                    onSelect = { viewModel.kind = it },
                    label = { if (it == EditKind.FIXED) "고정" else "가변" },
                )
            }
            Text(
                if (isFixed) "매주 같은 요일·시각에 반복돼요. 앱이 건드리지 않아요." else "조건만 정하면 언제 할지는 앱이 골라요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (isFixed) {
                FixedFields(viewModel.fixed) { viewModel.fixed = it }
            } else {
                val preview by viewModel.preview.collectAsStateWithLifecycle()
                FlexFields(viewModel.flex, preview) { viewModel.flex = it }
            }
            if (!viewModel.isNew) {
                TextButton(onClick = { viewModel.delete(onClose) }, modifier = Modifier.padding(vertical = 12.dp).align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text("이 일정 삭제", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit) {
    SectionLabel("이름")
    OutlinedTextField(value, onChange, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ColumnScope.FixedFields(form: FixedForm, onChange: (FixedForm) -> Unit) {
    NameField(form.title) { onChange(form.copy(title = it)) }

    SectionLabel("요일", hint = "여러 개 선택")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DAY_NAMES.forEachIndexed { i, name ->
            val day = i + 1
            val on = day in form.days
            val scheme = MaterialTheme.colorScheme
            Box(
                Modifier.size(44.dp)
                    .background(if (on) scheme.primary else scheme.surface, CircleShape)
                    .border(1.dp, if (on) scheme.primary else PlannerColors.FaintOutline, CircleShape)
                    .clickable(role = Role.Checkbox) { onChange(form.copy(days = if (on) form.days - day else form.days + day)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) scheme.onPrimary else if (i >= 5) PlannerColors.Faint else scheme.onSurface,
                )
            }
        }
    }

    SectionLabel("시작 시각")
    Stepper(
        value = formatTime(form.startMin),
        onMinus = { onChange(form.copy(startMin = form.startMin - GRID_MIN)) },
        onPlus = { onChange(form.copy(startMin = form.startMin + GRID_MIN)) },
        minusEnabled = form.startMin > 0,
        plusEnabled = form.startMin < LAST_START,
        stepLabel = "시작 시각 30분",
    )

    SectionLabel("소요시간")
    DurationPicker(form.durationMin) { onChange(form.copy(durationMin = it)) }

    if (form.days.isNotEmpty()) {
        val days = form.days.sorted().joinToString("·") { DAY_NAMES[it - 1] }
        SummaryLine("$days ${formatRange(form.startMin, form.startMin + form.durationMin)}")
    }
}

@Composable
private fun ColumnScope.FlexFields(form: FlexForm, preview: List<PlannedSlot>?, onChange: (FlexForm) -> Unit) {
    NameField(form.title) { onChange(form.copy(title = it)) }

    SectionLabel("색")
    TaskColor.entries.chunked(4).forEach { row ->
        Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { color ->
                val on = color.ordinal == form.colorIndex
                Box(
                    // 스와치는 28dp 지만 누르는 영역은 48dp.
                    Modifier.size(48.dp).clickable(role = Role.RadioButton) { onChange(form.copy(colorIndex = color.ordinal)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(28.dp).background(color.bg, CircleShape)
                            .border(if (on) 2.dp else 1.dp, if (on) color.fg else color.fg.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) Icon(Icons.Rounded.Check, contentDescription = "${color.label} 선택됨", tint = color.fg, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    SectionLabel("소요시간")
    DurationPicker(form.durationMin) { onChange(form.copy(durationMin = it)) }

    SectionLabel("주당 횟수")
    Stepper(
        value = "${form.timesPerWeek}회",
        onMinus = { onChange(form.copy(timesPerWeek = form.timesPerWeek - 1)) },
        onPlus = { onChange(form.copy(timesPerWeek = form.timesPerWeek + 1)) },
        minusEnabled = form.timesPerWeek > 1,
        plusEnabled = form.timesPerWeek < 7,
        stepLabel = "주당 횟수",
    )

    SectionLabel("우선순위")
    Segmented(PRIORITY_LABELS.keys.toList(), form.priority, { onChange(form.copy(priority = it)) }, label = { PRIORITY_LABELS.getValue(it) })

    SectionLabel("선호 시간대")
    Segmented(Window.entries, form.window, { onChange(form.copy(window = it)) }, label = { it.label }, icon = { it.icon })

    SectionLabel("마감 요일", hint = "이 요일까지 다 하도록 앞쪽에 배치")
    ChoiceChips(
        options = listOf<Pair<Int?, String>>(null to "없음") + DAY_NAMES.mapIndexed { i, name -> (i + 1) to name },
        selected = form.deadlineDay,
        onSelect = { onChange(form.copy(deadlineDay = it)) },
    )

    if (preview != null) {
        val placed = preview.joinToString(" · ") { "${DAY_NAMES[it.dayOfWeek - 1]} ${formatTime(it.startMin)}" }
        val missing = form.timesPerWeek - preview.size
        SummaryLine(
            when {
                preview.isEmpty() -> "지금 조건으로는 이번 주에 넣을 빈칸이 없어요."
                missing > 0 -> "이번 주 예상: $placed. ${missing}회는 빈칸이 모자라요."
                else -> "저장하면 이번 주 빈칸 ${preview.size}곳에 넣어요. 지금 예상: $placed"
            },
            icon = true,
        )
    }
}

/** 저장하면 어떻게 되는지 한 줄로 미리 보여 준다. */
@Composable
private fun SummaryLine(text: String, icon: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon) Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
