package com.uj.planner.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.uj.planner.data.PlannerRepository
import com.uj.planner.ui.cover.CoverScreen
import com.uj.planner.ui.flex.FlexScreen
import com.uj.planner.ui.today.TodayViewModel

/** 창의 가로·세로가 모두 이보다 작으면 커버 화면으로 본다. 플립7 커버는 약 399×361dp 다. */
private val COVER_MAX = 480.dp

/**
 * 기기 자세에 따라 화면을 가른다.
 * - 커버 화면 크기 → [CoverScreen]
 * - 반 접힘 + 가로 힌지 → [FlexScreen]
 * - 그 밖 → 내비게이션 호스트
 *
 * 내비게이션 컨트롤러는 갈림길 위에서 만든다. 접었다 펴도 보던 화면과 입력하던 폼이 남아 있어야 하기 때문이다.
 */
@Composable
fun AdaptiveHost(repository: PlannerRepository) {
    val activity = checkNotNull(LocalActivity.current)
    val layoutInfo by remember(activity) { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
        .collectAsStateWithLifecycle(initialValue = null)
    val nav = rememberNavController()
    val today: TodayViewModel = viewModel { TodayViewModel(repository) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isCover = maxWidth < COVER_MAX && maxHeight < COVER_MAX
        val hinge = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull {
            it.state == FoldingFeature.State.HALF_OPENED && it.orientation == FoldingFeature.Orientation.HORIZONTAL
        }
        if (!isCover && hinge == null) {
            PlannerNavHost(repository, nav)
            return@BoxWithConstraints
        }
        val state = today.state.collectAsStateWithLifecycle().value ?: return@BoxWithConstraints
        if (isCover) {
            CoverScreen(state, today::answer)
        } else {
            val bounds = checkNotNull(hinge).bounds
            with(LocalDensity.current) { FlexScreen(state, bounds.top.toDp(), bounds.bottom.toDp(), today::answer) }
        }
    }
}
