package com.example.caso1.data.api

import com.example.caso1.data.model.*
import kotlin.random.Random

/**
 * API simulada del sistema de monitoreo.
 * En el MVP no hay backend real: devuelve datos ficticios.
 */
object MockMonitorApi {

    private val galpones = listOf(
        Galpon(1, "Granja Norte", "Galpón 1"),
        Galpon(2, "Granja Norte", "Galpón 2"),
        Galpon(3, "Granja Sur", "Galpón 3"),
        Galpon(4, "Granja Sur", "Galpón 4")
    )

    fun getGalpones(): List<Galpon> = galpones.map { it.copy(estado = estadoActual(it.id)) }

    fun getMediciones(galponId: Int): List<Medicion> {
        val ahora = System.currentTimeMillis()
        return (0 until 24).map { i ->
            Medicion(
                id = galponId * 100 + i,
                galponId = galponId,
                temperatura = 22.0 + Random.nextDouble(-4.0, 14.0),
                humedad = 60.0 + Random.nextDouble(-15.0, 30.0),
                fechaHora = ahora - i * 3_600_000L
            )
        }
    }

    fun getUltimaMedicion(galponId: Int): Medicion {
        val m = getMediciones(galponId).first()
        return m.copy(temperatura = 20.0 + Random.nextDouble(0.0, 16.0))
    }

    fun getAlertasActivas(): List<Alerta> =
        getGalpones()
            .mapNotNull { g ->
                val u = getUltimaMedicion(g.id)
                val estado = Umbrales.evaluar(u.temperatura, u.humedad)
                if (estado == EstadoGalpon.NORMAL) null
                else Alerta(
                    galponId = g.id,
                    tipo = if (u.temperatura > 32) "TEMPERATURA_ALTA" else "HUMEDAD_ALTA",
                    nivel = if (estado == EstadoGalpon.CRITICO) NivelAlerta.CRITICO else NivelAlerta.ADVERTENCIA,
                    activa = true,
                    fechaHora = u.fechaHora
                )
            }

    fun getEventos(): List<Evento> = emptyList()

    private fun estadoActual(galponId: Int): EstadoGalpon {
        val u = getUltimaMedicion(galponId)
        return Umbrales.evaluar(u.temperatura, u.humedad)
    }
}
