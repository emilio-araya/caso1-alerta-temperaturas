package com.example.caso1

import com.example.caso1.data.local.borrarEvidencia
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Si el operario abre la cámara y cancela, el archivo que se preparó para la foto queda vacío.
 * [borrarEvidencia] lo descarta para no acumular basura en files/evidencias/.
 */
class EvidenciasTest {

    @Test fun borraElArchivoPendiente() {
        val carpeta = Files.createTempDirectory("evidencias").toFile()
        val archivo = File(carpeta, "accion-1.jpg").apply { writeBytes(ByteArray(0)) }
        try {
            borrarEvidencia(archivo.absolutePath)

            assertFalse(archivo.exists())
        } finally {
            carpeta.deleteRecursively()
        }
    }

    @Test fun unaRutaNulaOYaBorradaNoFalla() {
        borrarEvidencia(null)
        borrarEvidencia("/no/existe/accion-2.jpg")

        assertTrue(true)
    }
}
