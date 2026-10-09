package com.example.caso1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
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

class SessionViewModel(private val session: SessionManager) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = session.rol
        .map { rol -> if (rol == null) EstadoSesion.SinSesion else EstadoSesion.Activa(rol) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoSesion.Cargando)

    fun iniciarSesion(rol: Rol) = viewModelScope.launch { session.guardarRol(rol) }
    fun cerrarSesion() = viewModelScope.launch { session.cerrarSesion() }

    companion object {
        val Factory = viewModelFactory {
            initializer { SessionViewModel(contenedor().sesion) }
        }
    }
}
