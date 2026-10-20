package com.example.caso1.data.local

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Caché local del monitoreo, guardada como un único documento JSON en DataStore tipado.
 * Se usa DataStore tipado (no Preferences) porque son listas estructuradas; la sesión
 * sigue en Preferences DataStore. Cada `updateData` reescribe el documento completo de
 * forma atómica: o se guarda todo o no se guarda nada.
 *
 * Los `siguienteId*` reemplazan a los ids autogenerados: nunca se reutiliza un id.
 */
@Serializable
data class DatosMonitoreo(
    val galpones: List<GalponEntity> = emptyList(),
    val mediciones: List<MedicionEntity> = emptyList(),
    val alertas: List<AlertaEntity> = emptyList(),
    val eventos: List<EventoEntity> = emptyList(),
    val siguienteIdAlerta: Int = 1,
    val siguienteIdEvento: Int = 1,
    val siguienteIdMedicion: Int = 1
)

/** Convierte [DatosMonitoreo] a JSON y viceversa para DataStore. */
object SerializadorMonitoreo : Serializer<DatosMonitoreo> {
    private val json = Json { ignoreUnknownKeys = true }   // tolera campos de versiones futuras

    override val defaultValue: DatosMonitoreo = DatosMonitoreo()

    override suspend fun readFrom(input: InputStream): DatosMonitoreo = try {
        json.decodeFromString(DatosMonitoreo.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("monitoreo.json is not valid", e)
    }

    override suspend fun writeTo(t: DatosMonitoreo, output: OutputStream) {
        output.write(json.encodeToString(DatosMonitoreo.serializer(), t).encodeToByteArray())
    }
}

/**
 * Un JSON roto (escritura interrumpida, archivo editado a mano) no puede dejar la app inutilizable:
 * la caché se reemplaza por un documento vacío y el próximo sync la vuelve a llenar.
 */
private val reemplazarSiEstaCorrupto = ReplaceFileCorruptionHandler { DatosMonitoreo() }

/** DataStore de la app (archivo files/datastore/monitoreo.json). Distinto del de la sesión ("sesion"). */
val Context.datosMonitoreo: DataStore<DatosMonitoreo> by dataStore(
    fileName = "monitoreo.json",
    serializer = SerializadorMonitoreo,
    corruptionHandler = reemplazarSiEstaCorrupto
)

/** Para tests: un DataStore sobre [archivo]; cancelar [alcance] libera el archivo. */
fun crearAlmacenMonitoreo(archivo: File, alcance: CoroutineScope): DataStore<DatosMonitoreo> =
    DataStoreFactory.create(
        serializer = SerializadorMonitoreo,
        corruptionHandler = reemplazarSiEstaCorrupto,
        scope = alcance,
        produceFile = { archivo }
    )
