package com.example.caso1.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.viewmodel.AlertaUi
import com.example.caso1.viewmodel.AlertasViewModel

/**
 * Alertas activas. Operario y supervisor pueden confirmarlas (acusar recibo);
 * jefatura solo consulta. La alerta sigue activa hasta que el galpón vuelva a la normalidad.
 */
@Composable
fun AlertasScreen(marco: Marco, viewModel: AlertasViewModel = viewModel(factory = AlertasViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rol = marco.rol
    val puedeConfirmar = viewModel.puedeConfirmar(rol)
    var porConfirmar by remember { mutableStateOf<AlertaUi?>(null) }
    val snackbar = remember { SnackbarHostState() }
    var mensaje by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mensaje) {
        mensaje?.let { snackbar.showSnackbar(it); mensaje = null }
    }

    PantallaPrincipal(
        marco = marco,
        actual = Destino.ALERTAS,
        titulo = stringResource(R.string.alertas_titulo),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.alertas.isEmpty()) {
            EstadoVacio(
                R.drawable.ic_check_circle,
                stringResource(R.string.alertas_vacio_titulo),
                stringResource(R.string.alertas_vacio_texto),
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
                Text(
                    if (state.pendientes == 0) stringResource(R.string.alertas_todas_confirmadas)
                    else pluralStringResource(R.plurals.alertas_por_confirmar, state.pendientes, state.pendientes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.alertas, key = { it.id }) { a ->
                TarjetaAlerta(alerta = a, puedeConfirmar = puedeConfirmar, onConfirmar = { porConfirmar = a })
            }
            if (!puedeConfirmar) {
                item {
                    Text(
                        stringResource(R.string.alertas_solo_consulta),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    porConfirmar?.let { a ->
        val galpon = nombreGalpon(a.granja, a.galpon, a.galponId)
        val tipo = tipoAlertaLegible(a.tipo)
        val recursos = LocalResources.current
        AlertDialog(
            onDismissRequest = { porConfirmar = null },
            icon = { Icono(R.drawable.ic_warning, null, tint = a.nivel.colores().texto) },
            title = { Text(stringResource(R.string.alertas_dialogo_titulo)) },
            text = { Text(stringResource(R.string.alertas_dialogo_texto, tipo, galpon)) },
            confirmButton = {
                TextButton(onClick = {
                    val r = rol ?: return@TextButton
                    porConfirmar = null
                    val detalle = recursos.getString(R.string.alertas_detalle_historial, tipo, galpon, recursos.getString(r.etiquetaRes()))
                    viewModel.confirmar(a, r, detalle)
                    mensaje = recursos.getString(R.string.alertas_confirmada_snackbar, galpon)
                }) { Text(stringResource(R.string.alertas_confirmar)) }
            },
            dismissButton = {
                TextButton(onClick = { porConfirmar = null }) { Text(stringResource(R.string.alertas_cancelar)) }
            }
        )
    }
}

@Composable
private fun TarjetaAlerta(alerta: AlertaUi, puedeConfirmar: Boolean, onConfirmar: () -> Unit) {
    val c = alerta.nivel.colores()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        FranjaEstado(
            alerta.nivel.estado,
            alerta.nivel.etiqueta(),
            extra = stringResource(R.string.alertas_desde, formatoHora(alerta.desde))
        )
        Column(Modifier.padding(16.dp)) {
            Text(tipoAlertaLegible(alerta.tipo), style = MaterialTheme.typography.titleMedium)
            Text(
                nombreGalpon(alerta.granja, alerta.galpon, alerta.galponId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (alerta.confirmada) {
                    Icono(R.drawable.ic_check_circle, null, Modifier.size(18.dp), EstadoGalpon.NORMAL.colores().texto)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        textoConfirmacion(alerta.confirmadaPor, alerta.confirmadaEn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        stringResource(R.string.alertas_sin_confirmar),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.texto,
                        modifier = Modifier.weight(1f)
                    )
                    if (puedeConfirmar) {
                        Button(onClick = onConfirmar) {
                            Icono(R.drawable.ic_check_circle, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.alertas_confirmar))
                        }
                    }
                }
            }
        }
    }
}
