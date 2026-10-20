package com.example.caso1

import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.notifications.debeNotificar
import com.example.caso1.ui.screen.accionRecomendadaRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Respuestas 2 y 4 del docente: a quién se notifica y qué se recomienda hacer según el estado. */
class DestinatariosYAccionRecomendadaTest {

    @Test fun seNotificaSoloAOperarioYSupervisor() {
        assertTrue(debeNotificar(Rol.OPERARIO))
        assertTrue(debeNotificar(Rol.SUPERVISOR))
        // La jefatura consulta el panel general; sin sesión no hay destinatario
        assertFalse(debeNotificar(Rol.JEFATURA))
        assertFalse(debeNotificar(null))
    }

    @Test fun accionRecomendadaSegunElEstado() {
        assertNull(EstadoGalpon.NORMAL.accionRecomendadaRes())
        assertEquals(R.string.accion_recomendada_advertencia, EstadoGalpon.ADVERTENCIA.accionRecomendadaRes())
        assertEquals(R.string.accion_recomendada_critico, EstadoGalpon.CRITICO.accionRecomendadaRes())
    }
}
