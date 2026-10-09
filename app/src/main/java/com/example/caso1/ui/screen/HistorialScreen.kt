package com.example.caso1.ui.screen

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
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.EventoEntity
import com.example.caso1.viewmodel.HistorialViewModel

/**
 * Historial para supervisor y jefatura (F5): alertas pasadas (activas y resueltas)
 * y acciones registradas por los operarios, con filtro por galpón.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onBack: () -> Unit, viewModel: HistorialViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    val nombres = state.nombres
    val filtroGalpon = state.filtroGalpon
    val alertasFiltradas = state.alertas
    val eventosFiltrados = state.eventos

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Historial") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("← Volver") }
            })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pestana) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 },
                    text = { Text("Alertas (${alertasFiltradas.size})") })
                Tab(selected = pestana == 1, onClick = { pestana = 1 },
                    text = { Text("Acciones (${eventosFiltrados.size})") })
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filtroGalpon == null, onClick = { viewModel.filtrarPorGalpon(null) },
                    label = { Text("Todos") })
                state.galpones.forEach { g ->
                    FilterChip(selected = filtroGalpon == g.id, onClick = { viewModel.filtrarPorGalpon(g.id) },
                        label = { Text(g.nombre) })
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pestana == 0) {
                    if (alertasFiltradas.isEmpty()) item { Text("Sin alertas registradas") }
                    items(alertasFiltradas, key = { it.id }) { a ->
                        TarjetaAlertaHistorial(a, nombres[a.galponId] ?: "Galpón ${a.galponId}")
                    }
                } else {
                    if (eventosFiltrados.isEmpty()) item { Text("Aún no hay acciones registradas") }
                    items(eventosFiltrados, key = { it.id }) { e ->
                        TarjetaEvento(e, nombres[e.galponId] ?: "Galpón ${e.galponId}")
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAlertaHistorial(alerta: AlertaEntity, galpon: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(colorNivel(alerta.nivel), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tipoAlertaLegible(alerta.tipo), style = MaterialTheme.typography.titleSmall)
                Text("$galpon · ${formatoFechaHora(alerta.fechaHora)}", style = MaterialTheme.typography.bodySmall)
                Text(estadoConfirmacion(alerta), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (alerta.activa) "Activa" else "Resuelta",
                style = MaterialTheme.typography.labelLarge,
                color = if (alerta.activa) colorNivel(alerta.nivel) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaEvento(evento: EventoEntity, galpon: String) {
    val accion = evento.accionRegistrada ?: "Evento"
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(accion, style = MaterialTheme.typography.titleSmall)
            Text(evento.descripcion.removePrefix("[$accion] "))
            Text("$galpon · ${formatoFechaHora(evento.fechaHora)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
