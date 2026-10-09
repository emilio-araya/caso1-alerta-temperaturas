package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.model.Rol
import com.example.caso1.data.repository.GalponRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlertasUiState(
    val alertas: List<AlertaEntity> = emptyList(),   // pendientes primero, y dentro de ellas las críticas
    val nombres: Map<Int, String> = emptyMap()
)

class AlertasViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GalponRepository(DatabaseProvider.get(app))

    val uiState: StateFlow<AlertasUiState> = combine(repo.alertasActivas, repo.galpones) { alertas, galpones ->
        AlertasUiState(
            alertas = alertas.sortedWith(compareBy({ it.confirmadaPor != null }, { it.nivel != NivelAlerta.CRITICO })),
            nombres = galpones.associate { it.id to "${it.granja} · ${it.nombre}" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertasUiState())

    /** Operario y supervisor confirman; jefatura solo consulta (punto 3.1 del caso). */
    fun puedeConfirmar(rol: Rol?): Boolean = rol == Rol.OPERARIO || rol == Rol.SUPERVISOR

    fun confirmar(alerta: AlertaEntity, rol: Rol, detalle: String) {
        if (!puedeConfirmar(rol)) return
        viewModelScope.launch { repo.confirmarAlerta(alerta, rol, detalle) }
    }
}
