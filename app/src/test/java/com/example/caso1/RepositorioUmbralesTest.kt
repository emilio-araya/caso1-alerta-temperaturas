package com.example.caso1

import com.example.caso1.data.local.UmbralesRepository
import com.example.caso1.data.local.crearAlmacenUmbrales
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.Umbrales
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Thresholds in a real Preferences DataStore file (no emulator). */
class RepositorioUmbralesTest {

    @get:Rule val carpeta = TemporaryFolder()

    private val archivo: File by lazy { File(carpeta.root, "umbrales.preferences_pb") }
    private val alcances = mutableListOf<CoroutineScope>()

    private fun abrir(): UmbralesRepository {
        val alcance = CoroutineScope(Dispatchers.IO + Job())
        alcances += alcance
        return UmbralesRepository(crearAlmacenUmbrales(archivo, alcance))
    }

    private fun cerrar() = runBlocking {
        alcances.forEach { it.coroutineContext.job.cancelAndJoin() }
        alcances.clear()
    }

    @After fun liberarArchivo() = cerrar()

    private val personalizada = ConfigUmbrales(17.5, 25.0, 30.0, 45.0, 65.0, 85.0)

    @Test fun sinDatosDevuelveLosPredeterminados() = runBlocking {
        assertEquals(Umbrales.PREDETERMINADOS, abrir().umbrales.first())
    }

    @Test fun guardarYLeer() = runBlocking {
        val repo = abrir()
        repo.guardar(personalizada)
        assertEquals(personalizada, repo.umbrales.first())
    }

    @Test fun sobreviveAlReiniciarLaApp() = runBlocking {
        abrir().guardar(personalizada)
        cerrar()   // DataStore admite una sola instancia activa por archivo
        assertEquals(personalizada, abrir().umbrales.first())
    }

    @Test fun restaurarVuelveALosPredeterminados() = runBlocking {
        val repo = abrir()
        repo.guardar(personalizada)
        repo.restaurar()
        assertEquals(Umbrales.PREDETERMINADOS, repo.umbrales.first())
    }
}
