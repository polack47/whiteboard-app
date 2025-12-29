package com.whiteboard.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whiteboard.app.data.repository.DiagramRepository
import com.whiteboard.app.ui.editor.EditorScreen
import com.whiteboard.app.ui.editor.EditorViewModel
import com.whiteboard.app.ui.home.HomeScreen
import com.whiteboard.app.ui.home.HomeViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Editor : Screen("editor/{diagramId}") {
        fun createRoute(diagramId: String) = "editor/$diagramId"
    }
}

@Composable
fun AppNavigation(
    repository: DiagramRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            val viewModel = remember { HomeViewModel(repository) }
            val diagrams by viewModel.diagrams.collectAsState()

            HomeScreen(
                diagrams = diagrams,
                onDiagramClick = { diagram ->
                    navController.navigate(Screen.Editor.createRoute(diagram.id))
                },
                onNewDiagram = {
                    val newId = viewModel.createNewDiagram()
                    navController.navigate(Screen.Editor.createRoute(newId))
                },
                onDeleteDiagram = { diagram ->
                    viewModel.deleteDiagram(diagram)
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("diagramId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val diagramId = backStackEntry.arguments?.getString("diagramId")
            val viewModel = remember(diagramId) {
                EditorViewModel(repository, diagramId)
            }

            EditorScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
