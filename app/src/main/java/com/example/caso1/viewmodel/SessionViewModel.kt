package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.SessionManager
import com.example.caso1.data.model.Rol
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager(app)

    val rol = session.rol.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun iniciarSesion(rol: Rol) = viewModelScope.launch { session.guardarRol(rol) }
    fun cerrarSesion() = viewModelScope.launch { session.cerrarSesion() }
}
