package com.example.caso1.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.caso1.data.SessionManager
import com.example.caso1.data.model.Rol
import com.example.caso1.ui.screen.HomeScreen
import com.example.caso1.ui.screen.LoginScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val rolActual by session.rol.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    val destinoInicial = if (rolActual == null) AppRoutes.LOGIN else AppRoutes.HOME

    NavHost(navController = navController, startDestination = AppRoutes.LOGIN) {
        composable(AppRoutes.LOGIN) {
            LoginScreen { rol ->
                scope.launch { session.guardarRol(rol) }
                navController.navigate(AppRoutes.HOME) {
                    popUpTo(AppRoutes.LOGIN) { inclusive = true }
                }
            }
        }
        composable(AppRoutes.HOME) {
            HomeScreen(
                onCerrarSesion = {
                    scope.launch { session.cerrarSesion() }
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.HOME) { inclusive = true }
                    }
                },
                onVerDetalle = { id -> navController.navigate(AppRoutes.detalle(id)) },
                onVerAlertas = { navController.navigate(AppRoutes.ALERTAS) }
            )
        }
        composable(AppRoutes.ALERTAS) {
            com.example.caso1.ui.screen.AlertasScreen(onBack = { navController.popBackStack() })
        }
        composable(
            AppRoutes.DETALLE,
            arguments = listOf(androidx.navigation.navArgument("galponId") { type = androidx.navigation.NavType.IntType })
        ) { backStack ->
            val id = backStack.arguments?.getInt("galponId") ?: 0
            com.example.caso1.ui.screen.DetalleGalponScreen(galponId = id, onBack = { navController.popBackStack() })
        }
    }

    // Si ya hay sesión guardada, saltar directo al Home
    LaunchedEffect(rolActual) {
        if (rolActual != null && navController.currentDestination?.route == AppRoutes.LOGIN) {
            navController.navigate(AppRoutes.HOME) { popUpTo(AppRoutes.LOGIN) { inclusive = true } }
        }
    }
}
