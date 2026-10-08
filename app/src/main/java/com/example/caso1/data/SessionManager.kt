package com.example.caso1.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.caso1.data.model.Rol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sesion")

class SessionManager(private val context: Context) {
    private val KEY_ROL = stringPreferencesKey("rol")

    val rol: Flow<Rol?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ROL]?.let { runCatching { Rol.valueOf(it) }.getOrNull() }
    }

    suspend fun guardarRol(rol: Rol) {
        context.dataStore.edit { it[KEY_ROL] = rol.name }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { it.remove(KEY_ROL) }
    }
}
