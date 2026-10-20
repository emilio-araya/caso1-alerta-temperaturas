package com.example.caso1.data.model

import kotlin.math.PI
import kotlin.math.round
import kotlin.math.sin

/**
 * Valores ficticios de la próxima lectura automática de un galpón. Es pura y determinista
 * (misma entrada, misma salida): un seno lento alrededor de la última lectura, desfasado por
 * galpón. Así la demo es reproducible, cada galpón evoluciona distinto y las lecturas se
 * mantienen en rangos físicos. Temperatura en °C (1 decimal) y humedad en % (entero).
 */
fun valoresSimulados(galponId: Int, minuto: Long, temperaturaBase: Double, humedadBase: Double): Pair<Double, Double> {
    val fase = 2 * PI * (minuto % 60) / 60.0 + galponId * 0.9
    val temperatura = (temperaturaBase + 2.0 * sin(fase)).coerceIn(12.0, 40.0)
    val humedad = (humedadBase + 6.0 * sin(2 * fase)).coerceIn(25.0, 95.0)
    return round(temperatura * 10) / 10.0 to round(humedad)
}
