package com.example.caso1

import com.example.caso1.data.local.MedicionEntity
import com.example.caso1.viewmodel.PeriodoGrafico
import com.example.caso1.viewmodel.filtrarPorPeriodo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Respuesta 11 del docente: los gráficos muestran un período determinado (6 h, 24 h o todo). */
class PeriodoGraficoTest {

    private val ahora = 1_800_000_000_000L
    private val hora = 3_600_000L

    private fun medicion(id: Int, horasAtras: Long) = MedicionEntity(id, 1, 24.0, 60.0, ahora - horasAtras * hora)

    // De la más reciente a la más antigua, como las entrega el repositorio
    private val mediciones = listOf(medicion(4, 5), medicion(3, 7), medicion(2, 20), medicion(1, 25))

    @Test fun seisHorasConservaLasUltimasSeisYDroparLasViejas() {
        assertEquals(
            listOf(4),
            filtrarPorPeriodo(mediciones, ahora, PeriodoGrafico.SEIS_HORAS).map { it.id }
        )
    }

    @Test fun veinticuatroHorasConservaLasUltimasVeinticuatro() {
        assertEquals(
            listOf(4, 3, 2),
            filtrarPorPeriodo(mediciones, ahora, PeriodoGrafico.VEINTICUATRO_HORAS).map { it.id }
        )
    }

    @Test fun todoConservaTodasYRespetaElOrden() {
        assertEquals(mediciones, filtrarPorPeriodo(mediciones, ahora, PeriodoGrafico.TODO))
    }
}
