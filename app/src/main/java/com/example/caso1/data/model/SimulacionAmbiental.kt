package com.example.caso1.data.model

import kotlin.math.PI
import kotlin.math.round
import kotlin.math.sin

/**
 * Valores ficticios de la próxima lectura automática de un galpón. Es pura y determinista
 * (misma entrada, misma salida).
 *
 * La lectura se acerca a un objetivo que oscila despacio (ciclo de una hora, desfasado por galpón)
 * en vez de sumarle un seno a la lectura anterior: si se sumara, en la media hora en que el seno
 * sube la simulación se arrastraría hasta los topes (40 °C / 95 %) y las tarjetas quedarían pegadas
 * al máximo. Con la reversión, una medición simulada a mano se mantiene unos minutos y después
 * vuelve de a poco al rango del galpón. Temperatura en °C (1 decimal) y humedad en % (entero).
 */
fun valoresSimulados(galponId: Int, minuto: Long, temperaturaBase: Double, humedadBase: Double): Pair<Double, Double> {
    val fase = 2 * PI * (minuto % 60) / 60.0 + galponId * 0.9
    // Cada galpón tiene su rango de referencia ficticio (24, 26, 28 o 30 °C; 55, 62 o 69 %)
    val objetivoTemperatura = 22.0 + (galponId % 4) * 2.0 + 3.0 * sin(fase)
    val objetivoHumedad = 55.0 + (galponId % 3) * 7.0 + 6.0 * sin(2 * fase)

    val temperatura = (temperaturaBase + 0.12 * (objetivoTemperatura - temperaturaBase)).coerceIn(12.0, 40.0)
    val humedad = (humedadBase + 0.15 * (objetivoHumedad - humedadBase)).coerceIn(25.0, 95.0)
    return round(temperatura * 10) / 10.0 to round(humedad)
}
