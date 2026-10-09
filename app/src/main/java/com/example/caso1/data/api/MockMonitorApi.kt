package com.example.caso1.data.api

import com.example.caso1.data.model.*
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

    fun getEventos(): List<Evento> = emptyList()

    private fun medicionEn(galponId: Int, intervalo: Long): Medicion {
        val r = Random(galponId * 1_000_003L + intervalo)
        return Medicion(
            galponId = galponId,
            temperatura = 22.0 + r.nextDouble(-4.0, 14.0),
            humedad = 60.0 + r.nextDouble(-15.0, 30.0),
            fechaHora = intervalo * INTERVALO_MS
        )
    }
}
