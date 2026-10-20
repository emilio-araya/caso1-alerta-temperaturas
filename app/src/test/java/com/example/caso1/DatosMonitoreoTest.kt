package com.example.caso1

import com.example.caso1.data.local.SerializadorMonitoreo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

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
}
