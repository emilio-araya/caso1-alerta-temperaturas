package com.example.caso1

import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.viewmodel.CampoUmbral
import com.example.caso1.viewmodel.CampoUmbral.*
import com.example.caso1.viewmodel.configDe
import com.example.caso1.viewmodel.textosDe
import com.example.caso1.viewmodel.validarUmbrales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionUmbralesTest {

    /** Los predeterminados como texto, con [cambios] aplicados encima. */
    private fun textos(vararg cambios: Pair<CampoUmbral, String>) = textosDe(ConfigUmbrales()) + cambios

    @Test fun valoresValidosNoTienenErrores() {
        assertTrue(validarUmbrales(textos()).isEmpty())
    }

    @Test fun campoVacioEsObligatorio() {
        assertEquals(mapOf(TEMP_NORMAL_MIN to R.string.umbrales_error_vacio), validarUmbrales(textos(TEMP_NORMAL_MIN to "  ")))
    }

    @Test fun textoNoNumericoSeRechaza() {
        assertEquals(mapOf(HUMEDAD_NORMAL_MAX to R.string.umbrales_error_numero), validarUmbrales(textos(HUMEDAD_NORMAL_MAX to "abc")))
    }

    @Test fun aceptaComaDecimal() {
        val t = textos(TEMP_NORMAL_MAX to "28,5")
        assertTrue(validarUmbrales(t).isEmpty())
        assertEquals(28.5, configDe(t).tempNormalMax, 0.0)
    }

    @Test fun temperaturaFueraDeRango() {
        assertEquals(mapOf(TEMP_NORMAL_MIN to R.string.umbrales_error_rango_temperatura), validarUmbrales(textos(TEMP_NORMAL_MIN to "-10,1")))
        assertEquals(mapOf(TEMP_ADVERTENCIA_MAX to R.string.umbrales_error_rango_temperatura), validarUmbrales(textos(TEMP_ADVERTENCIA_MAX to "61")))
        // Bordes incluidos
        assertTrue(validarUmbrales(textos(TEMP_NORMAL_MIN to "-10", TEMP_ADVERTENCIA_MAX to "60")).isEmpty())
    }

    @Test fun humedadFueraDeRango() {
        assertEquals(mapOf(HUMEDAD_NORMAL_MIN to R.string.umbrales_error_rango_humedad), validarUmbrales(textos(HUMEDAD_NORMAL_MIN to "-1")))
        assertEquals(mapOf(HUMEDAD_ADVERTENCIA_MAX to R.string.umbrales_error_rango_humedad), validarUmbrales(textos(HUMEDAD_ADVERTENCIA_MAX to "100,5")))
        assertTrue(validarUmbrales(textos(HUMEDAD_NORMAL_MIN to "0", HUMEDAD_ADVERTENCIA_MAX to "100")).isEmpty())
    }

    @Test fun ordenSeReportaEnElCampoMayor() {
        // Máximo normal igual al mínimo: el error va en el máximo normal
        assertEquals(mapOf(TEMP_NORMAL_MAX to R.string.umbrales_error_mayor_min), validarUmbrales(textos(TEMP_NORMAL_MAX to "18")))
        assertEquals(mapOf(TEMP_ADVERTENCIA_MAX to R.string.umbrales_error_mayor_max), validarUmbrales(textos(TEMP_ADVERTENCIA_MAX to "27")))
        assertEquals(mapOf(HUMEDAD_NORMAL_MAX to R.string.umbrales_error_mayor_min), validarUmbrales(textos(HUMEDAD_NORMAL_MAX to "40")))
        assertEquals(mapOf(HUMEDAD_ADVERTENCIA_MAX to R.string.umbrales_error_mayor_max), validarUmbrales(textos(HUMEDAD_ADVERTENCIA_MAX to "70")))
    }

    @Test fun reportaTodosLosErroresALaVez() {
        val e = validarUmbrales(CampoUmbral.entries.associateWith { "" })
        assertEquals(CampoUmbral.entries.toSet(), e.keys)
    }
}
