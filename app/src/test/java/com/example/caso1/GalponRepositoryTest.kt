package com.example.caso1

import androidx.datastore.core.DataStore
import com.example.caso1.data.api.AlertaDto
import com.example.caso1.data.api.ClienteApi
import com.example.caso1.data.api.GalponDto
import com.example.caso1.data.api.MedicionDto
import com.example.caso1.data.api.MonitorApi
import com.example.caso1.data.api.SimulacionDto
import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.DatosMonitoreo
import com.example.caso1.data.local.EventoEntity
import com.example.caso1.data.local.MedicionEntity
import com.example.caso1.data.local.crearAlmacenMonitoreo
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.model.Rol
import com.example.caso1.data.repository.ACCION_CONFIRMAR
import com.example.caso1.data.repository.EstadoConexion
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.data.repository.RETENCION_MEDICIONES_MS
import com.example.caso1.data.repository.ResultadoSincronizacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException

/**
 * GalponRepository over a real DataStore file (no emulator): offline single source of truth,
 * all-or-nothing sync, one active alert per shed, only new critical alerts reported,
 * measurement upsert, 24 h retention, confirmation and persistence across restarts.
 */
class GalponRepositoryTest {

    @get:Rule val carpeta = TemporaryFolder()

    private val ahora = 1_800_000_000_000L
    private val archivo: File by lazy { File(carpeta.root, "monitoreo.json") }
    private val alcances = mutableListOf<CoroutineScope>()

    /** API under test control: answers what the test sets, or fails the way the network would. */
    private class ApiControlada(ahora: Long) : MonitorApi {
        var falla: (() -> Nothing)? = null
        var galpones = listOf(GalponDto(1, "Granja Norte", "Galpón 1", "CRITICO"))
        var mediciones = mapOf(1 to listOf(MedicionDto(1, temperatura = 33.5, humedad = 60.0, fechaHora = ahora)))
        var alertas = listOf(AlertaDto(1, "TEMPERATURA_ALTA", "CRITICO", ahora))

        override suspend fun galpones(): List<GalponDto> {
            falla?.invoke()
            return galpones
        }

        override suspend fun mediciones(galponId: Int) = mediciones[galponId].orEmpty()
        override suspend fun alertasActivas() = alertas
        override suspend fun simularPicoDeCalor() = SimulacionDto(1)
    }

    /** One store per file at a time (DataStore rule); its scope is cancelled to release the file. */
    private fun abrirAlmacen(): DataStore<DatosMonitoreo> {
        val alcance = CoroutineScope(Dispatchers.IO + Job())
        alcances += alcance
        return crearAlmacenMonitoreo(archivo, alcance)
    }

    private fun cerrarAlmacenes() = runBlocking {
        alcances.forEach { it.coroutineContext.job.cancelAndJoin() }
        alcances.clear()
    }

    @After fun liberarArchivo() = cerrarAlmacenes()

    /** Syncs once successfully, then runs [despues] against the same store. */
    private fun conDatosGuardados(despues: suspend (DataStore<DatosMonitoreo>, GalponRepository) -> Unit) = runBlocking {
        val store = abrirAlmacen()
        val repo = GalponRepository(store, ApiControlada(ahora), reloj = { ahora })
        assertTrue(repo.refrescar() is ResultadoSincronizacion.Correcto)
        assertEquals(EstadoConexion.EN_LINEA, repo.estadoConexion.value)
        despues(store, repo)
    }

    private suspend fun assertDatosGuardadosIntactos(repo: GalponRepository) {
        val galpones = repo.galpones.first()
        assertEquals(listOf(1), galpones.map { it.id })
        assertEquals(EstadoGalpon.CRITICO, galpones.single().estado)
        assertEquals(listOf(33.5), repo.mediciones(1).first().map { it.temperatura })
        assertEquals(listOf(1), repo.alertasActivas.first().map { it.galponId })
        assertEquals(1, repo.alertas.first().size)
        assertTrue(repo.eventos.first().isEmpty())
    }

    // --- Ported from the former androidTest RepositorioSinConexionTest (case 3.3) ---

    @Test fun sinRedSeSiguenMostrandoLosDatosGuardados() = conDatosGuardados { store, repo ->
        val api = ApiControlada(ahora).apply { falla = { throw IOException("sin red") } }
        val repoSinRed = GalponRepository(store, api, reloj = { ahora })

        assertEquals(ResultadoSincronizacion.SinConexion, repoSinRed.refrescar())
        assertEquals(EstadoConexion.SIN_CONEXION, repoSinRed.estadoConexion.value)
        assertDatosGuardadosIntactos(repoSinRed)
        assertDatosGuardadosIntactos(repo)
    }

