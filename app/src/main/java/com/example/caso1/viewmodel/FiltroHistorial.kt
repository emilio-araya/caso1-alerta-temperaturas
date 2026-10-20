package com.example.caso1.viewmodel

import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.EventoEntity
import java.util.Calendar

/*
 * Filtros del historial (respuesta 6 del docente): por galpón, por tipo de alerta y por
 * fecha. Son funciones puras para probarlas sin Android ni Compose.
 */

/** Rango de fecha del historial: el día de hoy, los últimos 7 días o todo. */
enum class RangoHistorial { HOY, SEMANA, TODO }

data class FiltroHistorial(
    val galponId: Int? = null,          // null = todos los galpones
    val tipo: String? = null,           // null = todos los tipos; solo aplica a las alertas
    val rango: RangoHistorial = RangoHistorial.TODO
)

/** Conserva el orden de [lista] (el repositorio ya la entrega de la más nueva a la más antigua). */
fun filtrarAlertas(lista: List<AlertaEntity>, filtro: FiltroHistorial, ahora: Long): List<AlertaEntity> =
    lista.filter { alerta ->
        (filtro.galponId == null || alerta.galponId == filtro.galponId) &&
            (filtro.tipo == null || alerta.tipo == filtro.tipo) &&
            dentroDelRango(alerta.fechaHora, filtro.rango, ahora)
    }

fun filtrarEventos(lista: List<EventoEntity>, filtro: FiltroHistorial, ahora: Long): List<EventoEntity> =
    lista.filter { evento ->
        (filtro.galponId == null || evento.galponId == filtro.galponId) &&
            dentroDelRango(evento.fechaHora, filtro.rango, ahora)
    }

private const val SEMANA_MS = 7L * 24 * 60 * 60_000

private fun dentroDelRango(fechaHora: Long, rango: RangoHistorial, ahora: Long): Boolean = when (rango) {
    RangoHistorial.TODO -> true
    RangoHistorial.SEMANA -> fechaHora >= ahora - SEMANA_MS
    RangoHistorial.HOY -> esDelMismoDia(fechaHora, ahora)
}

/** "Hoy" es el mismo día del calendario local, no las últimas 24 h. */
private fun esDelMismoDia(a: Long, b: Long): Boolean {
    val primero = Calendar.getInstance().apply { timeInMillis = a }
    val segundo = Calendar.getInstance().apply { timeInMillis = b }
    return primero.get(Calendar.YEAR) == segundo.get(Calendar.YEAR) &&
        primero.get(Calendar.DAY_OF_YEAR) == segundo.get(Calendar.DAY_OF_YEAR)
}
