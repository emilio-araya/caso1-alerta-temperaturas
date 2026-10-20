package com.example.caso1

import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.EventoEntity
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.viewmodel.FiltroHistorial
import com.example.caso1.viewmodel.RangoHistorial
import com.example.caso1.viewmodel.filtrarAlertas
import com.example.caso1.viewmodel.filtrarEventos
import org.junit.Assert.assertEquals
import org.junit.Test

/** Filtros del historial (respuesta 6 del docente): galpón, tipo de alerta y rango de fecha. */
class FiltroHistorialTest {

    private val ahora = 1_800_000_000_000L
    private val dia = 24 * 60 * 60_000L

    private fun alerta(id: Int, galponId: Int, tipo: String, cuando: Long) =
        AlertaEntity(id, galponId, tipo, NivelAlerta.CRITICO, true, cuando)

    private fun evento(id: Int, galponId: Int, cuando: Long) =
        EventoEntity(id, galponId, "Acción $id", "VENTILACION", cuando, "OPERARIO")

    // Ordenados como los entrega el repositorio: más recientes primero
    private val alertas = listOf(
        alerta(1, 1, "TEMPERATURA_ALTA", ahora),                 // hoy
        alerta(2, 2, "HUMEDAD_ALTA", ahora - dia),                // ayer
        alerta(3, 1, "HUMEDAD_ALTA", ahora - 6 * dia),            // dentro de la semana
        alerta(4, 2, "TEMPERATURA_ALTA", ahora - 8 * dia)         // fuera de la semana
    )
    private val eventos = listOf(
        evento(1, 1, ahora),
        evento(2, 2, ahora - dia),
        evento(3, 1, ahora - 6 * dia),
        evento(4, 2, ahora - 8 * dia)
    )

    @Test fun filtraAlertasPorGalpon() {
        val filtradas = filtrarAlertas(alertas, FiltroHistorial(galponId = 1), ahora)

        assertEquals(listOf(1, 3), filtradas.map { it.id })
    }

    @Test fun filtraEventosPorGalpon() {
        val filtrados = filtrarEventos(eventos, FiltroHistorial(galponId = 2), ahora)

        assertEquals(listOf(2, 4), filtrados.map { it.id })
    }

    @Test fun elTipoDeAlertaNoFiltraLasAcciones() {
        val filtro = FiltroHistorial(tipo = "HUMEDAD_ALTA")

        assertEquals(listOf(2, 3), filtrarAlertas(alertas, filtro, ahora).map { it.id })
        assertEquals(listOf(1, 2, 3, 4), filtrarEventos(eventos, filtro, ahora).map { it.id })
    }

    @Test fun hoyMantieneSoloElMismoDiaDelCalendario() {
        val filtro = FiltroHistorial(rango = RangoHistorial.HOY)

        assertEquals(listOf(1), filtrarAlertas(alertas, filtro, ahora).map { it.id })
        assertEquals(listOf(1), filtrarEventos(eventos, filtro, ahora).map { it.id })
    }

    @Test fun semanaMantieneSeisDiasAtrasYDescartaOcho() {
        val filtro = FiltroHistorial(rango = RangoHistorial.SEMANA)

        assertEquals(listOf(1, 2, 3), filtrarAlertas(alertas, filtro, ahora).map { it.id })
        assertEquals(listOf(1, 2, 3), filtrarEventos(eventos, filtro, ahora).map { it.id })
    }

    @Test fun todoMantieneLasListasCompletas() {
        val filtro = FiltroHistorial(rango = RangoHistorial.TODO)

        assertEquals(listOf(1, 2, 3, 4), filtrarAlertas(alertas, filtro, ahora).map { it.id })
        assertEquals(listOf(1, 2, 3, 4), filtrarEventos(eventos, filtro, ahora).map { it.id })
    }

    @Test fun combinaGalponTipoYRango() {
        val filtro = FiltroHistorial(galponId = 1, tipo = "HUMEDAD_ALTA", rango = RangoHistorial.SEMANA)

        assertEquals(listOf(3), filtrarAlertas(alertas, filtro, ahora).map { it.id })
        assertEquals(listOf(1, 3), filtrarEventos(eventos, filtro, ahora).map { it.id })
    }
}
