package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.db.EventoEntity
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.repository.GalponRepository
import kotlinx.coroutines.flow.*

data class HistorialUiState(
    val galpones: List<GalponEntity> = emptyList(),
    val nombres: Map<Int, String> = emptyMap(),
    val filtroGalpon: Int? = null,
    val alertas: List<AlertaEntity> = emptyList(),   // ya filtradas
    val eventos: List<EventoEntity> = emptyList()    // ya filtrados
)

class HistorialViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GalponRepository(DatabaseProvider.get(app))
    private val filtro = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<HistorialUiState> = combine(
        repo.galpones, repo.alertas, repo.eventos, filtro
    ) { galpones, alertas, eventos, f ->
        HistorialUiState(
            galpones = galpones,
            nombres = galpones.associate { it.id to "${it.granja} · ${it.nombre}" },
            filtroGalpon = f,
            alertas = alertas.filter { f == null || it.galponId == f },
            eventos = eventos.filter { f == null || it.galponId == f }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    /** null = todos los galpones. */
    fun filtrarPorGalpon(galponId: Int?) { filtro.value = galponId }
}
