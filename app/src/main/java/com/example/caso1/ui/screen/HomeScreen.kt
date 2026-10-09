package com.example.caso1.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.viewmodel.DetalleViewModel
import com.example.caso1.viewmodel.HomeViewModel

/**
 * Pantalla principal. Lo que se muestra depende del perfil (punto 3.1 del caso):
 * - Operario: estado de galpones + registrar/confirmar acciones.
 * - Supervisor: galpones ordenados por gravedad + acceso a alertas activas.
 * - Jefatura: resumen por estado y alertas del día.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    rol: Rol?,
    onCerrarSesion: () -> Unit,
    onVerDetalle: (Int) -> Unit = {},
    onVerAlertas: () -> Unit = {},
    onRegistrar: () -> Unit = {},
    onVerHistorial: () -> Unit = {},
    anchoVentana: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuAbierto by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.mensajes.collect { snackbar.showSnackbar(it) }
    }

    // Supervisor y jefatura ven primero lo más grave; el operario, en orden de galpón
    val listado = if (rol == Rol.OPERARIO) state.galpones
        else state.galpones.sortedByDescending { it.estado.ordinal }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Galpones")
                        rol?.let { Text("Perfil: ${it.etiqueta()}", style = MaterialTheme.typography.labelMedium) }
                    }
                },
                actions = {
                    TextButton(onClick = onVerAlertas) { Text("Alertas (${state.alertasActivas})") }
                    Box {
                        IconButton(onClick = { menuAbierto = true }) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
                        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                            DropdownMenuItem(
                                text = { Text("Simular evento crítico (demo)") },
                                onClick = {
                                    menuAbierto = false
                                    viewModel.simularEventoCritico()
                                }
                            )
                            DropdownMenuItem(text = { Text("Cerrar sesión") }, onClick = {
                                menuAbierto = false
                                onCerrarSesion()
                            })
                        }
                    }
                }
            )
        },
        bottomBar = {
            val modBoton = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
            when (rol) {
                Rol.OPERARIO -> Button(onClick = onRegistrar, modifier = modBoton) { Text("Registrar acción") }
                Rol.SUPERVISOR, Rol.JEFATURA ->
                    OutlinedButton(onClick = onVerHistorial, modifier = modBoton) { Text("Ver historial") }
                null -> {}
            }
        }
    ) { padding ->
        // Adaptabilidad (Guía 9): compact = 1 columna, medium = 2 columnas,
        // expanded = lista + detalle lado a lado
        @Composable
        fun Listado(columnas: Int, seleccionado: Int?, modifier: Modifier, onClickGalpon: (Int) -> Unit) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnas),
                modifier = modifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (rol == Rol.JEFATURA && !state.cargando) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ResumenJefatura(state.galpones, state.alertasActivas, state.alertasHoy)
                    }
                }
                if (state.cargando) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Cargando galpones…") }
                }
                items(listado, key = { it.id }) { g ->
                    TarjetaGalpon(g, state.ultimaPorGalpon[g.id], seleccionado = g.id == seleccionado) {
                        onClickGalpon(g.id)
                    }
                }
            }
        }

        when (anchoVentana) {
            WindowWidthSizeClass.Expanded -> {
                val detalleVm: DetalleViewModel = viewModel(key = "panel_detalle")
                val detalle by detalleVm.uiState.collectAsStateWithLifecycle()
                var seleccionado by rememberSaveable { mutableStateOf<Int?>(null) }
                val actual = seleccionado ?: listado.firstOrNull()?.id
                LaunchedEffect(actual) { actual?.let(detalleVm::seleccionar) }

                Row(Modifier.padding(padding).fillMaxSize()) {
                    Listado(1, actual, Modifier.weight(0.4f)) { seleccionado = it }
                    VerticalDivider()
                    Column(Modifier.weight(0.6f)) {
                        Text(
                            state.galpones.firstOrNull { it.id == actual }?.let { "${it.granja} · ${it.nombre}" } ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                        )
                        DetalleContenido(detalle)
                    }
                }
            }
            WindowWidthSizeClass.Medium -> Listado(2, null, Modifier.padding(padding), onVerDetalle)
            else -> Listado(1, null, Modifier.padding(padding), onVerDetalle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaGalpon(galpon: GalponEntity, ultima: MedicionEntity?, seleccionado: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (seleccionado) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            else CardDefaults.cardColors()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(galpon.estado.color(), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${galpon.granja} · ${galpon.nombre}", style = MaterialTheme.typography.titleMedium)
                ultima?.let {
                    Text("${"%.1f".format(it.temperatura)} °C · HR ${"%.0f".format(it.humedad)} %")
                }
            }
            Text(
                galpon.estado.etiqueta(),
                color = galpon.estado.color(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResumenJefatura(galpones: List<GalponEntity>, alertasActivas: Int, alertasHoy: Int) {
    val conteo = galpones.groupingBy { it.estado }.eachCount()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Resumen", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EstadoGalpon.entries.forEach { e ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${conteo[e] ?: 0}", style = MaterialTheme.typography.headlineMedium, color = e.color())
                        Text(e.etiqueta(), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("$alertasActivas alertas activas de ${galpones.size} galpones")
            Text("$alertasHoy alertas registradas hoy", style = MaterialTheme.typography.bodySmall)
        }
    }
}
