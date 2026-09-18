package com.uj.planner.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uj.planner.domain.GRID_MIN
import com.uj.planner.domain.model.PlannedSlot
import com.uj.planner.domain.model.Window
import com.uj.planner.ui.DAY_NAMES
import com.uj.planner.ui.components.ChoiceChips
import com.uj.planner.ui.components.DurationPicker
import com.uj.planner.ui.components.FieldShape
import com.uj.planner.ui.components.ScreenHeader
import com.uj.planner.ui.components.SectionLabel
import com.uj.planner.ui.components.Segmented
import com.uj.planner.ui.components.Stepper
import com.uj.planner.ui.components.TimeStepper
import com.uj.planner.ui.formatRange
import com.uj.planner.ui.formatTime
import com.uj.planner.ui.label
import com.uj.planner.ui.theme.PlannerColors
import com.uj.planner.ui.theme.TaskColor

private const val LAST_START = 23 * 60 + 30
private val PRIORITY_LABELS = mapOf(1 to "낮음", 2 to "보통", 3 to "높음")

private val Window.icon
    get() = when (this) {
        Window.MORNING -> Icons.Outlined.WbTwilight
        Window.AFTERNOON -> Icons.Outlined.LightMode
        Window.EVENING -> Icons.Outlined.Bedtime
        Window.ANY -> Icons.Rounded.AllInclusive
    }

