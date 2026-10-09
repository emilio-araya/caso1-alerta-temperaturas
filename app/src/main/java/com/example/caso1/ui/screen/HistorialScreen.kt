package com.example.caso1.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.EventoEntity
import com.example.caso1.data.repository.ACCION_CONFIRMAR
import com.example.caso1.viewmodel.HistorialViewModel

/**
 * Historial para supervisor y jefatura (F5): alertas pasadas (activas y resueltas)
 * y acciones registradas por los operarios, con filtro por galpón.
 */
@Composable
fun HistorialScreen(marco: Marco, viewModel: HistorialViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val nombres = state.nombres
    val filtroGalpon = state.filtroGalpon

    PantallaPrincipal(marco = marco, actual = Destino.HISTORIAL, titulo = "Historial") { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pestana, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 },
                    text = { Text("Alertas (${state.alertas.size})") })
                Tab(selected = pestana == 1, onClick = { pestana = 1 },
                    text = { Text("Acciones (${state.eventos.size})") })
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filtroGalpon == null, onClick = { viewModel.filtrarPorGalpon(null) },
                    label = { Text("Todos") })
                state.galpones.forEach { g ->
                    FilterChip(
                        selected = filtroGalpon == g.id,
                        onClick = { viewModel.filtrarPorGalpon(g.id) },
                        label = { Text(g.nombre) },
                        leadingIcon = { MarcaEstado(g.estado) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pestana == 0) {
                    if (state.alertas.isEmpty()) item {
                        EstadoVacio(R.drawable.ic_history, "Sin alertas registradas",
                            "Aquí quedan todas las alertas, activas y resueltas.")
                    }
                    items(state.alertas, key = { it.id }) { a ->
                        TarjetaAlertaHistorial(a, nombres[a.galponId] ?: "Galpón ${a.galponId}")
                    }
                } else {
                    if (state.eventos.isEmpty()) item {
                        EstadoVacio(R.drawable.ic_edit_note, "Aún no hay acciones",
                            "Las acciones que registren los operarios y las alertas confirmadas aparecerán aquí.")
                    }
                    items(state.eventos, key = { it.id }) { e ->
                        TarjetaEvento(e, nombres[e.galponId] ?: "Galpón ${e.galponId}")
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAlertaHistorial(alerta: AlertaEntity, galpon: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MarcaEstado(alerta.nivel.estado)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tipoAlertaLegible(alerta.tipo), style = MaterialTheme.typography.titleSmall)
                Text("$galpon · ${formatoFechaHora(alerta.fechaHora)}", style = MaterialTheme.typography.bodySmall)
                Text(estadoConfirmacion(alerta), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (alerta.activa) alerta.nivel.etiqueta() else "Resuelta",
                style = MaterialTheme.typography.labelLarge,
                color = if (alerta.activa) alerta.nivel.colores().texto else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaEvento(evento: EventoEntity, galpon: String) {
    val accion = evento.accionRegistrada ?: "Evento"
    val esConfirmacion = accion == ACCION_CONFIRMAR
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icono(
                    if (esConfirmacion) R.drawable.ic_check_circle else R.drawable.ic_edit_note, null,
                    Modifier.size(20.dp), MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(accion, style = MaterialTheme.typography.titleSmall)
                Text(evento.descripcion.removePrefix("[$accion] "), style = MaterialTheme.typography.bodyMedium)
                Text("$galpon · ${formatoFechaHora(evento.fechaHora)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
