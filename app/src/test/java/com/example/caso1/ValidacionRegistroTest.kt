package com.example.caso1

import com.example.caso1.viewmodel.TipoAccion
import com.example.caso1.viewmodel.validarRegistro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionRegistroTest {

    private val galpones = setOf(1, 2, 3, 4)

    @Test fun formularioCompletoEsValido() {
        val e = validarRegistro(3, galpones, TipoAccion.VENTILACION, "Encendí los ventiladores")
        assertFalse(e.hayErrores)
    }

    @Test fun galponInexistenteSeRechaza() {
        // Antes se podía escribir cualquier número, p. ej. 99
        assertEquals(R.string.registro_error_galpon, validarRegistro(99, galpones, TipoAccion.OTRO, "Encendí ventiladores").galpon)
        assertEquals(R.string.registro_error_galpon, validarRegistro(null, galpones, TipoAccion.OTRO, "Encendí ventiladores").galpon)
    }

    @Test fun tipoEsObligatorio() {
        assertEquals(R.string.registro_error_tipo, validarRegistro(1, galpones, null, "Encendí los ventiladores").tipo)
        assertNull(validarRegistro(1, galpones, TipoAccion.OTRO, "Encendí los ventiladores").tipo)
    }

    @Test fun descripcionMinimoCincoCaracteresSinContarEspacios() {
        assertEquals(R.string.registro_error_descripcion, validarRegistro(1, galpones, TipoAccion.OTRO, "   abc   ").descripcion)
        assertNull(validarRegistro(1, galpones, TipoAccion.OTRO, "abcde").descripcion)
    }

    @Test fun reportaTodosLosErroresALaVez() {
        val e = validarRegistro(null, galpones, null, "")
        assertTrue(e.galpon != null && e.tipo != null && e.descripcion != null)
    }
}
