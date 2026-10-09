package com.example.caso1.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.Umbrales
import com.example.caso1.ui.theme.EstiloCifra
import com.example.caso1.ui.theme.EstiloEstado
import com.example.caso1.ui.theme.LocalPaletaEstados
import com.example.caso1.viewmodel.DetalleUiState
import com.example.caso1.viewmodel.DetalleViewModel

@Composable
fun DetalleGalponScreen(onBack: () -> Unit, onRegistrar: ((Int) -> Unit)?, viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BarraSecundaria(nombreGalpon(state.galpon, state.galponId), onBack) }) { padding ->
        DetalleContenido(
            state,
            Modifier.padding(padding),
            onRegistrar = onRegistrar?.let { { it(state.galponId) } }
        )
    }
}

/** Contenido del detalle, reutilizado como panel derecho en pantallas expandidas. */
@Composable
fun DetalleContenido(state: DetalleUiState, modifier: Modifier = Modifier, onRegistrar: (() -> Unit)? = null) {
    val mediciones = state.mediciones
    val ultima = state.ultima
    val estado = state.estado

    if (ultima == null || estado == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PlacaEstado(ultima, state) }

        item {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.detalle_temperatura), style = MaterialTheme.typography.titleMedium)
                Text(
                    rangoCubierto(mediciones),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    GraficoTemperatura(mediciones.reversed(), Modifier.padding(12.dp))
                }
                if (onRegistrar != null) {
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = onRegistrar, modifier = Modifier.fillMaxWidth()) {
                        Icono(R.drawable.ic_edit_note, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.detalle_registrar))
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                Text(stringResource(R.string.detalle_mediciones), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FilaTabla(
                    stringResource(R.string.detalle_col_hora), stringResource(R.string.detalle_col_temp),
                    stringResource(R.string.detalle_col_humedad), encabezado = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        items(mediciones, key = { it.id }) { m ->
            Column(Modifier.padding(horizontal = 16.dp)) {
                FilaTabla(
                    formatoHora(m.fechaHora),
                    stringResource(R.string.valor_temperatura, cifra(m.temperatura)),
                    stringResource(R.string.valor_humedad, cifra(m.humedad, 0)),
                    medicion = m
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/** Placa del estado actual: el bloque de color pleno de esta pantalla. */
@Composable
private fun PlacaEstado(ultima: MedicionEntity, state: DetalleUiState) {
    val estado = state.estado ?: return
    val c = estado.colores()
    val causa = Umbrales.causa(ultima.temperatura, ultima.humedad)
    Surface(color = c.franja, contentColor = c.sobreFranja) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                estado.etiqueta().uppercase() + (causa?.let { " · " + tipoAlertaLegible(it).uppercase() } ?: ""),
                style = EstiloEstado
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(cifra(ultima.temperatura), style = MaterialTheme.typography.displayLarge)
                Text(" °C", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icono(R.drawable.ic_water_drop, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.humedad_etiqueta, cifra(ultima.humedad, 0)), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.detalle_lectura_de, formatoHora(ultima.fechaHora)), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun FilaTabla(hora: String, temp: String, humedad: String, encabezado: Boolean = false, medicion: MedicionEntity? = null) {
    val estilo = if (encabezado) MaterialTheme.typography.labelMedium else EstiloCifra
    val tenue = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(hora, style = estilo, color = tenue, modifier = Modifier.weight(1f))
        Text(
            temp, style = estilo, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
            color = if (encabezado || medicion == null) tenue else colorCifra(estadoTemperatura(medicion.temperatura))
        )
        Text(
            humedad, style = estilo, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
            color = if (encabezado || medicion == null) tenue else colorCifra(estadoHumedad(medicion.humedad))
        )
        // Estado de la lectura completa (temperatura y humedad juntas), igual que el galpón
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (medicion == null) {
                Text(stringResource(R.string.detalle_col_estado), style = estilo, color = tenue)
            } else {
                val e = Umbrales.evaluar(medicion.temperatura, medicion.humedad)
                MarcaEstado(e)
                Spacer(Modifier.width(6.dp))
                Text(e.etiquetaCorta(), style = MaterialTheme.typography.labelMedium, color = colorCifra(e, tenue))
            }
        }
    }
}

/** "Últimas 6 h · …" o, con pocas lecturas, "Últimas 4 lecturas · …". */
@Composable
private fun rangoCubierto(mediciones: List<MedicionEntity>): String {
    val horas = if (mediciones.size < 2) 0.0
        else (mediciones.first().fechaHora - mediciones.last().fechaHora) / 3_600_000.0
    return if (horas < 1.5) stringResource(R.string.detalle_rango_lecturas, mediciones.size)
        else stringResource(R.string.detalle_rango_horas, Math.round(horas).toInt())
}

/** Gráfico de línea con las zonas de los umbrales sombreadas y la última lectura marcada. */
@Composable
private fun GraficoTemperatura(cronologicas: List<MedicionEntity>, modifier: Modifier = Modifier) {
    if (cronologicas.size < 2) return
    val paleta = LocalPaletaEstados.current
    val linea = MaterialTheme.colorScheme.primary
    val fondo = MaterialTheme.colorScheme.surfaceContainerLowest
    val estiloEje = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val medidor = rememberTextMeasurer()
    val temps = cronologicas.map { it.temperatura }
    // Rango ajustado a los datos, pero siempre mostrando las líneas de 28 °C y 32 °C
    val min = minOf(temps.min(), Umbrales.TEMP_NORMAL_MAX) - 1.5
    val max = maxOf(temps.max(), Umbrales.TEMP_ADVERTENCIA_MAX) + 1.5
    val ultima = temps.last()
    val colorUltima = estadoTemperatura(ultima).let { paleta.de(it).franja }
    val descripcion = stringResource(R.string.detalle_grafico_descripcion, cifra(temps.first()), cifra(ultima))

    Canvas(modifier.fillMaxWidth().height(190.dp).semantics { contentDescription = descripcion }) {
        val margenDerecho = 30.dp.toPx()
        val margenInferior = 18.dp.toPx()
        val ancho = size.width - margenDerecho
        val alto = size.height - margenInferior
        fun y(t: Double) = (alto * (1 - (t - min) / (max - min))).toFloat()
        val paso = ancho / (temps.size - 1)

        // Zonas: amarilla entre 28 y 32 °C, roja sobre 32 °C
        drawRect(paleta.advertencia.franja.copy(alpha = 0.14f), Offset(0f, y(Umbrales.TEMP_ADVERTENCIA_MAX)),
            Size(ancho, y(Umbrales.TEMP_NORMAL_MAX) - y(Umbrales.TEMP_ADVERTENCIA_MAX)))
        drawRect(paleta.critico.franja.copy(alpha = 0.10f), Offset(0f, 0f), Size(ancho, y(Umbrales.TEMP_ADVERTENCIA_MAX)))

        val punteada = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        listOf(Umbrales.TEMP_NORMAL_MAX to paleta.advertencia.franja, Umbrales.TEMP_ADVERTENCIA_MAX to paleta.critico.franja)
            .forEach { (umbral, color) ->
                drawLine(color, Offset(0f, y(umbral)), Offset(ancho, y(umbral)), strokeWidth = 2f, pathEffect = punteada)
                val t = medidor.measure("${umbral.toInt()}°", estiloEje)
                drawText(t, topLeft = Offset(ancho + 6.dp.toPx(), y(umbral) - t.size.height / 2f))
            }

        val path = Path().apply {
            temps.forEachIndexed { i, t -> if (i == 0) moveTo(0f, y(t)) else lineTo(i * paso, y(t)) }
        }
        drawPath(path, linea, style = Stroke(width = 2.5.dp.toPx()))

        // Última lectura: punto con anillo del color de su estado
        val fin = Offset(ancho, y(ultima))
        drawCircle(fondo, radius = 7.dp.toPx(), center = fin)
        drawCircle(colorUltima, radius = 5.dp.toPx(), center = fin)

        // Eje de tiempo: primera y última hora
        val inicio = medidor.measure(formatoHora(cronologicas.first().fechaHora), estiloEje)
        val ahora = medidor.measure(formatoHora(cronologicas.last().fechaHora), estiloEje)
        drawText(inicio, topLeft = Offset(0f, alto + 4.dp.toPx()))
        drawText(ahora, topLeft = Offset(ancho - ahora.size.width, alto + 4.dp.toPx()))
    }
}
