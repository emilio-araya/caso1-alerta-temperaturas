package com.example.caso1

import com.example.caso1.data.api.ClienteApi
import com.example.caso1.data.api.MockMonitorApi
import com.example.caso1.data.api.ServidorSimulado
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

/**
 * Prueba la capa de red de punta a punta en la JVM: Retrofit arma la petición
 * HTTP, el servidor simulado responde JSON y Retrofit lo convierte en DTO.
 */
class ApiMonitoreoTest {

    private var hayRed = true
    private val api = ClienteApi.crear(ServidorSimulado(hayRed = { hayRed }, latenciaMs = 0))

    @Test fun galponesLleganComoJsonYCoincidenConElServidor() = runTest {
        val galpones = api.galpones()
        val esperados = MockMonitorApi.getGalpones()
        assertEquals(esperados.map { it.id }, galpones.map { it.id })
        assertEquals(esperados.map { it.estado.name }, galpones.map { it.estado })
        assertTrue(galpones.all { it.granja.isNotBlank() && it.nombre.isNotBlank() })
    }

    @Test fun medicionesDeUnGalpon() = runTest {
        val mediciones = api.mediciones(1)
        assertEquals(MockMonitorApi.getMediciones(1).size, mediciones.size)
        assertTrue(mediciones.all { it.galponId == 1 })
    }

    @Test fun alertasSoloDeGalponesFueraDeRango() = runTest {
        val fueraDeRango = api.galpones().filter { it.estado != "NORMAL" }.map { it.id }.toSet()
        assertEquals(fueraDeRango, api.alertasActivas().map { it.galponId }.toSet())
    }

    @Test fun galponInexistenteResponde404() = runTest {
        try {
            api.mediciones(99)
            fail("Se esperaba HTTP 404")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    @Test fun sinRedFallaComoUnaRedReal() = runTest {
        hayRed = false
        try {
            api.galpones()
            fail("Se esperaba un error de red")
        } catch (e: IOException) {
            assertFalse(e.message.isNullOrBlank())
        }
    }
}
