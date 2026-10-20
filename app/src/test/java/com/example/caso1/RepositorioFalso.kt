package com.example.caso1

import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.EventoEntity
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.local.MedicionEntity
import com.example.caso1.data.model.Rol
import com.example.caso1.data.repository.EstadoConexion
import com.example.caso1.data.repository.RepositorioGalpones
import com.example.caso1.data.repository.ResultadoSincronizacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Repositorio en memoria para los tests: sin DataStore, red ni emulador. Guarda las
 * llamadas que recibe para poder comprobarlas.
 */
class RepositorioFalso(galponesIniciales: List<GalponEntity> = emptyList()) : RepositorioGalpones {

    data class AccionGuardada(val galponId: Int, val tipo: String, val descripcion: String, val usuario: String?)

    val accionesGuardadas = mutableListOf<AccionGuardada>()

    data class MedicionSimulada(val galponId: Int, val temperatura: Double, val humedad: Double)

    val medicionesSimuladas = mutableListOf<MedicionSimulada>()

    /** Lo que devolverá el próximo registrarMedicionSimulada(). */
    var proximasCriticasSimuladas: List<AlertaEntity> = emptyList()

    /** Lo que devolverá el próximo refrescar(). */
    var proximoResultado: ResultadoSincronizacion = ResultadoSincronizacion.Correcto(emptyList())
    var vecesRefrescado = 0

    /** Lo que devolverá el próximo reevaluar(). */
    var proximasCriticasReevaluadas: List<AlertaEntity> = emptyList()
    var vecesReevaluado = 0

    override val galpones = MutableStateFlow(galponesIniciales)
    override val alertasActivas = MutableStateFlow<List<AlertaEntity>>(emptyList())
    override val alertas = MutableStateFlow<List<AlertaEntity>>(emptyList())
    override val eventos = MutableStateFlow<List<EventoEntity>>(emptyList())
    override val ultimasMediciones: Flow<List<MedicionEntity>> = MutableStateFlow(emptyList())
    override val estadoConexion = MutableStateFlow(EstadoConexion.SIN_DATOS)

    override fun mediciones(galponId: Int): Flow<List<MedicionEntity>> = emptyFlow()

    override suspend fun refrescar(): ResultadoSincronizacion {
        vecesRefrescado++
        estadoConexion.value = if (proximoResultado is ResultadoSincronizacion.Correcto) EstadoConexion.EN_LINEA
            else EstadoConexion.SIN_CONEXION
        return proximoResultado
    }

    override suspend fun reevaluar(): List<AlertaEntity> {
        vecesReevaluado++
        return proximasCriticasReevaluadas
    }

    override suspend fun simularPicoDeCalor(): Int? = null

    override suspend fun registrarMedicionSimulada(galponId: Int, temperatura: Double, humedad: Double): List<AlertaEntity> {
        medicionesSimuladas += MedicionSimulada(galponId, temperatura, humedad)
        return proximasCriticasSimuladas
    }

    override suspend fun confirmarAlerta(alertaId: Int, galponId: Int, rol: Rol, detalle: String) = Unit

    override suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String, usuario: String?) {
        accionesGuardadas += AccionGuardada(galponId, tipoAccion, descripcion, usuario)
    }
}
