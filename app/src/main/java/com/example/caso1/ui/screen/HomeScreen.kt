package com.example.caso1.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.data.model.Umbrales
import com.example.caso1.ui.theme.EstiloCifra
import com.example.caso1.ui.theme.EstiloEstado
import com.example.caso1.viewmodel.Boletin
import com.example.caso1.viewmodel.DetalleViewModel
import com.example.caso1.viewmodel.HomeViewModel

/**
 * Pantalla principal. Lo que se muestra depende del perfil (punto 3.1 del caso):
 * - Operario: estado de galpones + registrar acciones.
 * - Supervisor: galpones ordenados por gravedad.
 * - Jefatura: resumen por estado y alertas del día.
 */
@Composable
fun HomeScreen(
    marco: Marco,
    onCerrarSesion: () -> Unit,
    onVerDetalle: (Int) -> Unit,
    onVerAlertas: () -> Unit,
    onRegistrar: (Int?) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rol = marco.rol
    var menuAbierto by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.mensajes.collect { snackbar.showSnackbar(it) }
    }

    // Supervisor y jefatura ven primero lo más grave; el operario, en orden de galpón (no se mueven)
    val listado = if (rol == Rol.OPERARIO) state.galpones
        else state.galpones.sortedByDescending { it.estado.ordinal }

    PantallaPrincipal(
        marco = marco,
        actual = Destino.GALPONES,
        titulo = "Alerta Temperaturas",
        snackbarHost = { SnackbarHost(snackbar) },
        acciones = {
            Box {
                IconButton(onClick = { menuAbierto = true }) { Icono(R.drawable.ic_more_vert, "Más opciones") }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Simular pico de calor (demo)") },
                        leadingIcon = { Icono(R.drawable.ic_bolt, null) },
                        onClick = { menuAbierto = false; viewModel.simularEventoCritico() }
                    )
                    DropdownMenuItem(
                        text = { Text("Cerrar sesión") },
                        leadingIcon = { Icono(R.drawable.ic_logout, null) },
                        onClick = { menuAbierto = false; onCerrarSesion() }
                    )
                }
            }
        },
        botonFlotante = {
            if (rol == Rol.OPERARIO) {
                ExtendedFloatingActionButton(
                    onClick = { onRegistrar(null) },
                    icon = { Icono(R.drawable.ic_edit_note, null) },
                    text = { Text("Registrar acción") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        // Adaptabilidad (Guía 9): compact = 1 columna, medium = 2 columnas, expanded = lista + detalle
        @Composable
        fun Listado(columnas: Int, seleccionado: Int?, modifier: Modifier, onClickGalpon: (Int) -> Unit) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnas),
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 0.dp,
                    bottom = if (rol == Rol.OPERARIO) 96.dp else 24.dp   // espacio para el botón flotante
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.boletin?.let { b ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "boletin") {
                        // Anula el margen lateral de la grilla: la franja va de borde a borde
                        BoletinEstado(b, onVerAlertas, Modifier.sangrado(16.dp))
                    }
                }
                if (rol == Rol.JEFATURA && !state.cargando) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "resumen") {
                        ResumenJefatura(state.galpones, state.alertasActivas, state.alertasHoy)
                    }
                }
                if (state.cargando) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "titulo") {
                        Text(
                            if (rol == Rol.OPERARIO) "${listado.size} galpones" else "${listado.size} galpones · más graves primero",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                items(listado, key = { it.id }) { g ->
                    TarjetaGalpon(g, state.ultimaPorGalpon[g.id], seleccionado = g.id == seleccionado) {
                        onClickGalpon(g.id)
                    }
                }
            }
        }

        when (marco.ancho) {
            WindowWidthSizeClass.Expanded -> {
                val detalleVm: DetalleViewModel = viewModel(key = "panel_detalle")
                val detalle by detalleVm.uiState.collectAsStateWithLifecycle()
                var seleccionado by rememberSaveable { mutableStateOf<Int?>(null) }
                val actual = seleccionado ?: listado.firstOrNull()?.id
                LaunchedEffect(actual) { actual?.let(detalleVm::seleccionar) }

                Row(Modifier.padding(padding).fillMaxSize()) {
                    Listado(1, actual, Modifier.weight(0.42f)) { seleccionado = it }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.weight(0.58f)) {
                        Text(
                            detalle.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                        )
                        DetalleContenido(
                            detalle,
                            onRegistrar = if (rol == Rol.OPERARIO) { { onRegistrar(detalle.galponId) } } else null
                        )
                    }
                }
            }
            WindowWidthSizeClass.Medium -> Listado(2, null, Modifier.padding(padding), onVerDetalle)
            else -> Listado(1, null, Modifier.padding(padding), onVerDetalle)
        }
    }
}

