package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.SessionManager
import com.example.caso1.data.model.Rol
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Distingue "aún leyendo DataStore" de "no hay sesión", para no mostrar el login de paso. */
sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class Activa(val rol: Rol) : EstadoSesion
}

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager(app)

    val estado: StateFlow<EstadoSesion> = session.rol
        .map { rol -> if (rol == null) EstadoSesion.SinSesion else EstadoSesion.Activa(rol) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoSesion.Cargando)

    fun iniciarSesion(rol: Rol) = viewModelScope.launch { session.guardarRol(rol) }
    fun cerrarSesion() = viewModelScope.launch { session.cerrarSesion() }
}
