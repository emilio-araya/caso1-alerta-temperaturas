package com.example.caso1.ui.screen

import androidx.annotation.StringRes
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
import androidx.compose.ui.graphics.Color
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
import com.example.caso1.data.local.MedicionEntity
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.ui.theme.EstiloCifra
import com.example.caso1.ui.theme.EstiloEstado
import com.example.caso1.ui.theme.LocalPaletaEstados
import com.example.caso1.viewmodel.DetalleUiState
import com.example.caso1.viewmodel.DetalleViewModel
import com.example.caso1.viewmodel.PeriodoGrafico

@Composable
fun DetalleGalponScreen(onBack: () -> Unit, onRegistrar: ((Int) -> Unit)?, viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BarraSecundaria(nombreGalpon(state.galpon, state.galponId), onBack) }) { padding ->
        DetalleContenido(
            state,
            Modifier.padding(padding),
            onRegistrar = onRegistrar?.let { { it(state.galponId) } },
            onCambiarPeriodo = viewModel::cambiarPeriodo
        )
    }
}

/** Contenido del detalle, reutilizado como panel derecho en pantallas expandidas. */
@Composable
fun DetalleContenido(
    state: DetalleUiState,
    modifier: Modifier = Modifier,
    onRegistrar: (() -> Unit)? = null,
    onCambiarPeriodo: (PeriodoGrafico) -> Unit = {}
) {
    val mediciones = state.mediciones
    val ultima = state.ultima
    val estado = state.estado

    if (ultima == null || estado == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PlacaEstado(ultima, state) }

        // Qué hacer según el estado; en crítico el operario registra la acción correctiva desde aquí
        if (estado != EstadoGalpon.NORMAL) {
            item {
                AccionRecomendada(estado, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), onRegistrarCorrectiva = onRegistrar)
            }
        }

        item {
            Column(Modifier.padding(16.dp)) {
                // Período del gráfico (respuesta 11): 6 h por defecto, 24 h o todo lo guardado
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodoGrafico.entries.forEach { p ->
                        FilterChip(
                            selected = state.periodo == p,
                            onClick = { onCambiarPeriodo(p) },
                            label = { Text(stringResource(p.etiquetaRes())) }
                        )
                    }
                }
                Text(
                    rangoCubierto(state.medicionesGrafico),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Text(stringResource(R.string.detalle_temperatura), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    GraficoTemperatura(state.medicionesGrafico, state.umbrales, Modifier.padding(12.dp))
                }

                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.detalle_humedad), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    GraficoHumedad(state.medicionesGrafico, state.umbrales, Modifier.padding(12.dp))
                }

                // En crítico ya está el botón "Registrar acción correctiva" arriba
                if (onRegistrar != null && estado != EstadoGalpon.CRITICO) {
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
                    stringResource(R.string.detalle_col_humedad), state.umbrales, encabezado = true
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
                    state.umbrales,
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
    val causa = state.umbrales.causa(ultima.temperatura, ultima.humedad)
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
private fun FilaTabla(
    hora: String, temp: String, humedad: String, umbrales: ConfigUmbrales,
    encabezado: Boolean = false, medicion: MedicionEntity? = null
) {
    val estilo = if (encabezado) MaterialTheme.typography.labelMedium else EstiloCifra
    val tenue = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(hora, style = estilo, color = tenue, modifier = Modifier.weight(1f))
        Text(
            temp, style = estilo, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
            color = if (encabezado || medicion == null) tenue else colorCifra(estadoTemperatura(medicion.temperatura, umbrales))
        )
        Text(
            humedad, style = estilo, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
            color = if (encabezado || medicion == null) tenue else colorCifra(estadoHumedad(medicion.humedad, umbrales))
        )
        // Estado de la lectura completa (temperatura y humedad juntas), igual que el galpón
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (medicion == null) {
                Text(stringResource(R.string.detalle_col_estado), style = estilo, color = tenue)
            } else {
                val e = umbrales.evaluar(medicion.temperatura, medicion.humedad)
                MarcaEstado(e)
                Spacer(Modifier.width(6.dp))
                Text(e.etiquetaCorta(), style = MaterialTheme.typography.labelMedium, color = colorCifra(e, tenue))
            }
        }
    }
}

/** "Últimas 6 h" o, con pocas lecturas, "Últimas 4 lecturas", para el período elegido. */
@Composable
private fun rangoCubierto(mediciones: List<MedicionEntity>): String {
    val horas = if (mediciones.size < 2) 0.0
        else Math.abs(mediciones.first().fechaHora - mediciones.last().fechaHora) / 3_600_000.0
    return if (horas < 1.5) stringResource(R.string.detalle_rango_lecturas, mediciones.size)
        else stringResource(R.string.detalle_rango_horas, Math.round(horas).toInt())
}

/** Respuesta 11: etiqueta corta del período del gráfico. */
@StringRes
private fun PeriodoGrafico.etiquetaRes(): Int = when (this) {
    PeriodoGrafico.SEIS_HORAS -> R.string.detalle_periodo_seis_horas
    PeriodoGrafico.VEINTICUATRO_HORAS -> R.string.detalle_periodo_veinticuatro_horas
    PeriodoGrafico.TODO -> R.string.detalle_periodo_todo
}

