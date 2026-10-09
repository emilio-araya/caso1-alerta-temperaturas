package com.example.caso1.data.repository

import com.example.caso1.data.api.MockMonitorApi
import com.example.caso1.data.db.*
import kotlinx.coroutines.flow.Flow

class GalponRepository(private val db: AppDatabase) {

    val galpones: Flow<List<GalponEntity>> = db.galponDao().observarTodos()
    val alertasActivas: Flow<List<AlertaEntity>> = db.alertaDao().observarActivas()
    val eventos: Flow<List<EventoEntity>> = db.eventoDao().observarTodos()

    fun mediciones(galponId: Int): Flow<List<MedicionEntity>> =
        db.medicionDao().observarPorGalpon(galponId)

    suspend fun refrescar() {
        db.galponDao().insertarTodos(
            MockMonitorApi.getGalpones().map { GalponEntity(it.id, it.granja, it.nombre, it.estado) }
        )
        MockMonitorApi.getAlertasActivas().forEach { a ->
            db.alertaDao().insertar(AlertaEntity(0, a.galponId, a.tipo, a.nivel.name, a.activa, a.fechaHora))
        }
        MockMonitorApi.getGalpones().forEach { g ->
            MockMonitorApi.getMediciones(g.id).take(5).forEach { m ->
                db.medicionDao().insertar(MedicionEntity(0, m.galponId, m.temperatura, m.humedad, m.fechaHora))
            }
        }
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
