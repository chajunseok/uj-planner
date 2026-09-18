package com.uj.planner

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.uj.planner.ui.PlannerNavHost
import com.uj.planner.ui.theme.PlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 라이트 단일 테마라 시스템이 다크여도 상태바 아이콘은 어두운 색이어야 한다.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        val repository = (application as PlannerApp).repository
        setContent {
            PlannerTheme { PlannerNavHost(repository) }
        }
    }
}