    @Test fun errorDelServidorNoBorraLosDatosGuardados() = conDatosGuardados { store, _ ->
        val api = ApiControlada(ahora).apply {
            falla = { throw HttpException(Response.error<Any>(500, "{}".toResponseBody())) }
        }
        val repo = GalponRepository(store, api, reloj = { ahora })

        assertEquals(ResultadoSincronizacion.SinConexion, repo.refrescar())
        assertEquals(EstadoConexion.SIN_CONEXION, repo.estadoConexion.value)
        assertDatosGuardadosIntactos(repo)
    }

    @Test fun servidorApagadoUsaLosDatosGuardados() = conDatosGuardados { store, _ ->
        // Real HTTP client against a port where nothing listens (json-server not running)
        val api = ClienteApi.crear(urlBase = "http://localhost:1/api/v1/")
        val repo = GalponRepository(store, api, reloj = { ahora })

        assertEquals(ResultadoSincronizacion.SinConexion, repo.refrescar())
        assertEquals(EstadoConexion.SIN_CONEXION, repo.estadoConexion.value)
        assertDatosGuardadosIntactos(repo)
    }

    // --- Alert rules ---

    @Test fun alertaSinCambiosConservaLaConfirmacion() = runBlocking {
        val api = ApiControlada(ahora)
        var reloj = ahora
        val repo = GalponRepository(abrirAlmacen(), api, reloj = { reloj })
        repo.refrescar()
        val alerta = repo.alertasActivas.first().single()
        reloj = ahora + 1_000
        repo.confirmarAlerta(alerta.id, 1, Rol.SUPERVISOR, "Confirmada")

        // Same tipo and nivel, newer timestamp: the stored alert must stay untouched
        reloj = ahora + 60_000
        api.alertas = listOf(AlertaDto(1, "TEMPERATURA_ALTA", "CRITICO", ahora + 60_000))
        val r = repo.refrescar()

        assertEquals(ResultadoSincronizacion.Correcto(emptyList()), r)
        val esperada = AlertaEntity(1, 1, "TEMPERATURA_ALTA", NivelAlerta.CRITICO, true, ahora, "SUPERVISOR", ahora + 1_000)
        assertEquals(listOf(esperada), repo.alertas.first())
        assertEquals(listOf(esperada), repo.alertasActivas.first())
    }

    @Test fun cambioDeNivelReemplazaLaAlertaYNotificaSoloUnaVez() = runBlocking {
        val api = ApiControlada(ahora).apply {
            alertas = listOf(AlertaDto(1, "TEMPERATURA_ALTA", "ADVERTENCIA", ahora))
        }
        val repo = GalponRepository(abrirAlmacen(), api, reloj = { ahora })
        assertEquals(ResultadoSincronizacion.Correcto(emptyList()), repo.refrescar())

        api.alertas = listOf(AlertaDto(1, "TEMPERATURA_ALTA", "CRITICO", ahora + 5))
        val nueva = AlertaEntity(2, 1, "TEMPERATURA_ALTA", NivelAlerta.CRITICO, true, ahora + 5)
        assertEquals(ResultadoSincronizacion.Correcto(listOf(nueva)), repo.refrescar())

        val antigua = AlertaEntity(1, 1, "TEMPERATURA_ALTA", NivelAlerta.ADVERTENCIA, false, ahora)
        assertEquals(listOf(nueva, antigua), repo.alertas.first())
        assertEquals(listOf(nueva), repo.alertasActivas.first())

        // Next identical sync: nothing new to notify, nothing changes
        assertEquals(ResultadoSincronizacion.Correcto(emptyList()), repo.refrescar())
        assertEquals(listOf(nueva, antigua), repo.alertas.first())
    }

    @Test fun galponNormalizadoDesactivaSuAlertaYLosAusentesLaConservan() = runBlocking {
        val api = ApiControlada(ahora).apply {
            galpones = listOf(GalponDto(1, "Granja Norte", "Galpón 1", "CRITICO"), GalponDto(2, "Granja Sur", "Galpón 2", "CRITICO"))
            alertas = listOf(AlertaDto(1, "TEMPERATURA_ALTA", "CRITICO", ahora), AlertaDto(2, "HUMEDAD_ALTA", "CRITICO", ahora - 1))
        }
        val repo = GalponRepository(abrirAlmacen(), api, reloj = { ahora })
        val primera = repo.refrescar() as ResultadoSincronizacion.Correcto
        assertEquals(listOf(1, 2), primera.nuevasCriticas.map { it.id })

        // Shed 1 back to normal; shed 2 missing from this download keeps its alert
        api.galpones = listOf(GalponDto(1, "Granja Norte", "Galpón 1", "NORMAL"))
        api.alertas = emptyList()
        assertEquals(ResultadoSincronizacion.Correcto(emptyList()), repo.refrescar())

        assertEquals(listOf(2), repo.alertasActivas.first().map { it.galponId })
        assertEquals(listOf(1 to false, 2 to true), repo.alertas.first().map { it.galponId to it.activa })
        assertEquals(listOf(EstadoGalpon.NORMAL, EstadoGalpon.CRITICO), repo.galpones.first().map { it.estado })
    }

