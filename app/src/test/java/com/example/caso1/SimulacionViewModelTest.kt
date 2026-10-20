package com.example.caso1

import com.example.caso1.data.Sincronizador
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.viewmodel.CampoSimulacion
import com.example.caso1.viewmodel.SimulacionViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SimulacionViewModelTest {

    @get:Rule val reglaMain = ReglaDispatcherPrincipal()

    private val repo = RepositorioFalso(listOf(GalponEntity(1, "Granja Norte", "Galpón 1", EstadoGalpon.NORMAL)))
    private val umbrales = MutableStateFlow(ConfigUmbrales())
    // by lazy: el ViewModel se crea después de que la regla reemplace Dispatchers.Main
    private val vm by lazy { SimulacionViewModel(repo, Sincronizador(repo) {}, umbrales) }

    @Test fun datosInvalidosNoSimulan() = runTest {
        vm.onTemperaturaChange("70")

        vm.simular()

        assertTrue(repo.medicionesSimuladas.isEmpty())
        val s = vm.state.value
        assertEquals(
            mapOf(
                CampoSimulacion.GALPON to R.string.simulacion_error_galpon,
                CampoSimulacion.TEMPERATURA to R.string.umbrales_error_rango_temperatura,
                CampoSimulacion.HUMEDAD to R.string.umbrales_error_vacio
            ),
            s.errores
        )
        assertNull(s.resultado)
        assertFalse(s.guardando)
        assertEquals("70", s.temperatura)   // se conserva para corregirlo
    }

    @Test fun datosValidosSimulanConLosValoresLeidos() = runTest {
        vm.onGalponChange(1)
        vm.onTemperaturaChange("35,5")
        vm.onHumedadChange("61")

        vm.simular()

        assertEquals(listOf(RepositorioFalso.MedicionSimulada(1, 35.5, 61.0)), repo.medicionesSimuladas)
        val s = vm.state.value
        assertTrue(s.errores.isEmpty())
        assertFalse(s.guardando)
        assertEquals(EstadoGalpon.CRITICO, s.resultado)
    }

    @Test fun editarUnCampoBorraSoloSuError() = runTest {
        vm.simular()
        assertEquals(CampoSimulacion.entries.toSet(), vm.state.value.errores.keys)

        vm.onTemperaturaChange("30")
        assertEquals(setOf(CampoSimulacion.GALPON, CampoSimulacion.HUMEDAD), vm.state.value.errores.keys)
        vm.onGalponChange(1)
        assertEquals(setOf(CampoSimulacion.HUMEDAD), vm.state.value.errores.keys)
        vm.onHumedadChange("60")
        assertTrue(vm.state.value.errores.isEmpty())
    }

    @Test fun vistaPreviaUsaLosUmbralesGuardados() = runTest {
        vm.onTemperaturaChange("30")
        vm.onHumedadChange("60")
        assertEquals(EstadoGalpon.ADVERTENCIA, vm.state.value.vistaPrevia)

        umbrales.value = ConfigUmbrales(tempAdvertenciaMax = 29.0)

        assertEquals(EstadoGalpon.CRITICO, vm.state.value.vistaPrevia)
        assertEquals("30", vm.state.value.temperatura)   // DataStore no pisa lo escrito
    }
}
