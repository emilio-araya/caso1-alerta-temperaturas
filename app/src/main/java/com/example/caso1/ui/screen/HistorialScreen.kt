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
import androidx.compose.ui.res.stringResource
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
fun HistorialScreen(marco: Marco, viewModel: HistorialViewModel = viewModel(factory = HistorialViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val filtroGalpon = state.filtroGalpon

    PantallaPrincipal(marco = marco, actual = Destino.HISTORIAL, titulo = stringResource(R.string.historial_titulo)) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pestana, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 },
                    text = { Text(stringResource(R.string.historial_tab_alertas, state.alertas.size)) })
                Tab(selected = pestana == 1, onClick = { pestana = 1 },
                    text = { Text(stringResource(R.string.historial_tab_acciones, state.eventos.size)) })
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filtroGalpon == null, onClick = { viewModel.filtrarPorGalpon(null) },
                    label = { Text(stringResource(R.string.historial_todos)) })
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
                        EstadoVacio(R.drawable.ic_history, stringResource(R.string.historial_sin_alertas_titulo),
                            stringResource(R.string.historial_sin_alertas_texto))
                    }
                    items(state.alertas, key = { it.id }) { a ->
                        TarjetaAlertaHistorial(a, nombreGalpon(state.galponesPorId[a.galponId], a.galponId))
                    }
                } else {
                    if (state.eventos.isEmpty()) item {
                        EstadoVacio(R.drawable.ic_edit_note, stringResource(R.string.historial_sin_acciones_titulo),
                            stringResource(R.string.historial_sin_acciones_texto))
                    }
                    items(state.eventos, key = { it.id }) { e ->
                        TarjetaEvento(e, nombreGalpon(state.galponesPorId[e.galponId], e.galponId))
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
                Text(stringResource(R.string.meta_galpon_fecha, galpon, formatoFechaHora(alerta.fechaHora)), style = MaterialTheme.typography.bodySmall)
                Text(textoConfirmacion(alerta.confirmadaPor, alerta.confirmadaEn), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (alerta.activa) alerta.nivel.etiqueta() else stringResource(R.string.historial_resuelta),
                style = MaterialTheme.typography.labelLarge,
                color = if (alerta.activa) alerta.nivel.colores().texto else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaEvento(evento: EventoEntity, galpon: String) {
    val codigo = evento.accionRegistrada
    // Las filas antiguas guardaban el texto "Confirmación de alerta" en vez del código
    val esConfirmacion = codigo == ACCION_CONFIRMAR || codigo == "Confirmación de alerta"
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
                Text(accionLegible(codigo), style = MaterialTheme.typography.titleSmall)
                // Las filas antiguas llevaban el tipo como prefijo "[Ventilación] …"
                Text(evento.descripcion.removePrefix("[$codigo] "), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.meta_galpon_fecha, galpon, formatoFechaHora(evento.fechaHora)), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
