package com.example.caso1.data.repository

import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.EventoEntity
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.Rol
import kotlinx.coroutines.flow.Flow

/**
 * Lo que los ViewModels necesitan de los datos. Dependen de esta interfaz y no de
 * Room: en los tests se reemplaza por una implementación falsa en memoria.
 */
interface RepositorioGalpones {
    val galpones: Flow<List<GalponEntity>>
    val alertasActivas: Flow<List<AlertaEntity>>
    val alertas: Flow<List<AlertaEntity>>
    val eventos: Flow<List<EventoEntity>>
    val ultimasMediciones: Flow<List<MedicionEntity>>

    fun mediciones(galponId: Int): Flow<List<MedicionEntity>>

    /** Sincroniza con la API y devuelve las alertas críticas nuevas. */
    suspend fun refrescar(): List<AlertaEntity>

    suspend fun confirmarAlerta(alertaId: Int, galponId: Int, rol: Rol, detalle: String)

    suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String)
}
