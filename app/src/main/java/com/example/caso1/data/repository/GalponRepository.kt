package com.example.caso1.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import com.example.caso1.data.api.GalponDto
import com.example.caso1.data.api.MedicionDto
import com.example.caso1.data.api.MonitorApi
import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.DatosMonitoreo
import com.example.caso1.data.local.EventoEntity
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.local.MedicionEntity
import com.example.caso1.data.model.ConfigUmbrales
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Fuente única de verdad: la UI siempre lee de DataStore. La API solo se usa para
 * actualizar DataStore; si no hay conexión, la app sigue mostrando lo último guardado.
 *
 * Regla de negocio en el dispositivo: el estado y la alerta de cada galpón se calculan
 * aquí con la medición más reciente y los [umbrales] configurados; la API solo entrega
 * mediciones (su "estado" y sus alertas no se usan).
 */
class GalponRepository(
    private val store: DataStore<DatosMonitoreo>,
    private val api: MonitorApi,
    private val umbrales: Flow<ConfigUmbrales> = flowOf(Umbrales.PREDETERMINADOS),
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

    private class Descarga(val galpones: List<GalponDto>, val mediciones: List<MedicionDto>)

    private suspend fun descargar(): Descarga = coroutineScope {
        val galpones = api.galpones()
        // Las mediciones de cada galpón se piden en paralelo
        val mediciones = galpones.map { g -> async { api.mediciones(g.id) } }.awaitAll().flatten()
        Descarga(galpones, mediciones)
    }

    /**
     * Devuelve las alertas CRÍTICAS que aparecieron en esta sincronización (las que ya
     * estaban activas no se repiten), para notificar solo lo nuevo.
     */
    private suspend fun guardar(d: Descarga): List<AlertaEntity> {
        val config = umbrales.first()
        return actualizarYEvaluar { actual -> aplicarDescarga(actual, d, config) }
    }

    /**
     * Recalcula estados y alertas con los umbrales actuales usando las mediciones YA
     * guardadas: no llama a la API, funciona sin conexión. Devuelve las críticas nuevas.
     */
    override suspend fun reevaluar(): List<AlertaEntity> {
        val config = umbrales.first()
        return actualizarYEvaluar { actual -> evaluarGalpones(actual, actual.galpones.map { it.id }.toSet(), config) }
    }

    /** Un solo updateData (atómico); devuelve las alertas críticas nuevas que calculó [cambio]. */
    private suspend fun actualizarYEvaluar(
        cambio: (DatosMonitoreo) -> Pair<DatosMonitoreo, List<AlertaEntity>>
    ): List<AlertaEntity> {
        var nuevasCriticas = emptyList<AlertaEntity>()
        store.updateData { actual ->
            val (nuevo, criticas) = cambio(actual)
            nuevasCriticas = criticas
            nuevo
        }
        return nuevasCriticas
    }

    /** Función pura: estado guardado + descarga → nuevo estado y alertas críticas nuevas. */
    private fun aplicarDescarga(datos: DatosMonitoreo, d: Descarga, config: ConfigUmbrales): Pair<DatosMonitoreo, List<AlertaEntity>> {
        // Galpones: reemplazo por id; el estado se calcula después con las mediciones
        val galpones = datos.galpones.associateBy { it.id }.toMutableMap()
        d.galpones.forEach { g ->
            galpones[g.id] = GalponEntity(g.id, g.granja, g.nombre, galpones[g.id]?.estado ?: EstadoGalpon.NORMAL)
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

        val fusionado = datos.copy(
            galpones = galpones.values.sortedBy { it.id },
            mediciones = medicionesVigentes,
            siguienteIdMedicion = siguienteIdMedicion
        )
        // Solo se evalúan los galpones descargados: los ausentes quedan como estaban
        return evaluarGalpones(fusionado, d.galpones.map { it.id }.toSet(), config)
    }

    /**
     * Regla de negocio compartida por [refrescar] y [reevaluar]: el estado de cada galpón de
     * [ids] sale de su medición más reciente evaluada con [config]. Una alerta activa por
     * galpón: se mantiene si sigue igual (conserva su confirmación), se reemplaza si cambió
     * de tipo/nivel y se desactiva si el galpón volvió a la normalidad.
     */
    private fun evaluarGalpones(datos: DatosMonitoreo, ids: Set<Int>, config: ConfigUmbrales): Pair<DatosMonitoreo, List<AlertaEntity>> {
        val ultimaPorGalpon = datos.mediciones.filter { it.galponId in ids }
            .groupBy { it.galponId }.mapValues { (_, l) -> l.maxBy { it.fechaHora } }
        val alertas = datos.alertas.toMutableList()
        var siguienteIdAlerta = datos.siguienteIdAlerta
        val nuevasCriticas = mutableListOf<AlertaEntity>()

        val galpones = datos.galpones.map { g ->
            // Sin lecturas no hay nada que evaluar: el galpón y su alerta quedan como estaban
            val m = ultimaPorGalpon[g.id] ?: return@map g
            val estado = config.evaluar(m.temperatura, m.humedad)
            val tipo = config.causa(m.temperatura, m.humedad)
            val nivel = when (estado) {
                EstadoGalpon.CRITICO -> NivelAlerta.CRITICO
                EstadoGalpon.ADVERTENCIA -> NivelAlerta.ADVERTENCIA
                EstadoGalpon.NORMAL -> null
            }
            val indiceActual = alertas.indexOfFirst { it.galponId == g.id && it.activa }
            val actual = alertas.getOrNull(indiceActual)
            val sinCambios = actual?.tipo == tipo && actual?.nivel == nivel
            if (!sinCambios) {
                if (actual != null) alertas[indiceActual] = actual.copy(activa = false)
                if (nivel != null && tipo != null) {
                    val nueva = AlertaEntity(siguienteIdAlerta++, g.id, tipo, nivel, true, m.fechaHora)
                    alertas += nueva
                    if (nivel == NivelAlerta.CRITICO) nuevasCriticas += nueva
                }
            }
            g.copy(estado = estado)
        }

        val nuevo = datos.copy(galpones = galpones, alertas = alertas, siguienteIdAlerta = siguienteIdAlerta)
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
