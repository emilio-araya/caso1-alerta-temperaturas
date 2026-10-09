package com.example.caso1

import com.example.caso1.data.api.MockMonitorApi
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Umbrales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UmbralesTest {

    @Test fun estadoSegunUmbrales() {
        assertEquals(EstadoGalpon.NORMAL, Umbrales.evaluar(24.0, 60.0))
        assertEquals(EstadoGalpon.ADVERTENCIA, Umbrales.evaluar(30.0, 60.0))
        assertEquals(EstadoGalpon.ADVERTENCIA, Umbrales.evaluar(24.0, 75.0))
        assertEquals(EstadoGalpon.CRITICO, Umbrales.evaluar(33.0, 60.0))
        assertEquals(EstadoGalpon.CRITICO, Umbrales.evaluar(24.0, 85.0))
    }

    @Test fun causaDeLaAlerta() {
        assertNull(Umbrales.causa(24.0, 60.0))
        assertEquals("TEMPERATURA_ALTA", Umbrales.causa(30.0, 60.0))   // antes salía HUMEDAD_ALTA
        assertEquals("TEMPERATURA_BAJA", Umbrales.causa(15.0, 60.0))
        assertEquals("HUMEDAD_ALTA", Umbrales.causa(30.0, 85.0))       // gana la condición crítica
        assertEquals("HUMEDAD_BAJA", Umbrales.causa(24.0, 40.0))
    }

    @Test fun mockCoherenteEntreLlamadas() {
        val galpones = MockMonitorApi.getGalpones()
        val alertas = MockMonitorApi.getAlertasActivas().associateBy { it.galponId }
        galpones.forEach { g ->
            val ultima = MockMonitorApi.getUltimaMedicion(g.id)
            assertEquals(ultima, MockMonitorApi.getMediciones(g.id).first())
            assertEquals(g.estado, Umbrales.evaluar(ultima.temperatura, ultima.humedad))
            // Hay alerta si y solo si el galpón no está normal
            assertEquals(g.estado != EstadoGalpon.NORMAL, alertas.containsKey(g.id))
        }
    }
}
