package com.example.caso1

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.caso1.data.api.AlertaDto
import com.example.caso1.data.api.ClienteApi
import com.example.caso1.data.api.GalponDto
import com.example.caso1.data.api.MedicionDto
import com.example.caso1.data.api.MonitorApi
import com.example.caso1.data.api.SimulacionDto
import com.example.caso1.data.db.AppDatabase
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.repository.EstadoConexion
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.data.repository.ResultadoSincronizacion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Plan §8 / case 3.3 (intermittent connectivity): when the API fails, the data already
 * stored in Room keeps being served and the connection state reports the failure.
 */
@RunWith(AndroidJUnit4::class)
class RepositorioSinConexionTest {

    private val ahora = 1_800_000_000_000L
    private lateinit var db: AppDatabase

    /** API under test control: answers one shed, or fails the way the network would. */
    private class ApiControlada(private val ahora: Long) : MonitorApi {
        var falla: (() -> Nothing)? = null

        override suspend fun galpones(): List<GalponDto> {
            falla?.invoke()
            return listOf(GalponDto(1, "Granja Norte", "Galpón 1", "CRITICO"))
        }

        override suspend fun mediciones(galponId: Int) =
            listOf(MedicionDto(galponId, temperatura = 33.5, humedad = 60.0, fechaHora = ahora))

        override suspend fun alertasActivas() =
            listOf(AlertaDto(1, "TEMPERATURA_ALTA", "CRITICO", ahora))

        override suspend fun simularPicoDeCalor() = SimulacionDto(1)
    }

    @Before fun crearBaseEnMemoria() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun cerrarBase() = db.close()

    /** Syncs once successfully, then runs [despues] against the same database. */
    private fun conDatosGuardados(api: MonitorApi, despues: suspend (GalponRepository) -> Unit) = runBlocking {
        val repo = GalponRepository(db, api, reloj = { ahora })
        assertTrue(repo.refrescar() is ResultadoSincronizacion.Correcto)
        despues(repo)
    }

    private suspend fun assertDatosGuardadosIntactos(repo: GalponRepository) {
        val galpones = repo.galpones.first()
        assertEquals(listOf(1), galpones.map { it.id })
        assertEquals(EstadoGalpon.CRITICO, galpones.single().estado)
        assertEquals(listOf(33.5), repo.mediciones(1).first().map { it.temperatura })
        assertEquals(listOf(1), repo.alertasActivas.first().map { it.galponId })
    }

    @Test fun sinRedSeSiguenMostrandoLosDatosDeRoom() = conDatosGuardados(ApiControlada(ahora)) { repo ->
        val api = ApiControlada(ahora).apply { falla = { throw IOException("sin red") } }
        val repoSinRed = GalponRepository(db, api, reloj = { ahora })

        assertEquals(ResultadoSincronizacion.SinConexion, repoSinRed.refrescar())
        assertEquals(EstadoConexion.SIN_CONEXION, repoSinRed.estadoConexion.value)
        assertDatosGuardadosIntactos(repoSinRed)
        assertDatosGuardadosIntactos(repo)
    }

    @Test fun errorDelServidorNoBorraLosDatosDeRoom() = conDatosGuardados(ApiControlada(ahora)) { _ ->
        val api = ApiControlada(ahora).apply {
            falla = { throw HttpException(Response.error<Any>(500, "{}".toResponseBody())) }
        }
        val repo = GalponRepository(db, api, reloj = { ahora })

        assertEquals(ResultadoSincronizacion.SinConexion, repo.refrescar())
        assertDatosGuardadosIntactos(repo)
    }

    @Test fun servidorApagadoUsaLosDatosDeRoom() = conDatosGuardados(ApiControlada(ahora)) { _ ->
        // Real HTTP client against a port where nothing listens (json-server not running)
        val api = ClienteApi.crear(urlBase = "http://localhost:1/api/v1/")
        val repo = GalponRepository(db, api, reloj = { ahora })

        assertEquals(ResultadoSincronizacion.SinConexion, repo.refrescar())
        assertDatosGuardadosIntactos(repo)
    }
}
