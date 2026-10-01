package app.steptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.steptracker.ui.history.HistoryRoute
import app.steptracker.ui.history.HistoryViewModel
import app.steptracker.ui.theme.StepTrackerTheme
import app.steptracker.ui.today.TodayRoute
import app.steptracker.ui.today.TodayViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as StepApp).container
        setContent {
            StepTrackerTheme {
                StepTrackerNavHost(container)
            }
        }
    }
}

@Composable
private fun StepTrackerNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "today") {
        composable("today") {
            val viewModel: TodayViewModel = viewModel(
                factory = viewModelFactory { initializer { TodayViewModel(container.repository) } },
            )
            TodayRoute(viewModel = viewModel, onOpenHistory = { navController.navigate("history") })
        }
        composable("history") {
            val viewModel: HistoryViewModel = viewModel(
                factory = viewModelFactory { initializer { HistoryViewModel(container.repository) } },
            )
            HistoryRoute(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