@Composable
fun EditScreen(viewModel: EditViewModel, onClose: () -> Unit) {
    val isFixed = viewModel.kind == EditKind.FIXED
    val editingTitle = if (isFixed) viewModel.fixed.title else viewModel.flex.title

    Scaffold(
        topBar = { ScreenHeader(if (viewModel.isNew) "새 일정" else "$editingTitle 편집", Icons.Rounded.Close, "닫기", onClose) },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Column(
                    Modifier.navigationBarsPadding().imePadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    viewModel.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { viewModel.save(onClose) },
                        enabled = viewModel.canSave,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text(if (isFixed) "저장" else "저장하고 배치", style = MaterialTheme.typography.titleSmall) }
                    if (!viewModel.isNew) {
                        TextButton(
                            onClick = { viewModel.delete(onClose) },
                            enabled = !viewModel.busy,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("이 일정 삭제", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            if (viewModel.isNew) {
                Segmented(
                    options = EditKind.entries,
                    selected = viewModel.kind,
                    onSelect = { viewModel.kind = it },
                    label = { if (it == EditKind.FIXED) "고정" else "가변" },
                    modifier = Modifier.padding(top = 4.dp),
                    height = 40.dp,
                    checkSelected = true,
                )
            }
            Text(
                if (isFixed) "매주 같은 요일·시각에 반복돼요. 앱이 건드리지 않아요." else "조건만 정하면 언제 할지는 앱이 골라요.",
                style = MaterialTheme.typography.bodySmall,
                color = PlannerColors.Muted,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (isFixed) {
                FixedFields(viewModel.fixed) { viewModel.fixed = it }
            } else {
                val preview by viewModel.preview.collectAsStateWithLifecycle()
                FlexFields(viewModel.flex, preview) { viewModel.flex = it }
            }
        }
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value,
        onChange,
        singleLine = true,
        shape = FieldShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = PlannerColors.FaintOutline),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.FixedFields(form: FixedForm, onChange: (FixedForm) -> Unit) {
    SectionLabel("이름")
    NameField(form.title, { onChange(form.copy(title = it)) })

    SectionLabel("요일", hint = "여러 개 선택", top = 26.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DAY_NAMES.forEachIndexed { i, name ->
            val day = i + 1
            val on = day in form.days
            val scheme = MaterialTheme.colorScheme
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(if (on) scheme.primary else Color.Transparent)
                    .border(1.dp, if (on) Color.Transparent else PlannerColors.FaintOutline, CircleShape)
                    .toggleable(on, role = Role.Checkbox) { onChange(form.copy(days = if (on) form.days - day else form.days + day)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name,
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) scheme.onPrimary else if (i >= 5) PlannerColors.Faint else scheme.onSurface,
                )
            }
        }
    }

    SectionLabel("시작 시각", top = 26.dp)
    TimeStepper(
        value = formatTime(form.startMin),
        onMinus = { onChange(form.copy(startMin = form.startMin - GRID_MIN)) },
        onPlus = { onChange(form.copy(startMin = form.startMin + GRID_MIN)) },
        minusEnabled = form.startMin > 0,
        plusEnabled = form.startMin < LAST_START,
        stepLabel = "시작 시각",
    )

    SectionLabel("소요시간", top = 26.dp)
    DurationPicker(form.durationMin, { onChange(form.copy(durationMin = it)) })

    if (form.days.isNotEmpty()) {
        val days = form.days.sorted().joinToString("·") { DAY_NAMES[it - 1] }
        Text(
            "$days ${formatRange(form.startMin, form.startMin + form.durationMin)}",
            style = MaterialTheme.typography.bodySmall,
            color = PlannerColors.Muted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun ColorGrid(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TaskColor.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { color ->
                    val on = color.ordinal == selected
                    val shape = RoundedCornerShape(8.dp)
                    Box(
                        Modifier.size(28.dp).clip(shape).background(color.bg)
                            .border(if (on) 2.dp else 1.dp, if (on) color.fg else Color.Black.copy(alpha = 0.06f), shape)
                            .selectable(on, role = Role.RadioButton) { onSelect(color.ordinal) }
                            .semantics { contentDescription = color.label },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) Icon(Icons.Rounded.Check, contentDescription = null, tint = color.fg, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.FlexFields(form: FlexForm, preview: List<PlannedSlot>?, onChange: (FlexForm) -> Unit) {

    // 이름과 색은 한 줄에 나란히 둔다. 색 8칸은 28dp 두 줄이면 끝난다.
    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            SectionLabel("이름", top = 0.dp, bottom = 6.dp)
            NameField(form.title, { onChange(form.copy(title = it)) })
        }
        Column {
            SectionLabel("색", top = 0.dp, bottom = 6.dp)
            ColorGrid(form.colorIndex) { onChange(form.copy(colorIndex = it)) }
        }
    }

    SectionLabel("소요시간", top = 18.dp, bottom = 6.dp)
    DurationPicker(form.durationMin, { onChange(form.copy(durationMin = it)) }, gap = 6.dp)

    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            SectionLabel("주당 횟수", top = 0.dp, bottom = 6.dp)
            Stepper(
                value = "${form.timesPerWeek}",
                unit = "회",
                onMinus = { onChange(form.copy(timesPerWeek = form.timesPerWeek - 1)) },
                onPlus = { onChange(form.copy(timesPerWeek = form.timesPerWeek + 1)) },
                minusEnabled = form.timesPerWeek > 1,
                plusEnabled = form.timesPerWeek < 7,
                stepLabel = "주당 횟수",
            )
        }
        Column(Modifier.weight(1f)) {
            SectionLabel("우선순위", top = 0.dp, bottom = 6.dp)
            Segmented(
                PRIORITY_LABELS.keys.toList(), form.priority, { onChange(form.copy(priority = it)) },
                label = { PRIORITY_LABELS.getValue(it) }, height = 56.dp, shape = FieldShape,
            )
        }
    }

    SectionLabel("선호 시간대", top = 18.dp, bottom = 6.dp)
    Segmented(Window.entries, form.window, { onChange(form.copy(window = it)) }, label = { it.label }, icon = { it.icon })

    SectionLabel("마감 요일", hint = "이 요일까지 다 하도록 앞쪽에 배치", top = 18.dp, bottom = 6.dp)
    ChoiceChips(
        options = listOf<Pair<Int?, String>>(null to "없음") + DAY_NAMES.mapIndexed { i, name -> (i + 1) to name },
        selected = form.deadlineDay,
        onSelect = { onChange(form.copy(deadlineDay = it)) },
        gap = 6.dp,
        // 여덟 칸이 한 줄(371dp)에 들어가야 한다.
        horizontalPadding = 10.dp,
    )

    if (preview != null) {
        val placed = preview.joinToString(" · ") { "${DAY_NAMES[it.dayOfWeek - 1]} ${formatTime(it.startMin)}" }
        val missing = form.timesPerWeek - preview.size
        PreviewLine(
            when {
                preview.isEmpty() -> "지금 조건으로는 이번 주에 넣을 빈칸이 없어요." to ""
                missing > 0 -> "${missing}회는 빈칸이 모자라요. 이번 주 예상: " to placed
                else -> "저장하면 이번 주 빈칸 ${preview.size}곳에 넣어요. 지금 예상: " to placed
            },
        )
    }
}

/** 저장하면 어떻게 되는지 미리 보여 준다. 잡힐 자리만 진하게. */
@Composable
private fun PreviewLine(text: Pair<String, String>) {
    Row(
        Modifier.padding(top = 20.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, FieldShape).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Text(
            buildAnnotatedString {
                append(text.first)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) { append(text.second) }
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp,
            color = PlannerColors.Body,
        )
    }
}
