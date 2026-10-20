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

    data class AccionGuardada(val galponId: Int, val tipo: String, val descripcion: String)

    val accionesGuardadas = mutableListOf<AccionGuardada>()

    /** Lo que devolverá el próximo refrescar(). */
    var proximoResultado: ResultadoSincronizacion = ResultadoSincronizacion.Correcto(emptyList())
    var vecesRefrescado = 0

    override val galpones = MutableStateFlow(galponesIniciales)
    override val alertasActivas: Flow<List<AlertaEntity>> = MutableStateFlow(emptyList())
    override val alertas: Flow<List<AlertaEntity>> = MutableStateFlow(emptyList())
    override val eventos: Flow<List<EventoEntity>> = MutableStateFlow(emptyList())
    override val ultimasMediciones: Flow<List<MedicionEntity>> = MutableStateFlow(emptyList())
    override val estadoConexion = MutableStateFlow(EstadoConexion.SIN_DATOS)

    override fun mediciones(galponId: Int): Flow<List<MedicionEntity>> = emptyFlow()

    override suspend fun refrescar(): ResultadoSincronizacion {
        vecesRefrescado++
        estadoConexion.value = if (proximoResultado is ResultadoSincronizacion.Correcto) EstadoConexion.EN_LINEA
            else EstadoConexion.SIN_CONEXION
        return proximoResultado
    }

    override suspend fun simularPicoDeCalor(): Int? = null

    override suspend fun confirmarAlerta(alertaId: Int, galponId: Int, rol: Rol, detalle: String) = Unit

    override suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String) {
        accionesGuardadas += AccionGuardada(galponId, tipoAccion, descripcion)
    }
}
