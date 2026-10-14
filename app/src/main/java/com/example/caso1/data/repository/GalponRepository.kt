package com.example.caso1.data.repository

import androidx.room.withTransaction
import com.example.caso1.data.api.AlertaDto
import com.example.caso1.data.api.GalponDto
import com.example.caso1.data.api.MedicionDto
import com.example.caso1.data.api.MonitorApi
import com.example.caso1.data.db.*
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.model.Rol
import com.example.caso1.data.model.Umbrales
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerializationException
import android.util.Log
import retrofit2.HttpException
import java.io.IOException

/**
 * Fuente única de verdad: la UI siempre lee de Room. La API solo se usa para
 * actualizar Room; si no hay conexión, la app sigue mostrando lo último guardado.
 */
class GalponRepository(
    private val db: AppDatabase,
    private val api: MonitorApi,
    private val reloj: () -> Long = System::currentTimeMillis
) : RepositorioGalpones {

    override val galpones: Flow<List<GalponEntity>> = db.galponDao().observarTodos()
    override val alertasActivas: Flow<List<AlertaEntity>> = db.alertaDao().observarActivas()
    override val alertas: Flow<List<AlertaEntity>> = db.alertaDao().observarTodas()
    override val eventos: Flow<List<EventoEntity>> = db.eventoDao().observarTodos()
    override val ultimasMediciones: Flow<List<MedicionEntity>> = db.medicionDao().observarUltimas()

    private val _estadoConexion = MutableStateFlow(EstadoConexion.SIN_DATOS)
    override val estadoConexion: StateFlow<EstadoConexion> = _estadoConexion

    override fun mediciones(galponId: Int): Flow<List<MedicionEntity>> =
        db.medicionDao().observarPorGalpon(galponId)

    override suspend fun refrescar(): ResultadoSincronizacion {
        // 1) Descargar todo. Si algo falla aquí, Room no se toca.
        val descarga = try {
            descargar()
        } catch (e: IOException) {          // sin red, tiempo de espera agotado
            Log.w(TAG, "Sync failed: no connection", e)
            null
        } catch (e: HttpException) {        // el servidor respondió 4xx/5xx
            Log.w(TAG, "Sync failed: HTTP ${e.code()}", e)
            null
        } catch (e: SerializationException) { // JSON con un formato inesperado
            Log.w(TAG, "Sync failed: unexpected JSON", e)
            null
        }
        if (descarga == null) {
            _estadoConexion.value = EstadoConexion.SIN_CONEXION
            return ResultadoSincronizacion.SinConexion
        }
        // 2) Guardar todo de una vez (transacción): nunca quedan datos a medias.
        val nuevasCriticas = guardar(descarga)
        _estadoConexion.value = EstadoConexion.EN_LINEA
        return ResultadoSincronizacion.Correcto(nuevasCriticas)
    }

    private class Descarga(
        val galpones: List<GalponDto>,
        val mediciones: List<MedicionDto>,
        val alertas: List<AlertaDto>
    )

    private suspend fun descargar(): Descarga = coroutineScope {
        val galpones = api.galpones()
        // Las mediciones de cada galpón se piden en paralelo
        val mediciones = galpones.map { g -> async { api.mediciones(g.id) } }.awaitAll().flatten()
        Descarga(galpones, mediciones, api.alertasActivas())
    }

    /**
     * Devuelve las alertas CRÍTICAS que aparecieron en esta sincronización (las que ya
     * estaban activas no se repiten), para notificar solo lo nuevo.
     */
    private suspend fun guardar(d: Descarga): List<AlertaEntity> = db.withTransaction {
        val ultimaPorGalpon = d.mediciones.groupBy { it.galponId }.mapValues { (_, l) -> l.maxBy { it.fechaHora } }
        db.galponDao().insertarTodos(d.galpones.map { g ->
            // Si el servidor manda un estado desconocido, se calcula con los umbrales
            val estado = runCatching { EstadoGalpon.valueOf(g.estado) }.getOrNull()
                ?: ultimaPorGalpon[g.id]?.let { Umbrales.evaluar(it.temperatura, it.humedad) }
                ?: EstadoGalpon.NORMAL
            GalponEntity(g.id, g.granja, g.nombre, estado)
        })
        db.medicionDao().insertarTodas(d.mediciones.map {
            MedicionEntity(galponId = it.galponId, temperatura = it.temperatura, humedad = it.humedad, fechaHora = it.fechaHora)
        })
        db.medicionDao().borrarAnterioresA(reloj() - RETENCION_MEDICIONES_MS)

        // Una alerta activa por galpón: se mantiene si sigue igual, se reemplaza si cambió
        // de tipo/nivel y se desactiva si el galpón volvió a la normalidad.
        val alertasApi = d.alertas
            .mapNotNull { a -> runCatching { NivelAlerta.valueOf(a.nivel) }.getOrNull()?.let { a to it } }
            .associateBy { (a, _) -> a.galponId }
        val nuevasCriticas = mutableListOf<AlertaEntity>()
        d.galpones.forEach { g ->
            val actual = db.alertaDao().activaDeGalpon(g.id)
            val nueva = alertasApi[g.id]
            val sinCambios = actual != null && nueva != null &&
                actual.tipo == nueva.first.tipo && actual.nivel == nueva.second
            if (sinCambios) return@forEach

            actual?.let { db.alertaDao().desactivar(it.id) }
            if (nueva != null) {
                val (dto, nivel) = nueva
                val entidad = AlertaEntity(0, dto.galponId, dto.tipo, nivel, true, dto.fechaHora)
                val id = db.alertaDao().insertar(entidad).toInt()
                if (nivel == NivelAlerta.CRITICO) nuevasCriticas += entidad.copy(id = id)
            }
        }
        nuevasCriticas
    }

    override suspend fun simularPicoDeCalor(): Int? = try {
        api.simularPicoDeCalor().galponId
    } catch (e: IOException) {
        Log.w(TAG, "Heat peak simulation failed: no connection", e)
        _estadoConexion.value = EstadoConexion.SIN_CONEXION
        null
    } catch (e: HttpException) {
        Log.w(TAG, "Heat peak simulation failed: HTTP ${e.code()}", e)
        null
    }

    /**
     * Confirma (acusa recibo de) una alerta y deja constancia en el historial de acciones.
     * Es local: funciona sin conexión. [detalle] es el texto legible, armado por la UI.
     */
    override suspend fun confirmarAlerta(alertaId: Int, galponId: Int, rol: Rol, detalle: String) = db.withTransaction {
        val ahora = reloj()
        db.alertaDao().confirmar(alertaId, rol.name, ahora)
        db.eventoDao().insertar(
            EventoEntity(galponId = galponId, descripcion = detalle, accionRegistrada = ACCION_CONFIRMAR, fechaHora = ahora)
        )
    }

    /** Local: el operario puede registrar acciones aunque esté sin conexión dentro del galpón. */
    override suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String) {
        db.eventoDao().insertar(
            EventoEntity(galponId = galponId, descripcion = descripcion, accionRegistrada = tipoAccion, fechaHora = reloj())
        )
    }
}

/** Código guardado en eventos.accionRegistrada; el texto visible está en strings.xml. */
const val ACCION_CONFIRMAR = "CONFIRMACION_ALERTA"

/** Las mediciones se guardan 24 h (96 lecturas por galpón, una cada 15 min). */
const val RETENCION_MEDICIONES_MS = 24 * 60 * 60_000L

private const val TAG = "GalponRepository"
