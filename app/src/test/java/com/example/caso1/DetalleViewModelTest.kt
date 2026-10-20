package com.example.caso1

import androidx.lifecycle.SavedStateHandle
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.local.MedicionEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.viewmodel.DetalleUiState
import com.example.caso1.viewmodel.DetalleViewModel
import com.example.caso1.viewmodel.PeriodoGrafico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Respuesta 11 del docente: el detalle grafica un período elegido (6 h por defecto) sin perder
 * la tabla completa de mediciones.
 */
class DetalleViewModelTest {

    @get:Rule val reglaMain = ReglaDispatcherPrincipal()

    private val ahora = 1_800_000_000_000L
    private val hora = 3_600_000L

    private val mediciones = listOf(
        MedicionEntity(3, 1, 24.0, 60.0, ahora - hora),
        MedicionEntity(2, 1, 24.5, 61.0, ahora - 7 * hora),
        MedicionEntity(1, 1, 25.0, 62.0, ahora - 20 * hora)
    )

    private val repo = RepositorioFalso(listOf(GalponEntity(1, "Granja Norte", "Galpón 1", EstadoGalpon.NORMAL))).apply {
        medicionesPorGalpon[1] = MutableStateFlow(mediciones)
    }

    // by lazy: viewModelScope necesita Dispatchers.Main ya reemplazado por la regla
    private val vm by lazy {
        DetalleViewModel(repo, SavedStateHandle(mapOf("galponId" to 1)), reloj = { ahora })
    }

    private suspend fun estado(): DetalleUiState = vm.uiState.first { it.mediciones.isNotEmpty() }

    @Test fun porDefectoGraficaLasUltimasSeisHorasYLaTablaSigueCompleta() = runTest {
        val s = estado()

        assertEquals(PeriodoGrafico.SEIS_HORAS, s.periodo)
        assertEquals(listOf(3), s.medicionesGrafico.map { it.id })
        assertEquals(3, s.mediciones.size)
    }

    @Test fun cambiarPeriodoATodoGraficaTodoEnOrdenCronologico() = runTest {
        vm.cambiarPeriodo(PeriodoGrafico.TODO)

        val s = vm.uiState.first { it.periodo == PeriodoGrafico.TODO }

        assertEquals(listOf(1, 2, 3), s.medicionesGrafico.map { it.id })
        assertEquals(listOf(3, 2, 1), s.mediciones.map { it.id })
    }
}
