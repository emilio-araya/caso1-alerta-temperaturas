package com.example.caso1

import com.example.caso1.viewmodel.validarRegistro
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionRegistroTest {

    private val galpones = setOf(1, 2, 3, 4)

    @Test fun formularioCompletoEsValido() {
        val e = validarRegistro(3, galpones, "Ventilación", "Encendí los ventiladores")
        assertFalse(e.hayErrores)
    }

    @Test fun galponInexistenteSeRechaza() {
        // Antes se podía escribir cualquier número, p. ej. 99
        assertNotNull(validarRegistro(99, galpones, "Ventilación", "Encendí los ventiladores").galpon)
        assertNotNull(validarRegistro(null, galpones, "Ventilación", "Encendí los ventiladores").galpon)
    }

    @Test fun tipoDebeSerUnoDeLaLista() {
        assertNotNull(validarRegistro(1, galpones, "", "Encendí los ventiladores").tipo)
        assertNotNull(validarRegistro(1, galpones, "Inventado", "Encendí los ventiladores").tipo)
        assertNull(validarRegistro(1, galpones, "Otro", "Encendí los ventiladores").tipo)
    }

    @Test fun descripcionMinimoCincoCaracteresSinContarEspacios() {
        assertNotNull(validarRegistro(1, galpones, "Otro", "   abc   ").descripcion)
        assertNull(validarRegistro(1, galpones, "Otro", "abcde").descripcion)
    }

    @Test fun reportaTodosLosErroresALaVez() {
        val e = validarRegistro(null, galpones, "", "")
        assertTrue(e.galpon != null && e.tipo != null && e.descripcion != null)
    }
}
