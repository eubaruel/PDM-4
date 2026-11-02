package com.example.projetopdm4.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projetopdm4.presentation.viewmodel.FinanceViewModel
import com.example.projetopdm4.presentation.viewmodel.FinanceViewModelFactory

internal const val INCOME = "INCOME"

internal const val EXPENSE = "EXPENSE"

internal data class AppDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

internal val destinations = listOf(
    AppDestination("home", "Resumo", Icons.Outlined.Home),
    AppDestination("transactions", "Movimentos", Icons.Outlined.SwapHoriz),
    AppDestination("planning", "Planejamento", Icons.AutoMirrored.Outlined.EventNote),
    AppDestination("reports", "Relatórios", Icons.Outlined.BarChart),
    AppDestination("settings", "Configurações", Icons.Outlined.Settings)
)

@Composable
fun FinanceApp(viewModel: FinanceViewModel = viewModel(factory = FinanceViewModelFactory(LocalContext.current))) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "home"
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = if (state.isAuthenticated) {
            {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = null
                    )
                }
            }
            }
        } else {
            {}
        }
    ) { padding ->
        if (state.isAuthenticated) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()
                ) {
                    composable("home") { HomeScreen(state, viewModel) }
                    composable("transactions") { TransactionsScreen(state, viewModel) }
                    composable("planning") { PlanningScreen(state, viewModel) }
                    composable("reports") { ReportsScreen(state) }
                    composable("settings") { SettingsScreen(state, viewModel) }
                }
            }
        } else {
            LoginScreen(
                modifier = Modifier.padding(padding),
                state = state,
                viewModel = viewModel
            )
        }
    }
}
