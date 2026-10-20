package com.example.caso1

import com.example.caso1.data.Sincronizador
import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.repository.EstadoConexion
import com.example.caso1.data.repository.ResultadoSincronizacion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizadorTest {

    private val repo = RepositorioFalso()
    private val notificadas = mutableListOf<AlertaEntity>()
    private val sincronizador = Sincronizador(repo) { notificadas += it }

    private val critica = AlertaEntity(1, 4, "TEMPERATURA_ALTA", NivelAlerta.CRITICO, true, 0L)

    @Test fun conConexionNotificaLasAlertasCriticasNuevas() = runTest {
        repo.proximoResultado = ResultadoSincronizacion.Correcto(listOf(critica))

        val r = sincronizador.sincronizar()

        assertTrue(r is ResultadoSincronizacion.Correcto)
        assertEquals(listOf(critica), notificadas)
        assertEquals(EstadoConexion.EN_LINEA, repo.estadoConexion.value)
        assertFalse(sincronizador.sincronizando.value)
    }

    @Test fun sinConexionNoFallaNiNotifica() = runTest {
        repo.proximoResultado = ResultadoSincronizacion.SinConexion

        val r = sincronizador.sincronizar()

        assertEquals(ResultadoSincronizacion.SinConexion, r)
        assertTrue(notificadas.isEmpty())
        assertEquals(EstadoConexion.SIN_CONEXION, repo.estadoConexion.value)
        // El indicador de carga siempre se apaga, aunque falle
        assertFalse(sincronizador.sincronizando.value)
    }
}
