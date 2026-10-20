package com.example.caso1.viewmodel

import com.example.caso1.data.local.MedicionEntity

/** Períodos del gráfico del detalle del galpón (respuesta 11 del docente). */
enum class PeriodoGrafico(val duracionMs: Long?) {
    SEIS_HORAS(6 * 3_600_000L),
    VEINTICUATRO_HORAS(24 * 3_600_000L),
    TODO(null)
}

/** Conserva las mediciones del período que termina en [ahora], en el orden de la lista recibida. */
fun filtrarPorPeriodo(mediciones: List<MedicionEntity>, ahora: Long, periodo: PeriodoGrafico): List<MedicionEntity> {
    val duracion = periodo.duracionMs ?: return mediciones
    return mediciones.filter { it.fechaHora >= ahora - duracion }
}
