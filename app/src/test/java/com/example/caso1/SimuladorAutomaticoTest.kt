package com.example.caso1

import com.example.caso1.data.debeSimular
import com.example.caso1.data.model.Rol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La simulación automática solo corre con una sesión iniciada. */
class SimuladorAutomaticoTest {

    @Test fun conCualquierRolIniciadoSimula() {
        Rol.entries.forEach { assertTrue(debeSimular(it)) }
    }

    @Test fun sinSesionNoSimula() {
        assertFalse(debeSimular(null))
    }
}