/** Temperatura del período: rango ajustado a los datos, mostrando siempre los dos umbrales altos. */
@Composable
private fun GraficoTemperatura(cronologicas: List<MedicionEntity>, umbrales: ConfigUmbrales, modifier: Modifier = Modifier) {
    if (cronologicas.size < 2) return
    val paleta = LocalPaletaEstados.current
    val temps = cronologicas.map { it.temperatura }
    val normalMax = umbrales.tempNormalMax
    val advertenciaMax = umbrales.tempAdvertenciaMax
    val min = minOf(temps.min(), normalMax) - 1.5
    val max = maxOf(temps.max(), advertenciaMax) + 1.5
    val ultima = temps.last()

    GraficoLinea(
        cronologicas = cronologicas,
        valores = temps,
        min = min,
        max = max,
        // Zonas: amarilla entre el máximo normal y el de advertencia, roja sobre este último
        franjas = listOf(
            Triple(normalMax, advertenciaMax, paleta.advertencia.franja.copy(alpha = 0.14f)),
            Triple(advertenciaMax, max, paleta.critico.franja.copy(alpha = 0.10f))
        ),
        umbrales = listOf(normalMax to paleta.advertencia.franja, advertenciaMax to paleta.critico.franja),
        sufijo = "°",
        formatear = ::cifraUmbral,
        colorUltima = estadoTemperatura(ultima, umbrales).let { paleta.de(it).franja },
        descripcion = stringResource(R.string.detalle_grafico_descripcion, cifra(temps.first()), cifra(ultima)),
        modifier = modifier
    )
}

/** Humedad relativa del período: rango ajustado a los datos, mostrando siempre sus tres umbrales. */
@Composable
private fun GraficoHumedad(cronologicas: List<MedicionEntity>, umbrales: ConfigUmbrales, modifier: Modifier = Modifier) {
    if (cronologicas.size < 2) return
    val paleta = LocalPaletaEstados.current
    val valores = cronologicas.map { it.humedad }
    val normalMin = umbrales.humedadNormalMin
    val normalMax = umbrales.humedadNormalMax
    val advertenciaMax = umbrales.humedadAdvertenciaMax
    val min = maxOf(0.0, minOf(valores.min(), normalMin) - 5)
    val max = minOf(100.0, maxOf(valores.max(), advertenciaMax) + 5)
    val ultima = valores.last()

    GraficoLinea(
        cronologicas = cronologicas,
        valores = valores,
        min = min,
        max = max,
        franjas = listOf(
            Triple(normalMax, advertenciaMax, paleta.advertencia.franja.copy(alpha = 0.14f)),
            Triple(advertenciaMax, max, paleta.critico.franja.copy(alpha = 0.10f))
        ),
        umbrales = listOf(
            normalMin to paleta.advertencia.franja,
            normalMax to paleta.advertencia.franja,
            advertenciaMax to paleta.critico.franja
        ),
        sufijo = "%",
        formatear = ::cifraUmbral,
        colorUltima = estadoHumedad(ultima, umbrales).let { paleta.de(it).franja },
        descripcion = stringResource(R.string.detalle_grafico_humedad_descripcion, cifra(valores.first(), 0), cifra(ultima, 0)),
        modifier = modifier
    )
}

/**
 * Gráfico de línea compartido por temperatura y humedad: zonas de los umbrales sombreadas,
 * líneas punteadas con su valor y la última lectura marcada con el color de su estado.
 */
@Composable
private fun GraficoLinea(
    cronologicas: List<MedicionEntity>,
    valores: List<Double>,
    min: Double,
    max: Double,
    franjas: List<Triple<Double, Double, Color>>,
    umbrales: List<Pair<Double, Color>>,
    sufijo: String,
    formatear: (Double) -> String,
    colorUltima: Color,
    descripcion: String,
    modifier: Modifier = Modifier
) {
    val linea = MaterialTheme.colorScheme.primary
    val fondo = MaterialTheme.colorScheme.surfaceContainerLowest
    val estiloEje = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val medidor = rememberTextMeasurer()
    val ultima = valores.last()

    Canvas(modifier.fillMaxWidth().height(190.dp).semantics { contentDescription = descripcion }) {
        val margenDerecho = 30.dp.toPx()
        val margenInferior = 18.dp.toPx()
        val ancho = size.width - margenDerecho
        val alto = size.height - margenInferior
        fun y(t: Double) = (alto * (1 - (t - min) / (max - min))).toFloat()
        val paso = ancho / (valores.size - 1)

        franjas.forEach { (desde, hasta, color) ->
            drawRect(color, Offset(0f, y(hasta)), Size(ancho, y(desde) - y(hasta)))
        }

        val punteada = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        umbrales.forEach { (umbral, color) ->
            drawLine(color, Offset(0f, y(umbral)), Offset(ancho, y(umbral)), strokeWidth = 2f, pathEffect = punteada)
            val t = medidor.measure("${formatear(umbral)}$sufijo", estiloEje)
            drawText(t, topLeft = Offset(ancho + 6.dp.toPx(), y(umbral) - t.size.height / 2f))
        }

        val path = Path().apply {
            valores.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * paso, y(v)) }
        }
        drawPath(path, linea, style = Stroke(width = 2.5.dp.toPx()))

        // Última lectura: punto con anillo del color de su estado
        val fin = Offset(ancho, y(ultima))
        drawCircle(fondo, radius = 7.dp.toPx(), center = fin)
        drawCircle(colorUltima, radius = 5.dp.toPx(), center = fin)

        // Eje de tiempo: primera y última hora del período
        val inicio = medidor.measure(formatoHora(cronologicas.first().fechaHora), estiloEje)
        val finHora = medidor.measure(formatoHora(cronologicas.last().fechaHora), estiloEje)
        drawText(inicio, topLeft = Offset(0f, alto + 4.dp.toPx()))
        drawText(finHora, topLeft = Offset(ancho - finHora.size.width, alto + 4.dp.toPx()))
    }
}
