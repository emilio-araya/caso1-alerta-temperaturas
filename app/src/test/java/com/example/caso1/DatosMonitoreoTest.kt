package com.example.caso1

import com.example.caso1.data.local.DatosMonitoreo
import com.example.caso1.data.local.SerializadorMonitoreo
import com.example.caso1.data.local.crearAlmacenMonitoreo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

/**
 * El JSON ya guardado en los teléfonos no trae los campos nuevos (medición de la alerta y
 * usuario de la acción): debe seguir leyéndose, con null en esos campos.
 */
class DatosMonitoreoTest {

    private val jsonAntiguo = """
        {
          "galpones": [],
          "mediciones": [],
          "alertas": [
            {"id": 1, "galponId": 1, "tipo": "TEMPERATURA_ALTA", "nivel": "CRITICO", "activa": true, "fechaHora": 123}
          ],
          "eventos": [
            {"id": 1, "galponId": 1, "descripcion": "Encendí los ventiladores", "accionRegistrada": "VENTILACION", "fechaHora": 456}
          ],
          "siguienteIdAlerta": 2,
          "siguienteIdEvento": 2,
          "siguienteIdMedicion": 1
        }
    """.trimIndent()

    @Test fun unJsonSinLosCamposNuevosSeLeeConNulls() = runBlocking {
        val datos = SerializadorMonitoreo.readFrom(ByteArrayInputStream(jsonAntiguo.toByteArray()))

        val alerta = datos.alertas.single()
        assertNull(alerta.temperatura)
        assertNull(alerta.humedad)
        assertNull(datos.eventos.single().usuario)
        assertNull(datos.eventos.single().fotoRuta)
        assertEquals("TEMPERATURA_ALTA", alerta.tipo)
    }

    @Test fun unArchivoCorruptoSeReemplazaPorUnDocumentoVacio() = runBlocking {
        // Sin manejador de corrupción, un JSON roto deja la app inutilizable: cada lectura lanza
        // CorruptionException y la caché nunca vuelve a cargar.
        val carpeta = Files.createTempDirectory("monitoreo").toFile()
        val archivo = File(carpeta, "monitoreo-corrupto.json").apply { writeText("{ esto no es json") }
        val alcance = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val datos = crearAlmacenMonitoreo(archivo, alcance).data.first()

            assertEquals(DatosMonitoreo(), datos)
            // El archivo quedó reemplazado por un documento válido, listo para el próximo guardado
            val releido = SerializadorMonitoreo.readFrom(ByteArrayInputStream(archivo.readBytes()))
            assertEquals(DatosMonitoreo(), releido)
        } finally {
            alcance.cancel()
            carpeta.deleteRecursively()
        }
    }
}
