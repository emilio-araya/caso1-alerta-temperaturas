package com.example.caso1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Datos de la barra de navegación: alertas que aún nadie confirmó (para el contador). */
class NavegacionViewModel(repo: RepositorioGalpones) : ViewModel() {
    val alertasPendientes: StateFlow<Int> = repo.alertasActivas
        .map { lista -> lista.count { it.confirmadaPor == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        val Factory = viewModelFactory {
            initializer { NavegacionViewModel(contenedor().repositorio) }
        }
    }
}
