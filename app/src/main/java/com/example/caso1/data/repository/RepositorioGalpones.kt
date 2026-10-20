package com.example.caso1.data.repository

import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.EventoEntity
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.local.MedicionEntity
import com.example.caso1.data.model.Rol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Estado de la última comunicación con el sistema de monitoreo. */
enum class EstadoConexion { SIN_DATOS, EN_LINEA, SIN_CONEXION }

/** Resultado de sincronizar con la API. */
sealed interface ResultadoSincronizacion {
    /** [nuevasCriticas]: alertas críticas que aparecieron en esta sincronización (para notificar). */
    data class Correcto(val nuevasCriticas: List<AlertaEntity>) : ResultadoSincronizacion
    /** No se pudo contactar el servidor: la app sigue mostrando lo guardado en DataStore. */
    data object SinConexion : ResultadoSincronizacion
}

/**
 * Lo que los ViewModels necesitan de los datos. Dependen de esta interfaz y no de
 * DataStore ni de Retrofit: en los tests se reemplaza por una implementación falsa en memoria.
 */
interface RepositorioGalpones {
    val galpones: Flow<List<GalponEntity>>
    val alertasActivas: Flow<List<AlertaEntity>>
    val alertas: Flow<List<AlertaEntity>>
    val eventos: Flow<List<EventoEntity>>
    val ultimasMediciones: Flow<List<MedicionEntity>>
    val estadoConexion: StateFlow<EstadoConexion>

    fun mediciones(galponId: Int): Flow<List<MedicionEntity>>

    /** Descarga de la API y guarda en DataStore. Nunca lanza por falta de red. */
    suspend fun refrescar(): ResultadoSincronizacion

    /** Recalcula estados y alertas con los umbrales actuales, sin red. Devuelve las críticas nuevas. */
    suspend fun reevaluar(): List<AlertaEntity>

    /** Modo demo: pide al servidor un pico de calor. Devuelve el galpón afectado, o null sin conexión. */
    suspend fun simularPicoDeCalor(): Int?

    /**
     * Simulación manual (respuesta 5 del docente): guarda una medición ficticia en DataStore,
     * sin red, y recalcula el estado del galpón. Devuelve las críticas nuevas.
     * Lanza IllegalArgumentException si el galpón no existe.
     */
    suspend fun registrarMedicionSimulada(galponId: Int, temperatura: Double, humedad: Double): List<AlertaEntity>

    suspend fun confirmarAlerta(alertaId: Int, galponId: Int, rol: Rol, detalle: String)

    suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String, usuario: String?)
}
