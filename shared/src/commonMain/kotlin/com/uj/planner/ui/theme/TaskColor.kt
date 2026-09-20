package com.uj.planner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 가변 일정의 색. 배경/글자 쌍이 모두 대비 4.5:1 이상이다.
 * DB 에는 순서(ordinal)가 저장되므로 **순서를 바꾸거나 중간에 끼워 넣지 않는다.** 추가는 끝에만.
 */
enum class TaskColor(val label: String, val bg: Color, val fg: Color) {
    SAGE("세이지", Color(0xFFD5E3D3), Color(0xFF2E4A33)),
    SKY("스카이", Color(0xFFD8E6F5), Color(0xFF1F3A5F)),
    BUTTER("버터", Color(0xFFF7E7C3), Color(0xFF5A4100)),
    LAVENDER("라벤더", Color(0xFFE4DCF2), Color(0xFF3E2B66)),
    MINT("민트", Color(0xFFD2ECE6), Color(0xFF0F4A40)),
    ROSE("로즈", Color(0xFFF6D5D8), Color(0xFF6B2430)),
    MAUVE("모브", Color(0xFFEBD8E6), Color(0xFF5A2A50)),
    PEACH("피치", Color(0xFFFBE0CF), Color(0xFF5E2C11)),
    ;

    companion object {
        fun of(index: Int): TaskColor = entries[index.mod(entries.size)]
    }
}