    // --- Measurements ---

    @Test fun medicionRepetidaSeReemplazaSinDuplicar() = runBlocking {
        val api = ApiControlada(ahora)
        val repo = GalponRepository(abrirAlmacen(), api, reloj = { ahora })
        repo.refrescar()

        api.mediciones = mapOf(1 to listOf(MedicionDto(1, temperatura = 34.0, humedad = 61.0, fechaHora = ahora)))
        repo.refrescar()

        // Replaced like Room's REPLACE: new row id, new values, one row per (galponId, fechaHora)
        val esperada = MedicionEntity(2, 1, 34.0, 61.0, ahora)
        assertEquals(listOf(esperada), repo.mediciones(1).first())
        assertEquals(listOf(esperada), repo.ultimasMediciones.first())
    }

    @Test fun retencionBorraLasMedicionesAntiguas() = runBlocking {
        val antigua = ahora - RETENCION_MEDICIONES_MS - 1
        val limite = ahora - RETENCION_MEDICIONES_MS
        val api = ApiControlada(ahora).apply {
            galpones = listOf(GalponDto(1, "Granja Norte", "Galpón 1", "?"), GalponDto(2, "Granja Sur", "Galpón 2", "?"))
            mediciones = mapOf(
                1 to listOf(
                    MedicionDto(1, 20.0, 60.0, antigua),
                    MedicionDto(1, 30.0, 60.0, limite),
                    MedicionDto(1, 33.5, 60.0, ahora)
                ),
                2 to listOf(MedicionDto(2, 29.0, 60.0, ahora - 10))
            )
            alertas = emptyList()
        }
        val repo = GalponRepository(abrirAlmacen(), api, reloj = { ahora })
        repo.refrescar()

        assertEquals(listOf(ahora, limite), repo.mediciones(1).first().map { it.fechaHora })
        assertEquals(listOf(ahora - 10), repo.mediciones(2).first().map { it.fechaHora })
        assertEquals(listOf(1 to ahora, 2 to ahora - 10), repo.ultimasMediciones.first().map { it.galponId to it.fechaHora }.sortedBy { it.first })
        // Unknown server state: computed from the newest reading with the thresholds
        assertEquals(listOf(EstadoGalpon.CRITICO, EstadoGalpon.ADVERTENCIA), repo.galpones.first().map { it.estado })
    }

    // --- Local actions ---

    @Test fun confirmarAlertaRegistraQuienYCuandoConUnEvento() = runBlocking {
        val store = abrirAlmacen()
        val repo = GalponRepository(store, ApiControlada(ahora), reloj = { ahora + 7 })
        repo.refrescar()
        val id = repo.alertasActivas.first().single().id

        repo.confirmarAlerta(id, 1, Rol.OPERARIO, "Alerta confirmada")

        val alerta = repo.alertas.first().single()
        assertEquals("OPERARIO", alerta.confirmadaPor)
        assertEquals(ahora + 7, alerta.confirmadaEn)
        assertTrue(alerta.activa)
        assertEquals(listOf(EventoEntity(1, 1, "Alerta confirmada", ACCION_CONFIRMAR, ahora + 7)), repo.eventos.first())
    }

    @Test fun losDatosSobrevivenAlReiniciarLaApp() = runBlocking {
        val repo = GalponRepository(abrirAlmacen(), ApiControlada(ahora), reloj = { ahora })
        repo.refrescar()
        repo.registrarAccion(1, "VENTILACION", "Ventiladores al máximo")
        cerrarAlmacenes()

        // New store and repository over the same file, without network
        val api = ApiControlada(ahora).apply { falla = { throw IOException("sin red") } }
        val reabierto = GalponRepository(abrirAlmacen(), api, reloj = { ahora + 1 })

        assertEquals(listOf(1), reabierto.galpones.first().map { it.id })
        assertEquals(listOf(33.5), reabierto.mediciones(1).first().map { it.temperatura })
        assertEquals(listOf(1), reabierto.alertasActivas.first().map { it.id })
        assertEquals(listOf(EventoEntity(1, 1, "Ventiladores al máximo", "VENTILACION", ahora)), reabierto.eventos.first())
        assertEquals(ResultadoSincronizacion.SinConexion, reabierto.refrescar())

        // Id counters persisted too: the next event does not reuse id 1
        reabierto.registrarAccion(1, "OTRA", "Segunda")
        assertEquals(listOf(2, 1), reabierto.eventos.first().map { it.id })
    }
}
