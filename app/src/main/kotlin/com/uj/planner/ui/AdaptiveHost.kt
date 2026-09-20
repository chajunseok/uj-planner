package com.uj.planner.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.uj.planner.data.Backup
import com.uj.planner.data.PlannerRepository

/**
 * 안드로이드의 진입점. 힌지를 읽어 [PlannerRoot] 에 넘기는 일만 한다.
 *
 * 화면을 가르는 판단 자체는 [PlannerRoot] 에 있다 — iOS 와 같은 규칙을 써야 하기 때문이다.
 * 여기 남는 것은 `androidx.window` 의존뿐이다.
 *
 * 가로 힌지만 본다. 세로 힌지(책처럼 편 북형 폴더블)는 화면이 가장 넓은 때라 주간 그리드를
 * 그대로 두는 편이 낫다 — [PlannerRoot] 의 표 참고.
 */
@Composable
fun AdaptiveHost(repository: PlannerRepository, backup: Backup) {
    val activity = checkNotNull(LocalActivity.current)
    val layoutInfo by remember(activity) { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
        .collectAsStateWithLifecycle(initialValue = null)

    val hinge = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull {
        it.state == FoldingFeature.State.HALF_OPENED && it.orientation == FoldingFeature.Orientation.HORIZONTAL
    }
    val fold = with(LocalDensity.current) { hinge?.let { Fold(it.bounds.top.toDp(), it.bounds.bottom.toDp()) } }

    PlannerRoot(
        repository = repository,
        fold = fold,
        version = appVersion(),
        dataSection = { settings -> AndroidDataSection(backup, settings) },
    )
}
