package com.uj.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.planner.domain.GRID_MIN
import com.uj.planner.ui.formatDuration
import com.uj.planner.ui.icons.UjIcons
import com.uj.planner.ui.theme.PlannerColors

/** 입력칸·스테퍼·시각 칸이 같이 쓰는 모서리. */
val FieldShape = RoundedCornerShape(12.dp)

/** 시안의 칸 테두리: 1dp, 흐린 외곽선. */
fun Modifier.outlinedBox(shape: Shape = FieldShape): Modifier = clip(shape).border(1.dp, PlannerColors.FaintOutline, shape)

/** 한 줄이 이보다 길어지면 눈이 줄 끝에서 다음 줄 앞을 찾기 어려워진다. */
private val READABLE_WIDTH = 640.dp

/**
 * 폼의 폭을 읽기 좋은 만큼으로 묶고 가운데에 놓는다. 태블릿·가로·펼친 폴더블에서 입력칸이
 * 화면 끝까지 늘어나는 것을 막는다. 폭이 [READABLE_WIDTH] 이하인 화면에서는 `fillMaxWidth` 와 같다.
 *
 * 주간 그리드에는 쓰지 않는다. 7열은 넓을수록 좋다.
 */
fun Modifier.readableWidth(): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = READABLE_WIDTH).fillMaxWidth()

/** 닫기/뒤로 + 제목. 편집과 설정 화면의 머리. */
@Composable
fun ScreenHeader(title: String, navIcon: ImageVector, navLabel: String, onNav: () -> Unit) {
    Row(Modifier.statusBarsPadding().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNav) { Icon(navIcon, contentDescription = navLabel) }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
    }
}

/** 폼의 항목 제목. [hint] 는 옆에 흐리게 붙는 보충 설명. */
@Composable
fun SectionLabel(text: String, hint: String? = null, top: Dp = 20.dp, bottom: Dp = 8.dp) {
    Row(Modifier.padding(top = top, bottom = bottom), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hint != null) {
            Text("· $hint", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = PlannerColors.Faint)
        }
    }
}

@Composable
private fun StepCell(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit, caption: String? = null, width: Dp = 52.dp) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f)
    Column(
        // 이름은 여기 한 곳에서만 붙인다. 아이콘에도 같은 말을 달면 두 번 읽힌다.
        Modifier.width(width).fillMaxHeight().clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (caption == null) 22.dp else 20.dp))
        if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@Composable
private fun Divider() = Box(Modifier.width(1.dp).fillMaxHeight().background(PlannerColors.FaintOutline))

/** 한 칸 안의 − 값 + . 타이핑 없이 값을 바꾼다. [unit] 은 값 옆에 작게 붙는다. */
@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String = "",
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
    stepLabel: String = "",
) {
    Row(modifier.height(56.dp).outlinedBox(), verticalAlignment = Alignment.CenterVertically) {
        StepCell(UjIcons.Remove, "$stepLabel 줄이기", minusEnabled, onMinus)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
            if (unit.isNotEmpty()) {
                Text(unit, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = PlannerColors.Muted, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
            }
        }
        StepCell(UjIcons.Add, "$stepLabel 늘리기", plusEnabled, onPlus)
    }
}

/** 시각 칸 + 붙어 있는 −30분 / +30분. */
@Composable
fun TimeStepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit, minusEnabled: Boolean, plusEnabled: Boolean, stepLabel: String) {
    Row(Modifier.height(56.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight().outlinedBox(), contentAlignment = Alignment.Center) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.outlinedBox()) {
            StepCell(UjIcons.Remove, "$stepLabel 30분 앞으로", minusEnabled, onMinus, caption = "30분", width = 56.dp)
            Divider()
            StepCell(UjIcons.Add, "$stepLabel 30분 뒤로", plusEnabled, onPlus, caption = "30분", width = 56.dp)
        }
    }
}

/** 알약 모양 칩 하나. 고르면 채워지고 테두리가 사라진다. */
@Composable
fun PillChip(label: String, selected: Boolean, onClick: () -> Unit, horizontalPadding: Dp = 14.dp) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.height(40.dp).defaultMinSize(minWidth = 40.dp).clip(CircleShape)
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else PlannerColors.FaintOutline, CircleShape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            // 요일처럼 한 글자짜리는 40dp 원이 된다.
            .padding(horizontal = if (label.length == 1) 0.dp else horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant)
    }
}

/** 여러 개 중 하나를 고르는 칩 묶음. 칸이 모자라면 다음 줄로 넘어간다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, gap: Dp = 8.dp, horizontalPadding: Dp = 14.dp) {
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) -> PillChip(label, value == selected, { onSelect(value) }, horizontalPadding) }
    }
}

/**
 * 옆으로 붙은 칸 중 하나를 고른다. 모양은 쓰는 곳마다 다르다 —
 * 고정/가변 탭은 40dp 알약에 체크([checkSelected]), 우선순위는 56dp 둥근 사각, 선호 시간대는 48dp 알약에 아이콘.
 */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    shape: Shape = CircleShape,
    checkSelected: Boolean = false,
    icon: ((T) -> ImageVector)? = null,
) {
    Row(modifier.fillMaxWidth().height(height).outlinedBox(shape).selectableGroup()) {
        options.forEachIndexed { i, option ->
            if (i > 0) Divider()
            SegmentCell(label(option), option == selected, { onSelect(option) }, if (checkSelected) UjIcons.Check.takeIf { option == selected } else icon?.invoke(option), big = checkSelected)
        }
    }
}

@Composable
private fun RowScope.SegmentCell(label: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector?, big: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    Row(
        Modifier.weight(1f).fillMaxHeight()
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(end = if (big) 6.dp else 4.dp).size(if (big) 18.dp else 16.dp))
        Text(
            label,
            style = if (big) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private val DURATION_PRESETS = listOf(30, 60, 90, 120, 180)
private const val MAX_DURATION = 12 * 60

/** 소요시간. 자주 쓰는 값은 칩으로, 그 밖은 "직접" 을 골라 30분 단위로 맞춘다. */
@Composable
fun DurationPicker(durationMin: Int, onChange: (Int) -> Unit, gap: Dp = 8.dp) {
    // 값은 편집 화면이 뜬 뒤에 비동기로 들어온다. 그래서 "직접" 여부를 처음 값으로 굳히지 않고 매번 값에서 다시 본다.
    var customChosen by rememberSaveable { mutableStateOf(false) }
    val custom = customChosen || durationMin !in DURATION_PRESETS
    val options = DURATION_PRESETS.map { it to if (it == 90) "1.5시간" else formatDuration(it) } + (null to "직접")
    ChoiceChips<Int?>(
        options = options,
        selected = if (custom) null else durationMin,
        onSelect = {
            customChosen = it == null
            if (it != null) onChange(it)
        },
        gap = gap,
    )
    if (custom) {
        Stepper(
            value = formatDuration(durationMin),
            onMinus = { onChange(durationMin - GRID_MIN) },
            onPlus = { onChange(durationMin + GRID_MIN) },
            modifier = Modifier.padding(top = 8.dp).width(220.dp),
            minusEnabled = durationMin > GRID_MIN,
            plusEnabled = durationMin < MAX_DURATION,
            stepLabel = "소요시간",
        )
    }
}
