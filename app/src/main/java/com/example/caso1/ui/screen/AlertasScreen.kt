package com.example.caso1.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.viewmodel.AlertasViewModel

/**
 * Alertas activas. Operario y supervisor pueden confirmarlas (acusar recibo);
 * jefatura solo consulta. La alerta sigue activa hasta que el galpón vuelva a la normalidad.
 */
@Composable
fun AlertasScreen(marco: Marco, viewModel: AlertasViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nombres = state.nombres
    val rol = marco.rol
    val puedeConfirmar = viewModel.puedeConfirmar(rol)
    var porConfirmar by remember { mutableStateOf<AlertaEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }
    var mensaje by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mensaje) {
        mensaje?.let { snackbar.showSnackbar(it); mensaje = null }
    }

    PantallaPrincipal(
        marco = marco,
        actual = Destino.ALERTAS,
        titulo = "Alertas activas",
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.alertas.isEmpty()) {
            EstadoVacio(
                R.drawable.ic_check_circle,
                "Sin alertas activas",
                "Todos los galpones están en rango. Si alguno se sale, te llegará una notificación.",
                Modifier.padding(padding),
                tint = EstadoGalpon.NORMAL.colores().texto
            )
            return@PantallaPrincipal
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val pendientes = state.alertas.count { it.confirmadaPor == null }
                Text(
                    if (pendientes == 0) "Todas confirmadas" else "$pendientes por confirmar",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.alertas, key = { it.id }) { a ->
                TarjetaAlerta(
                    alerta = a,
                    galpon = nombres[a.galponId] ?: "Galpón ${a.galponId}",
                    puedeConfirmar = puedeConfirmar,
                    onConfirmar = { porConfirmar = a }
                )
            }
            if (!puedeConfirmar) {
                item {
                    Text(
                        "Con el perfil Jefatura las alertas se consultan; las confirman operarios y supervisores.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    porConfirmar?.let { a ->
        val galpon = nombres[a.galponId] ?: "Galpón ${a.galponId}"
        AlertDialog(
            onDismissRequest = { porConfirmar = null },
            icon = { Icono(R.drawable.ic_warning, null, tint = a.nivel.colores().texto) },
            title = { Text("¿Confirmar alerta?") },
            text = {
                Text(
                    "${tipoAlertaLegible(a.tipo)} en $galpon.\n\n" +
                        "Quedará en el historial que la recibiste. La alerta sigue activa " +
                        "hasta que el galpón vuelva a rango normal."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val r = rol ?: return@TextButton
                    porConfirmar = null
                    viewModel.confirmar(a, r, "${tipoAlertaLegible(a.tipo)} en $galpon (${r.etiqueta()})")
                    mensaje = "Alerta de $galpon confirmada"
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { porConfirmar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaAlerta(alerta: AlertaEntity, galpon: String, puedeConfirmar: Boolean, onConfirmar: () -> Unit) {
    val c = alerta.nivel.colores()
    val confirmada = alerta.confirmadaPor != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        FranjaEstado(alerta.nivel.estado, alerta.nivel.etiqueta(), extra = "Desde las ${formatoHora(alerta.fechaHora)}")
        Column(Modifier.padding(16.dp)) {
            Text(tipoAlertaLegible(alerta.tipo), style = MaterialTheme.typography.titleMedium)
            Text(galpon, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (confirmada) {
                    Icono(R.drawable.ic_check_circle, null, Modifier.size(18.dp), EstadoGalpon.NORMAL.colores().texto)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        estadoConfirmacion(alerta),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        "Sin confirmar",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.texto,
                        modifier = Modifier.weight(1f)
                    )
                    if (puedeConfirmar) {
                        Button(onClick = onConfirmar) {
                            Icono(R.drawable.ic_check_circle, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }
}
