package com.example.caso1

import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.EventoEntity
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.Rol
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Repositorio en memoria para los tests: sin Room ni emulador. Guarda las
 * llamadas que recibe para poder comprobarlas.
 */
class RepositorioFalso(galponesIniciales: List<GalponEntity>) : RepositorioGalpones {

    data class AccionGuardada(val galponId: Int, val tipo: String, val descripcion: String)

    val accionesGuardadas = mutableListOf<AccionGuardada>()

    override val galpones = MutableStateFlow(galponesIniciales)
    override val alertasActivas: Flow<List<AlertaEntity>> = MutableStateFlow(emptyList())
    override val alertas: Flow<List<AlertaEntity>> = MutableStateFlow(emptyList())
    override val eventos: Flow<List<EventoEntity>> = MutableStateFlow(emptyList())
    override val ultimasMediciones: Flow<List<MedicionEntity>> = MutableStateFlow(emptyList())

    override fun mediciones(galponId: Int): Flow<List<MedicionEntity>> = emptyFlow()

    override suspend fun refrescar(): List<AlertaEntity> = emptyList()

    override suspend fun confirmarAlerta(alertaId: Int, galponId: Int, rol: Rol, detalle: String) = Unit

    override suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String) {
        accionesGuardadas += AccionGuardada(galponId, tipoAccion, descripcion)
    }
}
