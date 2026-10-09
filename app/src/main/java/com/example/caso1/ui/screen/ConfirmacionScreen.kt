package com.example.caso1.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caso1.R
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.viewmodel.RegistroAccionViewModel

/** Muestra lo registrado leyendo el mismo ViewModel que el formulario (Guía 11), sin argumentos de ruta. */
@Composable
fun ConfirmacionScreen(viewModel: RegistroAccionViewModel, onVolver: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val registro = state.ultimoRegistro

    Scaffold(topBar = { BarraSecundaria(stringResource(R.string.confirmacion_barra), onVolver) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icono(R.drawable.ic_check_circle, null, Modifier.size(64.dp), EstadoGalpon.NORMAL.colores().texto)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.confirmacion_titulo), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.confirmacion_texto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            if (registro != null) {
                Card(
                    Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Dato(stringResource(R.string.confirmacion_galpon), nombreGalpon(registro.granja, registro.galpon, 0))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Dato(stringResource(R.string.confirmacion_tipo), stringResource(registro.tipo.etiqueta))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Dato(stringResource(R.string.confirmacion_que), registro.descripcion)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Dato(stringResource(R.string.confirmacion_hora), formatoFechaHora(registro.fechaHora))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onVolver, modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.confirmacion_volver))
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp))
        Text(valor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
