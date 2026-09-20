package com.uj.planner.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uj.planner.data.PlannerRepository
import com.uj.planner.ui.edit.EditKind
import com.uj.planner.ui.edit.EditScreen
import com.uj.planner.ui.edit.EditViewModel
import com.uj.planner.ui.missed.MissedViewModel
import com.uj.planner.ui.settings.SettingsScreen
import com.uj.planner.ui.settings.SettingsViewModel
import com.uj.planner.ui.week.WeekScreen
import com.uj.planner.ui.week.WeekViewModel

private const val NO_ID = -1L

/** 목적지는 week / edit / settings 셋. ViewModel 은 DI 없이 여기서 repository 를 넘겨 만든다. */
@Composable
fun PlannerNavHost(
    repository: PlannerRepository,
    nav: NavHostController,
    version: String,
    dataSection: @Composable ColumnScope.(SettingsViewModel) -> Unit,
) {
    NavHost(nav, startDestination = "week") {
        composable("week") {
            WeekScreen(
                viewModel = viewModel { WeekViewModel(repository) },
                missedViewModel = viewModel { MissedViewModel(repository) },
                onEdit = { kind, id -> nav.navigate("edit/${kind.name}?id=${id ?: NO_ID}") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable(
            "edit/{kind}?id={id}",
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType; defaultValue = NO_ID },
            ),
        ) { entry ->
            val args = checkNotNull(entry.arguments)
            val kind = EditKind.valueOf(checkNotNull(args.getString("kind")))
            val id = args.getLong("id").takeIf { it != NO_ID }
            EditScreen(viewModel { EditViewModel(repository, kind, id) }, onClose = { nav.popBackStack() })
        }
        composable("settings") {
            val settings: SettingsViewModel = viewModel { SettingsViewModel(repository) }
            SettingsScreen(
                viewModel = settings,
                version = version,
                onBack = { nav.popBackStack() },
                dataSection = { dataSection(settings) },
            )
        }
    }
}
