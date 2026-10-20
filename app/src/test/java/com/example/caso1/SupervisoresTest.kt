package com.example.caso1

import com.example.caso1.data.model.Supervisores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Respuesta 8 del docente: cada galpón tiene un supervisor ficticio con nombre y teléfono.
 * El mapeo es estable (no aleatorio): la misma demo muestra siempre la misma persona.
 */
class SupervisoresTest {

    private val galpones = 1..12

    @Test fun cadaGalponTieneNombreYTelefono() {
        galpones.forEach { id ->
            val contacto = Supervisores.de(id)
            assertTrue("galpón $id sin nombre", contacto.nombre.isNotBlank())
            assertTrue("galpón $id sin teléfono", contacto.telefono.isNotBlank())
        }
    }

    @Test fun elMismoGalponSiempreDevuelveLaMismaPersona() {
        galpones.forEach { id ->
            assertEquals(Supervisores.de(id), Supervisores.de(id))
        }
    }

    @Test fun losDatosSonLosMismosEnCadaLlamadaSinAleatoriedad() {
        val primera = galpones.map { Supervisores.de(it) }
        val segunda = galpones.map { Supervisores.de(it) }

        assertEquals(primera, segunda)
    }
}
