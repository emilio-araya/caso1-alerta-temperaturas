package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.repository.GalponRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Datos de la barra de navegación: alertas que aún nadie confirmó (para el contador). */
class NavegacionViewModel(app: Application) : AndroidViewModel(app) {
    val alertasPendientes: StateFlow<Int> = GalponRepository(DatabaseProvider.get(app)).alertasActivas
        .map { lista -> lista.count { it.confirmadaPor == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
