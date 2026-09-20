package com.uj.planner.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.uj.planner.data.PlannerRepository
import com.uj.planner.ui.cover.CoverScreen
import com.uj.planner.ui.flex.FlexScreen
import com.uj.planner.ui.settings.SettingsViewModel
import com.uj.planner.ui.today.TodayViewModel

/**
 * 반 접어 세운 자세의 힌지 자리. 창 위에서 잰 값이다.
 *
 * 접혀 있지 않거나 힌지가 없는 기기면 null 이다. 이 값을 무엇으로 알아내는지는 플랫폼마다
 * 다르고(안드로이드는 androidx.window), 그 차이를 여기까지 들이지 않으려고 인자로 받는다.
 */
data class Fold(val top: Dp, val bottom: Dp)

/** 창의 가로·세로가 **모두** 이보다 작으면 한눈에 보는 화면으로 바꾼다. [PlannerRoot] 설명 참고. */
private val GLANCE_MAX = 480.dp

/**
 * 창 크기와 힌지에 따라 화면을 가른다. 기기 이름이 아니라 창의 성질로만 판단한다.
 *
 * | 조건 | 화면 | 어느 기기 |
 * |---|---|---|
 * | 가로·세로 모두 [GLANCE_MAX] 미만 | [CoverScreen] | 플립·레이저의 커버 |
 * | [fold] 가 있음 | [FlexScreen] | 세워 접은 플립, 탁자 자세의 북형 폴더블 |
 * | 그 밖 | [PlannerNavHost] | 일반 폰, 태블릿, 펼친 폴더블, 폴드류의 커버, iPhone·iPad |
 *
 * [GLANCE_MAX] 는 특정 기기의 치수가 아니라 "주간 그리드를 그릴 수 없을 만큼 작은 창" 이라는
 * 뜻이다. 가로·세로를 모두 보기 때문에 가로로 돌린 폰은 걸리지 않는다 — 높이가 낮아도 폭이 넓다.
 * 폴드류의 길쭉한 커버도 세로가 길어 걸리지 않고, 그게 맞는 동작이다.
 *
 * 내비게이션 컨트롤러는 갈림길 **위에서** 만든다. 접었다 펴도 보던 화면과 입력하던 폼이 남아 있어야 한다.
 *
 * @param version 설정 화면 맨 아래에 적을 앱 버전.
 * @param dataSection 설정 화면의 내보내기·가져오기 블록. 파일 선택기가 플랫폼 전용이다.
 */
@Composable
fun PlannerRoot(
    repository: PlannerRepository,
    fold: Fold?,
    version: String,
    dataSection: @Composable ColumnScope.(SettingsViewModel) -> Unit,
) {
    val nav = rememberNavController()
    val today: TodayViewModel = viewModel { TodayViewModel(repository) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isCover = maxWidth < GLANCE_MAX && maxHeight < GLANCE_MAX
        if (!isCover && fold == null) {
            PlannerNavHost(repository = repository, nav = nav, version = version, dataSection = dataSection)
            return@BoxWithConstraints
        }
        val state = today.state.collectAsStateWithLifecycle().value ?: return@BoxWithConstraints
        if (isCover) {
            CoverScreen(state, today::answer)
        } else {
            val hinge = checkNotNull(fold)
            FlexScreen(state, hinge.top, hinge.bottom, today::answer)
        }
    }
}
