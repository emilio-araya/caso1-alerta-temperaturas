package com.example.caso1.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import com.example.caso1.data.api.AlertaDto
import com.example.caso1.data.api.GalponDto
import com.example.caso1.data.api.MedicionDto
import com.example.caso1.data.api.MonitorApi
import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.DatosMonitoreo
import com.example.caso1.data.local.EventoEntity
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.local.MedicionEntity
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Fuente única de verdad: la UI siempre lee de DataStore. La API solo se usa para
 * actualizar DataStore; si no hay conexión, la app sigue mostrando lo último guardado.
 */
class GalponRepository(
    private val store: DataStore<DatosMonitoreo>,
    private val api: MonitorApi,
    private val reloj: () -> Long = System::currentTimeMillis
) : RepositorioGalpones {

    /** Cada vista del documento solo emite cuando su parte cambió. */
    private fun <T> vista(selector: (DatosMonitoreo) -> T): Flow<T> =
        store.data.map(selector).distinctUntilChanged()

    override val galpones: Flow<List<GalponEntity>> = vista { it.galpones }
    override val alertasActivas: Flow<List<AlertaEntity>> =
        vista { d -> d.alertas.filter { it.activa }.sortedByDescending { it.fechaHora } }
    override val alertas: Flow<List<AlertaEntity>> =
        vista { d -> d.alertas.sortedWith(compareByDescending<AlertaEntity> { it.fechaHora }.thenByDescending { it.id }) }
    override val eventos: Flow<List<EventoEntity>> = vista { d -> d.eventos.sortedByDescending { it.fechaHora } }

    /** La medición más reciente de cada galpón (para mostrarla en el listado). */
    override val ultimasMediciones: Flow<List<MedicionEntity>> = vista { d ->
        d.mediciones.groupBy { it.galponId }.values.map { l -> l.maxBy { it.fechaHora } }
    }

    private val _estadoConexion = MutableStateFlow(EstadoConexion.SIN_DATOS)
    override val estadoConexion: StateFlow<EstadoConexion> = _estadoConexion

    override fun mediciones(galponId: Int): Flow<List<MedicionEntity>> =
        vista { d -> d.mediciones.filter { it.galponId == galponId }.sortedByDescending { it.fechaHora } }

    override suspend fun refrescar(): ResultadoSincronizacion {
        // 1) Descargar todo. Si algo falla aquí, DataStore no se toca.
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
        // 2) Guardar todo en un solo updateData (atómico): nunca quedan datos a medias.
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
    private suspend fun guardar(d: Descarga): List<AlertaEntity> {
        var nuevasCriticas = emptyList<AlertaEntity>()
        store.updateData { actual ->
            val r = aplicarDescarga(actual, d)
            nuevasCriticas = r.second
            r.first
        }
        return nuevasCriticas
    }

    /** Función pura: estado guardado + descarga → nuevo estado y alertas críticas nuevas. */
    private fun aplicarDescarga(datos: DatosMonitoreo, d: Descarga): Pair<DatosMonitoreo, List<AlertaEntity>> {
        val ultimaPorGalpon = d.mediciones.groupBy { it.galponId }.mapValues { (_, l) -> l.maxBy { it.fechaHora } }
        // Galpones: reemplazo por id
        val galpones = datos.galpones.associateBy { it.id }.toMutableMap()
        d.galpones.forEach { g ->
            // Si el servidor manda un estado desconocido, se calcula con los umbrales
            val estado = runCatching { EstadoGalpon.valueOf(g.estado) }.getOrNull()
                ?: ultimaPorGalpon[g.id]?.let { Umbrales.evaluar(it.temperatura, it.humedad) }
                ?: EstadoGalpon.NORMAL
            galpones[g.id] = GalponEntity(g.id, g.granja, g.nombre, estado)
        }

        // Mediciones: una sola por (galponId, fechaHora); la repetida se reemplaza con id nuevo
        var siguienteIdMedicion = datos.siguienteIdMedicion
        val mediciones = datos.mediciones.associateBy { it.galponId to it.fechaHora }.toMutableMap()
        d.mediciones.forEach {
            val clave = it.galponId to it.fechaHora
            mediciones.remove(clave)   // al final, como una fila nueva
            mediciones[clave] = MedicionEntity(siguienteIdMedicion++, it.galponId, it.temperatura, it.humedad, it.fechaHora)
        }
        // Mantiene solo las últimas 24 h: sin esto el archivo crece para siempre
        val desde = reloj() - RETENCION_MEDICIONES_MS
        val medicionesVigentes = mediciones.values.filter { it.fechaHora >= desde }

        // Una alerta activa por galpón: se mantiene si sigue igual, se reemplaza si cambió
        // de tipo/nivel y se desactiva si el galpón volvió a la normalidad.
        val alertasApi = d.alertas
            .mapNotNull { a -> runCatching { NivelAlerta.valueOf(a.nivel) }.getOrNull()?.let { a to it } }
            .associateBy { (a, _) -> a.galponId }
        val alertas = datos.alertas.toMutableList()
        var siguienteIdAlerta = datos.siguienteIdAlerta
        val nuevasCriticas = mutableListOf<AlertaEntity>()
        d.galpones.forEach { g ->
            val indiceActual = alertas.indexOfFirst { it.galponId == g.id && it.activa }
            val actual = alertas.getOrNull(indiceActual)
            val nueva = alertasApi[g.id]
            val sinCambios = actual != null && nueva != null &&
                actual.tipo == nueva.first.tipo && actual.nivel == nueva.second
            if (sinCambios) return@forEach

            if (actual != null) alertas[indiceActual] = actual.copy(activa = false)
            if (nueva != null) {
                val (dto, nivel) = nueva
                val entidad = AlertaEntity(siguienteIdAlerta++, dto.galponId, dto.tipo, nivel, true, dto.fechaHora)
                alertas += entidad
                if (nivel == NivelAlerta.CRITICO) nuevasCriticas += entidad
            }
        }

        val nuevo = datos.copy(
            galpones = galpones.values.sortedBy { it.id },
            mediciones = medicionesVigentes,
            alertas = alertas,
            siguienteIdAlerta = siguienteIdAlerta,
            siguienteIdMedicion = siguienteIdMedicion
        )
        return nuevo to nuevasCriticas
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
    override suspend fun confirmarAlerta(alertaId: Int, galponId: Int, rol: Rol, detalle: String) {
        store.updateData { d ->
            val ahora = reloj()
            d.copy(
                alertas = d.alertas.map { if (it.id == alertaId) it.copy(confirmadaPor = rol.name, confirmadaEn = ahora) else it },
                eventos = d.eventos + EventoEntity(d.siguienteIdEvento, galponId, detalle, ACCION_CONFIRMAR, ahora),
                siguienteIdEvento = d.siguienteIdEvento + 1
            )
        }
    }

    /** Local: el operario puede registrar acciones aunque esté sin conexión dentro del galpón. */
    override suspend fun registrarAccion(galponId: Int, tipoAccion: String, descripcion: String) {
        store.updateData { d ->
            d.copy(
                eventos = d.eventos + EventoEntity(d.siguienteIdEvento, galponId, descripcion, tipoAccion, reloj()),
                siguienteIdEvento = d.siguienteIdEvento + 1
            )
        }
    }
}

/** Código guardado en eventos.accionRegistrada; el texto visible está en strings.xml. */
const val ACCION_CONFIRMAR = "CONFIRMACION_ALERTA"

/** Las mediciones se guardan 24 h (96 lecturas por galpón, una cada 15 min). */
const val RETENCION_MEDICIONES_MS = 24 * 60 * 60_000L

private const val TAG = "GalponRepository"
