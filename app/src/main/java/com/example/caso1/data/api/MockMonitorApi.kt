package com.example.caso1.data.api

import com.example.caso1.data.model.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * API simulada del sistema de monitoreo.
 * En el MVP no hay backend real: devuelve datos ficticios.
 *
 * Los datos son deterministas: cada medición depende solo del galpón y del
 * intervalo de tiempo, así todas las llamadas (estado, alertas, historial)
 * ven exactamente los mismos valores, igual que un servidor real.
 */
object MockMonitorApi {

    /** Cada cuánto "llega" una medición nueva del sensor. */
    private const val INTERVALO_MS = 15 * 60_000L
    private const val HISTORIAL = 24
    private const val INTERVALOS_POR_DIA = 24 * 60 * 60_000L / INTERVALO_MS

    private val galpones = listOf(
        Galpon(1, "Granja Norte", "Galpón 1"),
        Galpon(2, "Granja Norte", "Galpón 2"),
        Galpon(3, "Granja Sur", "Galpón 3"),
        Galpon(4, "Granja Sur", "Galpón 4")
    )

    fun getGalpones(): List<Galpon> = galpones.map { g ->
        val u = getUltimaMedicion(g.id)
        g.copy(estado = Umbrales.evaluar(u.temperatura, u.humedad))
    }

    /** Historial del galpón, de la más reciente a la más antigua. */
    fun getMediciones(galponId: Int): List<Medicion> {
        val actual = System.currentTimeMillis() / INTERVALO_MS
        return (0 until HISTORIAL).map { i -> medicionEn(galponId, actual - i) }
    }

    fun getUltimaMedicion(galponId: Int): Medicion =
        medicionEn(galponId, System.currentTimeMillis() / INTERVALO_MS)

    fun getAlertasActivas(): List<Alerta> =
        galpones.mapNotNull { g ->
            val u = getUltimaMedicion(g.id)
            val estado = Umbrales.evaluar(u.temperatura, u.humedad)
            val causa = Umbrales.causa(u.temperatura, u.humedad)
            if (estado == EstadoGalpon.NORMAL || causa == null) null
            else Alerta(
                galponId = g.id,
                tipo = causa,
                nivel = if (estado == EstadoGalpon.CRITICO) NivelAlerta.CRITICO else NivelAlerta.ADVERTENCIA,
                activa = true,
                fechaHora = u.fechaHora
            )
        }

    /** Lecturas forzadas por el modo demo, por (galpón, intervalo). */
    private val lecturasForzadas = mutableMapOf<Pair<Int, Long>, Medicion>()

    /**
     * Modo demo: fuerza un pico de temperatura crítico en un galpón que hoy no esté
     * crítico (o en el 1 si todos lo están). Devuelve el id del galpón afectado.
     * Solo vive en memoria: se pierde si el sistema mata el proceso.
     */
    @Synchronized
    fun simularEventoCritico(): Int {
        val intervalo = System.currentTimeMillis() / INTERVALO_MS
        val galponId = getGalpones().firstOrNull { it.estado != EstadoGalpon.CRITICO }?.id ?: galpones.first().id
        val normal = medicionEn(galponId, intervalo)
        lecturasForzadas[galponId to intervalo] = normal.copy(temperatura = 35.0 + Random.nextDouble(0.0, 1.5))
        return galponId
    }

    /**
     * Ciclo suave de 24 h desfasado por galpón, más un
     * pequeño ruido. Así el historial parece el de un sensor real y no saltos al azar.
     */
    private fun medicionEn(galponId: Int, intervalo: Long): Medicion {
        lecturasForzadas[galponId to intervalo]?.let { return it }
        val r = Random(galponId * 1_000_003L + intervalo)
        val fase = 2 * PI * (intervalo % INTERVALOS_POR_DIA) / INTERVALOS_POR_DIA + galponId * 1.3
        return Medicion(
            galponId = galponId,
            temperatura = 26.0 + 7.0 * sin(fase) + r.nextDouble(-1.5, 1.5),
            humedad = 66.0 + 14.0 * sin(fase + PI / 3) + r.nextDouble(-3.0, 3.0),
            fechaHora = intervalo * INTERVALO_MS
        )
    }
}
