package com.uj.planner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 값의 출처는 .claude/design/2609/260918.uj-planner.design-spec.md. 색을 바꿀 일이 생기면 이 패키지만 고친다.

private val Scheme = lightColorScheme(
    primary = Color(0xFFB45A68),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF6D5D8),
    onPrimaryContainer = Color(0xFF5A1F2A),
    secondary = Color(0xFF7E9A79),
    secondaryContainer = Color(0xFFF6D5D8),
    onSecondaryContainer = Color(0xFF5A1F2A),
    tertiaryContainer = Color(0xFFF7E7C3),
    onTertiaryContainer = Color(0xFF5A4100),
    background = Color(0xFFFBF7F2),
    onBackground = Color(0xFF2A2523),
    surface = Color(0xFFFBF7F2),
    onSurface = Color(0xFF2A2523),
    surfaceVariant = Color(0xFFEAE2D8),
    onSurfaceVariant = Color(0xFF5E5652),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F4F0),
    surfaceContainer = Color(0xFFF4EEE6),
    surfaceContainerHigh = Color(0xFFEDEAE4),
    surfaceContainerHighest = Color(0xFFEAE2D8),
    outline = Color(0xFF8A817B),
    outlineVariant = Color(0xFFD6CCC2),
    // 붉은색은 하나만 쓴다. 현재 시각선·오류·주 색이 모두 로즈다.
    error = Color(0xFFB45A68),
    errorContainer = Color(0xFFF6D5D8),
    onErrorContainer = Color(0xFF5A1F2A),
    scrim = Color(0xFF2A2523),
)

/** Material 역할에 없는 시안의 보조 색. */
object PlannerColors {
    /** 주말 요일, 버림 상태 글자. */
    val Faint = Color(0xFF9A8F88)

    /** 평일 요일 라벨. */
    val Muted = Color(0xFF6E645E)

    /** 비선택 칩과 버림 블록의 테두리. */
    val FaintOutline = Color(0xFFD6CCC2)

    /** 주간 그리드의 오늘 열. */
    val TodayColumn = Color(0xFFF4EBE6)

    /** 카드 안 본문 글자. */
    val Body = Color(0xFF5E5652)

    /** 자정을 넘긴 종료 시각 옆의 "+1". */
    val NextDay = Color(0xFF5A4100)

    /** "잘 됐다" 의 색(세이지). "끝!" 카드, 재배치 확인 줄, 빈 상태 아이콘. */
    val DoneContainer = Color(0xFFD5E3D3)
    val OnDoneContainer = Color(0xFF2E4A33)
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

/** 주 이름과 커버·플렉스의 큰 제목에만 쓰는 세리프. 아이콘의 워드마크와 잇는다. 본문과 블록은 산세리프 그대로다. */
// ponytail: 기기에 든 세리프(Noto Serif CJK)를 쓴다. 실기기에서 모양이 어긋나면 Noto Serif KR 500 을 res/font 에 싣는다.
fun TextStyle.serif(): TextStyle = copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)

/** 블록 2행의 시작 시각. */
val BlockSubStyle = style(9, FontWeight.Medium, 10)

/** 시간축 숫자. */
val AxisStyle = style(10, FontWeight.Medium, 12)

/** 시안이 라이트 단일 테마다. 시스템이 다크여도 따라가지 않는다. */
@Composable
fun PlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = PlannerTypography, content = content)
}
