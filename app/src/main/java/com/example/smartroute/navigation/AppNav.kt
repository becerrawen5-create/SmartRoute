package com.example.smartroute.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.screens.*

object Dest {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val MAP = "map"
    const val DETAIL = "detail"
    const val FAVORITES = "favorites"
    const val CREATE = "create"
    const val HISTORY = "history"
    const val WEATHER = "weather"
    const val TRAFFIC = "traffic"
    const val PROFILE = "profile"
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun SmartRouteApp(vm: SmartRouteViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    val tabs = listOf(
        BottomTab(Dest.HOME, "Inicio", Icons.Filled.Home),
        BottomTab(Dest.FAVORITES, "Rutas", Icons.Filled.Directions),
        BottomTab(Dest.HISTORY, "Historial", Icons.Filled.History),
        BottomTab(Dest.PROFILE, "Perfil", Icons.Filled.Person)
    )

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (tabs.any { it.route == current }) {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Dest.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.SPLASH) {
                SplashScreen(onStart = { nav.navigate(Dest.LOGIN) })
            }
            composable(Dest.LOGIN) {
                LoginScreen(vm, onLoggedIn = {
                    nav.navigate(Dest.HOME) { popUpTo(Dest.SPLASH) { inclusive = true } }
                })
            }
            composable(Dest.HOME) {
                HomeScreen(
                    vm,
                    onOpenMap = { nav.navigate(Dest.MAP) },
                    onOpenWeather = { nav.navigate(Dest.WEATHER) },
                    onOpenTraffic = { nav.navigate(Dest.TRAFFIC) },
                    onOpenFavorites = { nav.navigate(Dest.FAVORITES) },
                    onOpenRoute = { id ->
                        vm.select(id)
                        nav.navigate(Dest.DETAIL)
                    }
                )
            }
            composable(Dest.MAP) {
                MapRoutesScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(Dest.DETAIL) {
                RouteDetailScreen(vm, onBack = { nav.popBackStack() }, onOpenMap = { nav.navigate(Dest.MAP) })
            }
            composable(Dest.FAVORITES) {
                FavoritesScreen(
                    vm,
                    onOpenRoute = { id ->
                        vm.select(id)
                        nav.navigate(Dest.DETAIL)
                    },
                    onAddRoute = { nav.navigate(Dest.CREATE) }
                )
            }
            composable(Dest.CREATE) {
                CreateRouteScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onCreated = {
                        nav.navigate(Dest.DETAIL) { popUpTo(Dest.CREATE) { inclusive = true } }
                    }
                )
            }
            composable(Dest.HISTORY) { HistoryScreen(vm) }
            composable(Dest.WEATHER) { WeatherScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Dest.TRAFFIC) { TrafficScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Dest.PROFILE) {
                ProfileScreen(vm, onLogout = {
                    nav.navigate(Dest.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
                })
            }
        }
    }
}
