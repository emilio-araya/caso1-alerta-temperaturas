package com.example.caso1.data.repository

import com.example.caso1.data.api.MockMonitorApi
import androidx.room.withTransaction
import com.example.caso1.data.db.*
import com.example.caso1.data.model.NivelAlerta
import kotlinx.coroutines.flow.Flow

class GalponRepository(private val db: AppDatabase) {

    val galpones: Flow<List<GalponEntity>> = db.galponDao().observarTodos()
    val alertasActivas: Flow<List<AlertaEntity>> = db.alertaDao().observarActivas()
    val alertas: Flow<List<AlertaEntity>> = db.alertaDao().observarTodas()
    val eventos: Flow<List<EventoEntity>> = db.eventoDao().observarTodos()

    val ultimasMediciones: Flow<List<MedicionEntity>> = db.medicionDao().observarUltimas()

    fun mediciones(galponId: Int): Flow<List<MedicionEntity>> =
        db.medicionDao().observarPorGalpon(galponId)

    /**
     * Sincroniza Room con la API. Devuelve las alertas CRÍTICAS que aparecieron
     * en este refresco (las que ya estaban activas no se repiten), para notificar
     * solo lo nuevo.
     */
    suspend fun refrescar(): List<AlertaEntity> = db.withTransaction {
        val galpones = MockMonitorApi.getGalpones()
        db.galponDao().insertarTodos(galpones.map { GalponEntity(it.id, it.granja, it.nombre, it.estado) })

        galpones.forEach { g ->
            db.medicionDao().insertarTodas(
                MockMonitorApi.getMediciones(g.id).map {
                    MedicionEntity(galponId = it.galponId, temperatura = it.temperatura, humedad = it.humedad, fechaHora = it.fechaHora)
                }
            )
        }

        // Una alerta activa por galpón: se mantiene si sigue igual, se reemplaza si cambió
        // de tipo/nivel y se desactiva si el galpón volvió a la normalidad.
        val alertasApi = MockMonitorApi.getAlertasActivas().associateBy { it.galponId }
        val nuevasCriticas = mutableListOf<AlertaEntity>()
        galpones.forEach { g ->
            val actual = db.alertaDao().activaDeGalpon(g.id)
            val nueva = alertasApi[g.id]
            val sinCambios = actual != null && nueva != null &&
                actual.tipo == nueva.tipo && actual.nivel == nueva.nivel.name
            if (sinCambios) return@forEach

            actual?.let { db.alertaDao().desactivar(it.id) }
            if (nueva != null) {
                val entidad = AlertaEntity(0, nueva.galponId, nueva.tipo, nueva.nivel.name, true, nueva.fechaHora)
                val id = db.alertaDao().insertar(entidad).toInt()
                if (nueva.nivel == NivelAlerta.CRITICO) nuevasCriticas += entidad.copy(id = id)
            }
        }
        nuevasCriticas
    }

    suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String) {
        db.eventoDao().insertar(
            EventoEntity(
                galponId = galponId,
                descripcion = "[$tipoAccion] $descripcion",
                accionRegistrada = tipoAccion,
                fechaHora = System.currentTimeMillis()
            )
        )
    }
}
