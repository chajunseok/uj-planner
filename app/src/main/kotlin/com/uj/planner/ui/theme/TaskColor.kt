package com.uj.planner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 가변 일정의 색. 배경/글자 쌍이 모두 대비 4.5:1 이상이다.
 * DB 에는 순서(ordinal)가 저장되므로 **순서를 바꾸거나 중간에 끼워 넣지 않는다.** 추가는 끝에만.
 */
enum class TaskColor(val label: String, val bg: Color, val fg: Color) {
    GREEN("초록", Color(0xFFCDEBD1), Color(0xFF0D3F1B)),
    BLUE("파랑", Color(0xFFD3E4FA), Color(0xFF0B3560)),
    AMBER("호박", Color(0xFFF6E3B3), Color(0xFF4A3300)),
    PURPLE("보라", Color(0xFFE6DBF7), Color(0xFF37215F)),
    TEAL("청록", Color(0xFFC6ECE9), Color(0xFF003F3B)),
    ROSE("장미", Color(0xFFF9D9DC), Color(0xFF5A1420)),
    LIME("라임", Color(0xFFE4EDBA), Color(0xFF2F3F00)),
    TANGERINE("귤", Color(0xFFFBDCC6), Color(0xFF5C2600)),
    ;

    companion object {
        fun of(index: Int): TaskColor = entries[index.mod(entries.size)]
    }
}
