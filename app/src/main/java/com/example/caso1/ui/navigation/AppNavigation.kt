package com.example.caso1.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.caso1.data.model.Rol
import com.example.caso1.ui.screen.*
import com.example.caso1.viewmodel.EstadoSesion
import com.example.caso1.viewmodel.NavegacionViewModel
import com.example.caso1.viewmodel.RegistroAccionViewModel
import com.example.caso1.viewmodel.SessionViewModel

@Composable
fun AppNavigation(
    anchoVentana: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    abrirAlertas: Boolean = false,
    onAlertasAbiertas: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    sesion: SessionViewModel = viewModel(),
    navegacion: NavegacionViewModel = viewModel()
) {
    val estado by sesion.estado.collectAsStateWithLifecycle()
    val pendientes by navegacion.alertasPendientes.collectAsStateWithLifecycle()

    // Mientras DataStore no responde no sabemos si hay sesión: así no aparece el login "de paso"
    if (estado is EstadoSesion.Cargando) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val rolActual = (estado as? EstadoSesion.Activa)?.rol
    val destinoInicial = remember { if (rolActual == null) AppRoutes.LOGIN else AppRoutes.HOME }

    // Pestañas principales: Galpones es la base; Alertas e Historial se apilan sobre ella
    fun irA(destino: Destino) {
        if (destino == Destino.GALPONES) navController.popBackStack(AppRoutes.HOME, inclusive = false)
        else navController.navigate(destino.ruta) {
            popUpTo(AppRoutes.HOME)
            launchSingleTop = true
        }
    }

    val marco = Marco(rol = rolActual, ancho = anchoVentana, alertasPendientes = pendientes, onNavegar = ::irA)

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
                marco = marco,
                onCerrarSesion = {
                    sesion.cerrarSesion()
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.HOME) { inclusive = true }
                    }
                },
                onVerDetalle = { id -> navController.navigate(AppRoutes.detalle(id)) },
                onVerAlertas = { irA(Destino.ALERTAS) },
                onRegistrar = { id -> navController.navigate(AppRoutes.registrar(id)) }
            )
        }
        composable(AppRoutes.ALERTAS) { AlertasScreen(marco) }
        composable(AppRoutes.HISTORIAL) { HistorialScreen(marco) }

        // Registro y confirmación comparten el ViewModel, ligado a la entrada HOME de la pila
        composable(
            AppRoutes.REGISTRAR,
            arguments = listOf(navArgument("galponId") { type = NavType.IntType; defaultValue = -1 })
        ) { entrada ->
            val vm: RegistroAccionViewModel = viewModel(navController.getBackStackEntry(AppRoutes.HOME))
            val preseleccion = entrada.arguments?.getInt("galponId") ?: -1
            LaunchedEffect(preseleccion) { if (preseleccion > 0) vm.preseleccionar(preseleccion) }
            RegistroAccionScreen(viewModel = vm, onBack = { navController.popBackStack() }) {
                // Se saca el formulario de la pila: "atrás" desde la confirmación vuelve a Galpones
                navController.navigate(AppRoutes.CONFIRMAR) {
                    popUpTo(AppRoutes.REGISTRAR) { inclusive = true }
                }
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
            DetalleGalponScreen(
                onBack = { navController.popBackStack() },
                onRegistrar = if (rolActual == Rol.OPERARIO) { id -> navController.navigate(AppRoutes.registrar(id)) } else null
            )
        }
    }

    // Se tocó una notificación de alerta crítica: ir directo a Alertas (si hay sesión)
    LaunchedEffect(abrirAlertas, rolActual) {
        if (abrirAlertas && rolActual != null) {
            irA(Destino.ALERTAS)
            onAlertasAbiertas()
        }
    }
}
