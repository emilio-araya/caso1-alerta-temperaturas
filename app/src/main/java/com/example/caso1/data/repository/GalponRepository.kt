package com.example.caso1.data.repository

import com.example.caso1.data.api.MockMonitorApi
import androidx.room.withTransaction
import com.example.caso1.data.db.*
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.model.Rol
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
        db.medicionDao().borrarAnterioresA(System.currentTimeMillis() - RETENCION_MEDICIONES_MS)

        // Una alerta activa por galpón: se mantiene si sigue igual, se reemplaza si cambió
        // de tipo/nivel y se desactiva si el galpón volvió a la normalidad.
        val alertasApi = MockMonitorApi.getAlertasActivas().associateBy { it.galponId }
        val nuevasCriticas = mutableListOf<AlertaEntity>()
        galpones.forEach { g ->
            val actual = db.alertaDao().activaDeGalpon(g.id)
            val nueva = alertasApi[g.id]
            val sinCambios = actual != null && nueva != null &&
                actual.tipo == nueva.tipo && actual.nivel == nueva.nivel
            if (sinCambios) return@forEach

            actual?.let { db.alertaDao().desactivar(it.id) }
            if (nueva != null) {
                val entidad = AlertaEntity(0, nueva.galponId, nueva.tipo, nueva.nivel, true, nueva.fechaHora)
                val id = db.alertaDao().insertar(entidad).toInt()
                if (nueva.nivel == NivelAlerta.CRITICO) nuevasCriticas += entidad.copy(id = id)
            }
        }
        nuevasCriticas
    }

    /**
     * Confirma (acusa recibo de) una alerta y deja constancia en el historial de acciones.
     * [detalle] es el texto legible de la alerta, armado por la UI.
     */
    suspend fun confirmarAlerta(alerta: AlertaEntity, rol: Rol, detalle: String) = db.withTransaction {
        val ahora = System.currentTimeMillis()
        db.alertaDao().confirmar(alerta.id, rol.name, ahora)
        db.eventoDao().insertar(
            EventoEntity(
                galponId = alerta.galponId,
                descripcion = "[$ACCION_CONFIRMAR] $detalle",
                accionRegistrada = ACCION_CONFIRMAR,
                fechaHora = ahora
            )
        )
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

const val ACCION_CONFIRMAR = "Confirmación de alerta"

/** Las mediciones se guardan 24 h (96 lecturas por galpón, una cada 15 min). */
const val RETENCION_MEDICIONES_MS = 24 * 60 * 60_000L
