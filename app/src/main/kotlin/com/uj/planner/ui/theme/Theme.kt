package com.uj.planner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 값의 출처는 .claude/design/2609/260918.uj-planner.design-spec.md. 색을 바꿀 일이 생기면 이 패키지만 고친다.

private val Scheme = lightColorScheme(
    primary = Color(0xFF3F5E8F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF0F2A4E),
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF0F2A4E),
    tertiaryContainer = Color(0xFFF6E3B3),
    onTertiaryContainer = Color(0xFF4A3300),
    background = Color(0xFFFBF9F6),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFBF9F6),
    onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFE6E2DD),
    onSurfaceVariant = Color(0xFF484643),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F4F0),
    surfaceContainer = Color(0xFFF3F0EB),
    surfaceContainerHigh = Color(0xFFEDEAE4),
    surfaceContainerHighest = Color(0xFFE6E2DD),
    outline = Color(0xFF7A7770),
    outlineVariant = Color(0xFFCBC7C0),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    scrim = Color(0xFF1C1B19),
)

/** Material 역할에 없는 시안의 보조 색. */
object PlannerColors {
    /** 주말 요일, 버림 상태 글자. */
    val Faint = Color(0xFF8E8C87)

    /** 평일 요일 라벨. */
    val Muted = Color(0xFF6B6A66)

    /** 비선택 칩과 버림 블록의 테두리. */
    val FaintOutline = Color(0xFFBDB9B2)

    /** 주간 그리드의 오늘 열. */
    val TodayColumn = Color(0xFFF0EDE7)

    /** 카드 안 본문 글자. */
    val Body = Color(0xFF4A4946)

    /** 자정을 넘긴 종료 시각 옆의 "+1". */
    val NextDay = Color(0xFF7A5300)

    /** "끝!" 카드. */
    val DoneContainer = Color(0xFFE8F1E9)
    val OnDoneContainer = Color(0xFF0D3F1B)
}

/** 숫자 폭을 고정한다. 시각이 세로로 나란히 놓이는 곳에서 자리가 흔들리지 않게 한다. */
private const val TNUM = "tnum"

private fun style(size: Int, weight: FontWeight, lineHeight: Int = size + 6) =
    TextStyle(fontSize = size.sp, fontWeight = weight, lineHeight = lineHeight.sp, fontFeatureSettings = TNUM)

private val PlannerTypography = Typography(
    headlineMedium = style(34, FontWeight.SemiBold, 40),
    headlineSmall = style(30, FontWeight.SemiBold, 36),
    titleLarge = style(22, FontWeight.SemiBold),
    titleMedium = style(20, FontWeight.SemiBold),
    titleSmall = style(16, FontWeight.SemiBold),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 16),
    labelLarge = style(14, FontWeight.SemiBold, 20),
    labelMedium = style(13, FontWeight.SemiBold, 18),
    labelSmall = style(11, FontWeight.Bold, 13),
)

/** 블록 2행의 시작 시각. */
val BlockSubStyle = style(9, FontWeight.Medium, 10)

/** 시간축 숫자. */
val AxisStyle = style(10, FontWeight.Medium, 12)

/** 시안이 라이트 단일 테마다. 시스템이 다크여도 따라가지 않는다. */
@Composable
fun PlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = PlannerTypography, content = content)
}
