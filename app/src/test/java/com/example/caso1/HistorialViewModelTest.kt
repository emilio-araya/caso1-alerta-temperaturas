package com.example.caso1

import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.EventoEntity
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.viewmodel.FiltroHistorial
import com.example.caso1.viewmodel.HistorialUiState
import com.example.caso1.viewmodel.HistorialViewModel
import com.example.caso1.viewmodel.RangoHistorial
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistorialViewModelTest {

    @get:Rule val reglaMain = ReglaDispatcherPrincipal()

    private val ahora = 1_800_000_000_000L
    private val dia = 24 * 60 * 60_000L

    private val galpones = listOf(
        GalponEntity(1, "Granja Norte", "Galpón 1", EstadoGalpon.CRITICO),
        GalponEntity(2, "Granja Sur", "Galpón 2", EstadoGalpon.ADVERTENCIA)
    )

    private val alertasGuardadas = listOf(
        AlertaEntity(5, 1, "TEMPERATURA_ALTA", NivelAlerta.CRITICO, true, ahora, temperatura = 33.5, humedad = 60.0),
        AlertaEntity(4, 2, "HUMEDAD_ALTA", NivelAlerta.ADVERTENCIA, false, ahora - 8 * dia, temperatura = 24.0, humedad = 85.0),
        AlertaEntity(3, 1, "HUMEDAD_ALTA", NivelAlerta.ADVERTENCIA, false, ahora - 2 * dia, temperatura = 24.0, humedad = 85.0)
    )

    private val eventosGuardados = listOf(
        EventoEntity(1, 1, "Ventilé", "VENTILACION", ahora, "OPERARIO"),
        EventoEntity(2, 2, "Revisé equipos", "REVISION_EQUIPOS", ahora - 8 * dia, "SUPERVISOR")
    )

    private val repo = RepositorioFalso(galpones).apply {
        alertas.value = alertasGuardadas
        eventos.value = eventosGuardados
    }

    // by lazy: viewModelScope necesita Dispatchers.Main ya reemplazado por la regla
    private val vm by lazy { HistorialViewModel(repo, reloj = { ahora }) }

    /** Espera la primera emisión que ya combina las listas (galpones llega sin demora). */
    private suspend fun estado(): HistorialUiState = vm.uiState.first { it.galpones.isNotEmpty() }

    @Test fun filtraPorGalpon() = runTest {
        vm.aplicarFiltro(FiltroHistorial(galponId = 1))

        val s = estado()

        assertEquals(listOf(5, 3), s.alertas.map { it.id })
        assertEquals(listOf(1), s.eventos.map { it.id })
        assertEquals(1, s.filtro.galponId)
    }

    @Test fun elTipoSoloFiltraLasAlertas() = runTest {
        vm.aplicarFiltro(FiltroHistorial(tipo = "HUMEDAD_ALTA"))

        val s = estado()

        assertEquals(listOf(4, 3), s.alertas.map { it.id })
        assertEquals(listOf(1, 2), s.eventos.map { it.id })
    }

    @Test fun elRangoSeAplicaALasDosPestanas() = runTest {
        vm.aplicarFiltro(FiltroHistorial(rango = RangoHistorial.HOY))

        val s = estado()

        assertEquals(listOf(5), s.alertas.map { it.id })
        assertEquals(listOf(1), s.eventos.map { it.id })
    }

    @Test fun tiposDisponiblesSonLosDistintosDelHistorial() = runTest {
        val s = estado()

        assertEquals(listOf("TEMPERATURA_ALTA", "HUMEDAD_ALTA"), s.tiposDisponibles)
    }
}
