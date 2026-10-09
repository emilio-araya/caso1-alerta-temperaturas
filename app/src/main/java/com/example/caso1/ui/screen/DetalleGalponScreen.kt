package com.example.caso1.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.Umbrales
import com.example.caso1.ui.theme.EstadoAdvertencia
import com.example.caso1.ui.theme.EstadoCritico
import com.example.caso1.viewmodel.DetalleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleGalponScreen(onBack: () -> Unit, viewModel: DetalleViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mediciones = state.mediciones
    val galponId = viewModel.galponId

    Scaffold(
        topBar = { TopAppBar(title = { Text("Galpón $galponId") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("← Volver") }
        }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            val ultima = state.ultima
            val estado = state.estado
            item {
                if (ultima != null && estado != null) {
                    Text(estado.etiqueta(), style = MaterialTheme.typography.titleMedium, color = estado.color())
                    Text("Temperatura: ${"%.1f".format(ultima.temperatura)} °C", style = MaterialTheme.typography.headlineSmall)
                    Text("Humedad: ${"%.0f".format(ultima.humedad)} %", style = MaterialTheme.typography.titleLarge)
                    Text("Actualizado: ${formatoFechaHora(ultima.fechaHora)}")
                    Spacer(Modifier.height(16.dp))
                    Text("Temperatura (últimas ${mediciones.size} mediciones)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    GraficoTemperatura(mediciones.reversed())
                    Text(
                        "Líneas: advertencia ${Umbrales.TEMP_NORMAL_MAX.toInt()} °C · crítico ${Umbrales.TEMP_ADVERTENCIA_MAX.toInt()} °C",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Historial", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Sin mediciones registradas")
                }
            }
            items(mediciones) { m ->
                Text("${formatoFechaHora(m.fechaHora)} — ${"%.1f".format(m.temperatura)} °C, ${"%.0f".format(m.humedad)} %")
                HorizontalDivider()
            }
        }
    }
}

/** Gráfico de línea simple con Canvas, con los umbrales como líneas punteadas. */
@Composable
private fun GraficoTemperatura(cronologicas: List<MedicionEntity>) {
    if (cronologicas.size < 2) return
    val linea = MaterialTheme.colorScheme.primary
    val ejes = MaterialTheme.colorScheme.outlineVariant
    val temps = cronologicas.map { it.temperatura }
    val min = minOf(temps.min(), Umbrales.TEMP_NORMAL_MIN) - 1
    val max = maxOf(temps.max(), Umbrales.TEMP_ADVERTENCIA_MAX) + 1

    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        fun y(t: Double) = (size.height * (1 - (t - min) / (max - min))).toFloat()
        val paso = size.width / (temps.size - 1)
        val punteada = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))

        drawLine(ejes, Offset(0f, size.height), Offset(size.width, size.height))
        listOf(Umbrales.TEMP_NORMAL_MAX to EstadoAdvertencia, Umbrales.TEMP_ADVERTENCIA_MAX to EstadoCritico)
            .forEach { (umbral, color) ->
                drawLine(color, Offset(0f, y(umbral)), Offset(size.width, y(umbral)), strokeWidth = 2f, pathEffect = punteada)
            }

        val path = Path().apply {
            temps.forEachIndexed { i, t -> if (i == 0) moveTo(0f, y(t)) else lineTo(i * paso, y(t)) }
        }
        drawPath(path, linea, style = Stroke(width = 4f))
        temps.forEachIndexed { i, t ->
            val color = when {
                t > Umbrales.TEMP_ADVERTENCIA_MAX -> EstadoCritico
                t > Umbrales.TEMP_NORMAL_MAX -> EstadoAdvertencia
                else -> linea
            }
            drawCircle(color, radius = 5f, center = Offset(i * paso, y(t)))
        }
    }
}
