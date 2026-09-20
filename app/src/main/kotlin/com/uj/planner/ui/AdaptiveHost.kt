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
import com.uj.planner.data.Backup
import com.uj.planner.data.PlannerRepository
import com.uj.planner.ui.cover.CoverScreen
import com.uj.planner.ui.flex.FlexScreen
import com.uj.planner.ui.today.TodayViewModel

/**
 * 창의 가로·세로가 **모두** 이보다 작으면 한눈에 보는 화면으로 바꾼다.
 *
 * 특정 기기의 치수가 아니라 "주간 그리드를 그릴 수 없을 만큼 작은 창" 이라는 뜻이다. 양쪽을 모두 보기
 * 때문에 가로로 돌린 폰은 걸리지 않는다 — 가로는 높이가 낮아도 폭이 넓다. 실제로 걸리는 것은
 * 플립·레이저처럼 정사각형에 가까운 커버(400dp 안팎)뿐이고, 폴드류의 길쭉한 커버는 폭이 좁아도
 * 세로가 길어 주간 그리드가 제대로 나오므로 걸리지 않는다. 그게 맞는 동작이다.
 */
private val GLANCE_MAX = 480.dp

/**
 * 창 크기와 힌지에 따라 화면을 가른다. 기기 이름이 아니라 창의 성질로만 판단한다.
 *
 * | 조건 | 화면 | 어느 기기 |
 * |---|---|---|
 * | 가로·세로 모두 [GLANCE_MAX] 미만 | [CoverScreen] | 플립·레이저의 커버 |
 * | 반 접힘 + 가로 힌지 | [FlexScreen] | 세워 접은 플립, 탁자 자세의 북형 폴더블 |
 * | 그 밖 | 내비게이션 호스트 | 일반 폰, 태블릿, 펼친 폴더블, 폴드류의 커버 |
 *
 * 세로 힌지(책처럼 편 북형 폴더블)는 일부러 걸러내지 않는다. 그 자세는 화면이 넓어 주간 그리드가
 * 가장 잘 보이는 때라, 한눈에 보는 카드로 바꾸면 오히려 쓰기 나빠진다.
 *
 * 내비게이션 컨트롤러는 갈림길 위에서 만든다. 접었다 펴도 보던 화면과 입력하던 폼이 남아 있어야 하기 때문이다.
 */
@Composable
fun AdaptiveHost(repository: PlannerRepository, backup: Backup) {
    val activity = checkNotNull(LocalActivity.current)
    val layoutInfo by remember(activity) { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
        .collectAsStateWithLifecycle(initialValue = null)
    val nav = rememberNavController()
    val today: TodayViewModel = viewModel { TodayViewModel(repository) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isCover = maxWidth < GLANCE_MAX && maxHeight < GLANCE_MAX
        val hinge = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull {
            it.state == FoldingFeature.State.HALF_OPENED && it.orientation == FoldingFeature.Orientation.HORIZONTAL
        }
        if (!isCover && hinge == null) {
            PlannerNavHost(repository, backup, nav)
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
