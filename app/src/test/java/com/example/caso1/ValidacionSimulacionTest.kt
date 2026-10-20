package com.example.caso1

import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.viewmodel.CampoSimulacion.*
import com.example.caso1.viewmodel.estadoSimulado
import com.example.caso1.viewmodel.validarSimulacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionSimulacionTest {

    private val existentes = setOf(1, 4)

    @Test fun valoresValidosNoTienenErrores() {
        assertTrue(validarSimulacion(1, existentes, "35", "60").isEmpty())
        // Coma o punto decimal
        assertTrue(validarSimulacion(4, existentes, "34,5", "62.5").isEmpty())
    }

    @Test fun galponObligatorioYExistente() {
        assertEquals(mapOf(GALPON to R.string.simulacion_error_galpon), validarSimulacion(null, existentes, "30", "60"))
        assertEquals(mapOf(GALPON to R.string.simulacion_error_galpon), validarSimulacion(99, existentes, "30", "60"))
    }

    @Test fun numerosObligatorios() {
        assertEquals(
            mapOf(TEMPERATURA to R.string.umbrales_error_vacio, HUMEDAD to R.string.umbrales_error_numero),
            validarSimulacion(1, existentes, " ", "abc")
        )
    }

    @Test fun temperaturaEntreMenos10Y60() {
        assertEquals(mapOf(TEMPERATURA to R.string.umbrales_error_rango_temperatura), validarSimulacion(1, existentes, "-10,1", "60"))
        assertEquals(mapOf(TEMPERATURA to R.string.umbrales_error_rango_temperatura), validarSimulacion(1, existentes, "60.1", "60"))
        // Bordes incluidos
        assertTrue(validarSimulacion(1, existentes, "-10", "60").isEmpty())
        assertTrue(validarSimulacion(1, existentes, "60", "60").isEmpty())
    }

    @Test fun humedadEntre0Y100() {
        assertEquals(mapOf(HUMEDAD to R.string.umbrales_error_rango_humedad), validarSimulacion(1, existentes, "25", "-1"))
        assertEquals(mapOf(HUMEDAD to R.string.umbrales_error_rango_humedad), validarSimulacion(1, existentes, "25", "100,5"))
        assertTrue(validarSimulacion(1, existentes, "25", "0").isEmpty())
        assertTrue(validarSimulacion(1, existentes, "25", "100").isEmpty())
    }

    @Test fun vistaPreviaConLosUmbralesActuales() {
        val u = ConfigUmbrales()
        assertEquals(EstadoGalpon.NORMAL, estadoSimulado("24", "60", u))
        assertEquals(EstadoGalpon.ADVERTENCIA, estadoSimulado("30", "60", u))
        assertEquals(EstadoGalpon.CRITICO, estadoSimulado("35", "60", u))
        assertEquals(EstadoGalpon.ADVERTENCIA, estadoSimulado("30", "60", ConfigUmbrales(tempAdvertenciaMax = 31.0)))
        assertEquals(EstadoGalpon.CRITICO, estadoSimulado("30", "60", ConfigUmbrales(tempAdvertenciaMax = 29.0)))
        // Sin vista previa mientras falte un número válido
        assertNull(estadoSimulado("", "60", u))
        assertNull(estadoSimulado("35", "120", u))
    }
}
