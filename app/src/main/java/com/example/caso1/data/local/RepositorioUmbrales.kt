package com.example.caso1.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.Umbrales
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

/** Lo que la app necesita de los umbrales. En los tests se reemplaza por una versión en memoria. */
interface RepositorioUmbrales {
    /** Umbrales actuales; si nunca se guardaron, los predeterminados. */
    val umbrales: Flow<ConfigUmbrales>
    suspend fun guardar(c: ConfigUmbrales)
    suspend fun restaurar()
}

/** Preferences DataStore propio (archivo files/datastore/umbrales.preferences_pb). */
val Context.almacenUmbrales: DataStore<Preferences> by preferencesDataStore(name = "umbrales")

/** Para tests: un DataStore sobre [archivo] (debe terminar en .preferences_pb); cancelar [alcance] lo libera. */
fun crearAlmacenUmbrales(archivo: File, alcance: CoroutineScope): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(scope = alcance, produceFile = { archivo })

/** Guarda los seis umbrales como pares clave-valor (Double) en DataStore. */
class UmbralesRepository(private val store: DataStore<Preferences>) : RepositorioUmbrales {

    override val umbrales: Flow<ConfigUmbrales> = store.data
        // Si el archivo no se puede leer, se usan los predeterminados en vez de cerrar la app
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            // Clave ausente → valor predeterminado
            val d = Umbrales.PREDETERMINADOS
            ConfigUmbrales(
                tempNormalMin = p[TEMP_NORMAL_MIN] ?: d.tempNormalMin,
                tempNormalMax = p[TEMP_NORMAL_MAX] ?: d.tempNormalMax,
                tempAdvertenciaMax = p[TEMP_ADVERTENCIA_MAX] ?: d.tempAdvertenciaMax,
                humedadNormalMin = p[HUMEDAD_NORMAL_MIN] ?: d.humedadNormalMin,
                humedadNormalMax = p[HUMEDAD_NORMAL_MAX] ?: d.humedadNormalMax,
                humedadAdvertenciaMax = p[HUMEDAD_ADVERTENCIA_MAX] ?: d.humedadAdvertenciaMax
            )
        }

    /** Un solo edit: los seis valores se guardan juntos o no se guarda ninguno. */
    override suspend fun guardar(c: ConfigUmbrales) {
        store.edit { p ->
            p[TEMP_NORMAL_MIN] = c.tempNormalMin
            p[TEMP_NORMAL_MAX] = c.tempNormalMax
            p[TEMP_ADVERTENCIA_MAX] = c.tempAdvertenciaMax
            p[HUMEDAD_NORMAL_MIN] = c.humedadNormalMin
            p[HUMEDAD_NORMAL_MAX] = c.humedadNormalMax
            p[HUMEDAD_ADVERTENCIA_MAX] = c.humedadAdvertenciaMax
        }
    }

    /** Borra las claves: al leer, vuelven los predeterminados. */
    override suspend fun restaurar() {
        store.edit { it.clear() }
    }

    private companion object {
        val TEMP_NORMAL_MIN = doublePreferencesKey("temp_normal_min")
        val TEMP_NORMAL_MAX = doublePreferencesKey("temp_normal_max")
        val TEMP_ADVERTENCIA_MAX = doublePreferencesKey("temp_advertencia_max")
        val HUMEDAD_NORMAL_MIN = doublePreferencesKey("humedad_normal_min")
        val HUMEDAD_NORMAL_MAX = doublePreferencesKey("humedad_normal_max")
        val HUMEDAD_ADVERTENCIA_MAX = doublePreferencesKey("humedad_advertencia_max")
    }
}
