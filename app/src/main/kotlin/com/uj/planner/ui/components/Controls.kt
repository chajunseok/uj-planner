package com.uj.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uj.planner.domain.GRID_MIN
import com.uj.planner.ui.formatDuration

/** 폼의 항목 제목. [hint] 는 옆에 흐리게 붙는 보충 설명. */
@Composable
fun SectionLabel(text: String, hint: String? = null) {
    Row(Modifier.padding(top = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (hint != null) {
            Text("· $hint", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** − 값 + . 타이핑 없이 값을 바꾼다. */
@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
    stepLabel: String = "",
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalIconButton(onClick = onMinus, enabled = minusEnabled, modifier = Modifier.size(52.dp, 48.dp)) {
            Icon(Icons.Rounded.Remove, contentDescription = "$stepLabel 줄이기")
        }
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 88.dp),
        )
        FilledTonalIconButton(onClick = onPlus, enabled = plusEnabled, modifier = Modifier.size(52.dp, 48.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = "$stepLabel 늘리기")
        }
    }
}

/** 여러 개 중 하나를 고르는 칩 묶음. 칸이 모자라면 다음 줄로 넘어간다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                modifier = Modifier.height(40.dp),
            )
        }
    }
}

/** 옆으로 붙은 칸 중 하나를 고른다. 우선순위와 선호 시간대에 쓴다. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, onSelect: (T) -> Unit, label: (T) -> String, icon: ((T) -> ImageVector)? = null) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                // 기본 체크 표시는 칸을 넓혀 글자를 밀어낸다. 선택은 배경색으로 충분히 보인다.
                icon = {},
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (icon != null) Icon(icon(option), contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(label(option), maxLines = 1)
                    }
                },
                modifier = Modifier.height(48.dp),
            )
        }
    }
}

private val DURATION_PRESETS = listOf(30, 60, 90, 120, 180)
private const val MAX_DURATION = 12 * 60

/** 소요시간. 자주 쓰는 값은 칩으로, 그 밖은 "직접" 을 골라 30분 단위로 맞춘다. */
@Composable
fun DurationPicker(durationMin: Int, onChange: (Int) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(durationMin !in DURATION_PRESETS) }
    val options = DURATION_PRESETS.map { it to if (it == 90) "1.5시간" else formatDuration(it) } + (null to "직접")
    ChoiceChips<Int?>(
        options = options,
        selected = if (custom) null else durationMin,
        onSelect = {
            custom = it == null
            if (it != null) onChange(it)
        },
    )
    if (custom) {
        Row(Modifier.padding(top = 8.dp)) {
            Stepper(
                value = formatDuration(durationMin),
                onMinus = { onChange(durationMin - GRID_MIN) },
                onPlus = { onChange(durationMin + GRID_MIN) },
                minusEnabled = durationMin > GRID_MIN,
                plusEnabled = durationMin < MAX_DURATION,
                stepLabel = "소요시간",
            )
        }
    }
}