/**
 * Boletín: franja plana a todo el ancho con el color del peor galpón, qué hacer y una
 * sola próxima acción, como un aviso oficial. Es el único bloque de color pleno grande del inicio.
 */
@Composable
private fun BoletinEstado(b: Boletin, onVerAlertas: () -> Unit, modifier: Modifier = Modifier) {
    val c = b.estado.colores()
    val fondo by animateColorAsState(c.franja, tween(450), label = "boletin")
    val tinta by animateColorAsState(c.sobreFranja, tween(450), label = "boletinTinta")
    val normal = b.estado == EstadoGalpon.NORMAL

    Surface(color = fondo, contentColor = tinta, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icono(if (normal) R.drawable.ic_check_circle else R.drawable.ic_warning, null, Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(b.estado.etiqueta().uppercase(), style = EstiloEstado.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize))
            }
            Spacer(Modifier.height(6.dp))
            val g = b.masGrave
            val m = b.medicionMasGrave
            if (normal || g == null) {
                Text("Los ${b.total} galpones están en rango normal", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    "${g.granja} · ${g.nombre}" + (m?.let { " a ${cifra(it.temperatura)} °C" } ?: ""),
                    style = MaterialTheme.typography.titleMedium
                )
                val otros = b.afectados - 1
                if (otros > 0) {
                    Text(
                        if (otros == 1) "y 1 galpón más fuera de rango" else "y $otros galpones más fuera de rango",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(queHacer(b.estado), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    b.actualizado?.let { "Actualizado a las ${formatoHora(it)}" } ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                if (!normal) {
                    OutlinedButton(
                        onClick = onVerAlertas,
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.5.dp, tinta),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = tinta)
                    ) { Text("Ver alertas") }
                }
            }
        }
    }
}

/** La instrucción breve del aviso: qué se espera del operario ahora. */
private fun queHacer(estado: EstadoGalpon): String = when (estado) {
    EstadoGalpon.CRITICO -> "Revisa el galpón ahora y confirma la alerta."
    EstadoGalpon.ADVERTENCIA -> "Mantén el galpón en observación."
    EstadoGalpon.NORMAL -> ""
}

/** Ensancha el elemento [margen] por cada lado, para salir del relleno de la grilla. */
private fun Modifier.sangrado(margen: Dp) = layout { medible, restricciones ->
    val extra = margen.roundToPx() * 2
    val ancho = restricciones.maxWidth + extra
    val p = medible.measure(restricciones.copy(minWidth = ancho, maxWidth = ancho))
    layout(restricciones.maxWidth, p.height) { p.place(-extra / 2, 0) }
}

@Composable
private fun TarjetaGalpon(galpon: GalponEntity, ultima: MedicionEntity?, seleccionado: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = if (seleccionado) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        FranjaEstado(galpon.estado, galpon.estado.etiqueta(), extra = ultima?.let { formatoHora(it.fechaHora) })
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(galpon.nombre, style = MaterialTheme.typography.titleMedium)
                Text(galpon.granja, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ultima?.let {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icono(R.drawable.ic_water_drop, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("Humedad ${cifra(it.humedad, 0)} %", style = EstiloCifra,
                            color = colorCifra(estadoHumedad(it.humedad), MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                }
            }
            ultima?.let {
                Text(
                    "${cifra(it.temperatura)}°",
                    style = MaterialTheme.typography.displaySmall,
                    color = colorCifra(estadoTemperatura(it.temperatura))
                )
            }
        }
        ultima?.let {
            EscalaTermica(it.temperatura, Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp))
        } ?: Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ResumenJefatura(galpones: List<GalponEntity>, alertasActivas: Int, alertasHoy: Int) {
    val conteo = galpones.groupingBy { it.estado }.eachCount()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Resumen", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                EstadoGalpon.entries.forEach { e ->
                    Column(Modifier.weight(1f)) {
                        Text("${conteo[e] ?: 0}", style = MaterialTheme.typography.displaySmall, color = e.colores().texto)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MarcaEstado(e)
                            Spacer(Modifier.width(6.dp))
                            Text(e.etiquetaCorta(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text("$alertasActivas alertas activas en ${galpones.size} galpones", style = MaterialTheme.typography.bodyMedium)
            Text("$alertasHoy alertas registradas hoy", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

