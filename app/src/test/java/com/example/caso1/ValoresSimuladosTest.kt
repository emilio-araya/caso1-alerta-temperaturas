package com.example.caso1

import com.example.caso1.data.model.valoresSimulados
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.round

/**
 * Respuesta 10 del docente: la simulación automática genera una lectura por minuto y por galpón.
 * [valoresSimulados] es pura y determinista: la misma entrada siempre da la misma salida.
 */
class ValoresSimuladosTest {

    private val tempBase = 24.0
    private val humedadBase = 60.0

    @Test fun mismosArgumentosMismosValores() {
        val primera = valoresSimulados(3, 12_345L, tempBase, humedadBase)
        val segunda = valoresSimulados(3, 12_345L, tempBase, humedadBase)

        assertEquals(primera, segunda)
    }

    @Test fun respetaLosLimitesFisicos() {
        for (galpon in 1..5) {
            for (minuto in 0L..180L) {
                // Bases pegadas a los límites: el redondeo y el recorte no deben salirse del rango
                val (temperatura, humedad) = valoresSimulados(galpon, minuto, 12.0, 25.0)

                assertTrue("temperatura $temperatura", temperatura in 12.0..40.0)
                assertTrue("humedad $humedad", humedad in 25.0..95.0)
            }
        }
    }

    @Test fun variaConElMinutoYConElGalpon() {
        val enCero = valoresSimulados(1, 0L, tempBase, humedadBase)
        val quinceMinutosDespues = valoresSimulados(1, 15L, tempBase, humedadBase)
        val otroGalpon = valoresSimulados(2, 0L, tempBase, humedadBase)

        assertNotEquals(enCero.first, quinceMinutosDespues.first)
        assertNotEquals(enCero, otroGalpon)
    }

    @Test fun noSeQuedaPegadoAlMaximoSiParteDeUnaLecturaCritica() {
        // Sin reversión, cada lectura parte de la anterior: en la media hora en que el seno sube,
        // la simulación se arrastra hasta 40 °C / 95 % y las tarjetas quedan pegadas al máximo.
        var temperatura = 33.0
        var humedad = 70.0
        for (minuto in 0L until 15L) {
            val (t, h) = valoresSimulados(1, minuto, temperatura, humedad)
            temperatura = t
            humedad = h
        }

        assertTrue("temperatura $temperatura", temperatura <= 36.5)
        assertTrue("humedad $humedad", humedad <= 85.0)
    }

    @Test fun seMantieneCercaDeLaBaseYRedondeaComoSeMuestra() {
        for (minuto in 0L..180L) {
            val (temperatura, humedad) = valoresSimulados(2, minuto, tempBase, humedadBase)

            assertTrue(abs(temperatura - tempBase) <= 2.05)
            assertTrue(abs(humedad - humedadBase) <= 6.05)
            assertEquals(temperatura, round(temperatura * 10) / 10.0, 0.0)   // 1 decimal
            assertEquals(humedad, round(humedad).toDouble(), 0.0)             // entero
        }
    }
}
