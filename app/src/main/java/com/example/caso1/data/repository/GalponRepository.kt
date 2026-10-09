package com.example.caso1.data.repository

import com.example.caso1.data.api.MockMonitorApi
import androidx.room.withTransaction
import com.example.caso1.data.db.*
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.model.Rol
import kotlinx.coroutines.flow.Flow

class GalponRepository(private val db: AppDatabase) : RepositorioGalpones {

    override val galpones: Flow<List<GalponEntity>> = db.galponDao().observarTodos()
    override val alertasActivas: Flow<List<AlertaEntity>> = db.alertaDao().observarActivas()
    override val alertas: Flow<List<AlertaEntity>> = db.alertaDao().observarTodas()
    override val eventos: Flow<List<EventoEntity>> = db.eventoDao().observarTodos()

    override val ultimasMediciones: Flow<List<MedicionEntity>> = db.medicionDao().observarUltimas()

    override fun mediciones(galponId: Int): Flow<List<MedicionEntity>> =
        db.medicionDao().observarPorGalpon(galponId)

    /**
     * Sincroniza Room con la API. Devuelve las alertas CRÍTICAS que aparecieron
     * en este refresco (las que ya estaban activas no se repiten), para notificar
     * solo lo nuevo.
     */
    override suspend fun refrescar(): List<AlertaEntity> = db.withTransaction {
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
    override suspend fun confirmarAlerta(alertaId: Int, galponId: Int, rol: Rol, detalle: String) = db.withTransaction {
        val ahora = System.currentTimeMillis()
        db.alertaDao().confirmar(alertaId, rol.name, ahora)
        db.eventoDao().insertar(
            EventoEntity(
                galponId = galponId,
                descripcion = detalle,
                accionRegistrada = ACCION_CONFIRMAR,
                fechaHora = ahora
            )
        )
    }

    override suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String) {
        db.eventoDao().insertar(
            EventoEntity(
                galponId = galponId,
                descripcion = descripcion,
                accionRegistrada = tipoAccion,
                fechaHora = System.currentTimeMillis()
            )
        )
    }
}

/** Código guardado en eventos.accionRegistrada; el texto visible está en strings.xml. */
const val ACCION_CONFIRMAR = "CONFIRMACION_ALERTA"

/** Las mediciones se guardan 24 h (96 lecturas por galpón, una cada 15 min). */
const val RETENCION_MEDICIONES_MS = 24 * 60 * 60_000L
