package com.example.caso1.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.caso1.ui.screen.*
import com.example.caso1.viewmodel.EstadoSesion
import com.example.caso1.viewmodel.RegistroAccionViewModel
import com.example.caso1.viewmodel.SessionViewModel

@Composable
fun AppNavigation(
    anchoVentana: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    navController: NavHostController = rememberNavController(),
    sesion: SessionViewModel = viewModel()
) {
    val estado by sesion.estado.collectAsStateWithLifecycle()

    // Mientras DataStore no responde no sabemos si hay sesión: así no aparece el login "de paso"
    if (estado is EstadoSesion.Cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val rolActual = (estado as? EstadoSesion.Activa)?.rol
    val destinoInicial = remember { if (rolActual == null) AppRoutes.LOGIN else AppRoutes.HOME }

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable(AppRoutes.LOGIN) {
            LoginScreen { rol ->
                sesion.iniciarSesion(rol)
                navController.navigate(AppRoutes.HOME) {
                    popUpTo(AppRoutes.LOGIN) { inclusive = true }
                }
            }
        }
        composable(AppRoutes.HOME) {
            HomeScreen(
                rol = rolActual,
                onCerrarSesion = {
                    sesion.cerrarSesion()
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.HOME) { inclusive = true }
                    }
                },
                onVerDetalle = { id -> navController.navigate(AppRoutes.detalle(id)) },
                onVerAlertas = { navController.navigate(AppRoutes.ALERTAS) },
                onRegistrar = { navController.navigate(AppRoutes.REGISTRAR) },
                onVerHistorial = { navController.navigate(AppRoutes.HISTORIAL) },
                anchoVentana = anchoVentana
            )
        }
        composable(AppRoutes.ALERTAS) {
            AlertasScreen(rol = rolActual, onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.HISTORIAL) {
            HistorialScreen(onBack = { navController.popBackStack() })
        }
        // Registro y confirmación comparten el ViewModel, ligado a la entrada HOME de la pila
        composable(AppRoutes.REGISTRAR) {
            val vm: RegistroAccionViewModel = viewModel(navController.getBackStackEntry(AppRoutes.HOME))
            RegistroAccionScreen(viewModel = vm) {
                navController.navigate(AppRoutes.CONFIRMAR)
            }
        }
        composable(AppRoutes.CONFIRMAR) {
            val vm: RegistroAccionViewModel = viewModel(navController.getBackStackEntry(AppRoutes.HOME))
            ConfirmacionScreen(viewModel = vm) {
                navController.popBackStack(AppRoutes.HOME, inclusive = false)
            }
        }
        // DetalleViewModel lee "galponId" desde su SavedStateHandle
        composable(
            AppRoutes.DETALLE,
            arguments = listOf(navArgument("galponId") { type = NavType.IntType })
        ) {
            DetalleGalponScreen(onBack = { navController.popBackStack() })
        }
    }
}
