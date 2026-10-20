package com.example.caso1

import com.example.caso1.data.Sincronizador
import com.example.caso1.data.local.RepositorioUmbrales
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.Umbrales
import com.example.caso1.viewmodel.CampoUmbral
import com.example.caso1.viewmodel.UmbralesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UmbralesViewModelTest {

    @get:Rule val reglaMain = ReglaDispatcherPrincipal()

    /** Umbrales en memoria: guarda lo que recibe para comprobarlo. */
    private class UmbralesFalsos(inicial: ConfigUmbrales) : RepositorioUmbrales {
        override val umbrales = MutableStateFlow(inicial)
        val guardados = mutableListOf<ConfigUmbrales>()
        var restaurados = 0
        override suspend fun guardar(c: ConfigUmbrales) { guardados += c; umbrales.value = c }
        override suspend fun restaurar() { restaurados++; umbrales.value = Umbrales.PREDETERMINADOS }
    }

    private val umbrales = UmbralesFalsos(ConfigUmbrales(tempNormalMax = 25.0, tempAdvertenciaMax = 30.5))
    private val repo = RepositorioFalso()
    // by lazy: el ViewModel se crea después de que la regla reemplace Dispatchers.Main
    private val vm by lazy { UmbralesViewModel(umbrales, Sincronizador(repo) {}) }

    @Test fun cargaLosUmbralesGuardadosUnaSolaVez() = runTest {
        val textos = vm.state.value.textos
        assertEquals("25", textos[CampoUmbral.TEMP_NORMAL_MAX])
        assertEquals("30,5", textos[CampoUmbral.TEMP_ADVERTENCIA_MAX])
        assertFalse(vm.state.value.cargando)

        // Lo que el usuario escribe no se pisa si DataStore vuelve a emitir
        vm.onCambio(CampoUmbral.TEMP_NORMAL_MIN, "1")
        umbrales.umbrales.value = ConfigUmbrales(tempNormalMin = 10.0)
        assertEquals("1", vm.state.value.textos[CampoUmbral.TEMP_NORMAL_MIN])
    }

    @Test fun datosInvalidosNoSeGuardan() = runTest {
        vm.onCambio(CampoUmbral.TEMP_NORMAL_MIN, "")
        vm.onCambio(CampoUmbral.HUMEDAD_ADVERTENCIA_MAX, "120")

        vm.guardar()

        assertTrue(umbrales.guardados.isEmpty())
        assertEquals(0, repo.vecesReevaluado)
        val s = vm.state.value
        assertEquals(
            mapOf(
                CampoUmbral.TEMP_NORMAL_MIN to R.string.umbrales_error_vacio,
                CampoUmbral.HUMEDAD_ADVERTENCIA_MAX to R.string.umbrales_error_rango_humedad
            ),
            s.errores
        )
        assertNull(s.mensaje)
        assertFalse(s.guardando)
        assertEquals("120", s.textos[CampoUmbral.HUMEDAD_ADVERTENCIA_MAX])   // se conserva para corregirlo
    }

    @Test fun datosValidosSeGuardanYSeReevalua() = runTest {
        vm.onCambio(CampoUmbral.TEMP_ADVERTENCIA_MAX, "29,5")

        vm.guardar()

        assertEquals(listOf(ConfigUmbrales(tempNormalMax = 25.0, tempAdvertenciaMax = 29.5)), umbrales.guardados)
        assertEquals(1, repo.vecesReevaluado)
        val s = vm.state.value
        assertTrue(s.errores.isEmpty())
        assertFalse(s.guardando)
        assertEquals(R.string.umbrales_guardados, s.mensaje)
    }

    @Test fun corregirUnCampoBorraSoloSuError() = runTest {
        vm.onCambio(CampoUmbral.TEMP_NORMAL_MIN, "x")
        vm.onCambio(CampoUmbral.HUMEDAD_NORMAL_MIN, "x")
        vm.guardar()

        vm.onCambio(CampoUmbral.TEMP_NORMAL_MIN, "18")

        assertEquals(setOf(CampoUmbral.HUMEDAD_NORMAL_MIN), vm.state.value.errores.keys)
    }

    @Test fun restaurarVuelveALosPredeterminados() = runTest {
        vm.onCambio(CampoUmbral.TEMP_NORMAL_MIN, "x")
        vm.guardar()

        vm.restaurarPredeterminados()

        assertEquals(1, umbrales.restaurados)
        assertEquals(1, repo.vecesReevaluado)
        val s = vm.state.value
        assertEquals("28", s.textos[CampoUmbral.TEMP_NORMAL_MAX])
        assertEquals("18", s.textos[CampoUmbral.TEMP_NORMAL_MIN])
        assertTrue(s.errores.isEmpty())
        assertEquals(R.string.umbrales_restaurados, s.mensaje)
    }
}
