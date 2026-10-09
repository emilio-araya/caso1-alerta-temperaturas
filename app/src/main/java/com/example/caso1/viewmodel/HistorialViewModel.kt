package com.example.caso1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.EventoEntity
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.flow.*

data class HistorialUiState(
    val galpones: List<GalponEntity> = emptyList(),
    val galponesPorId: Map<Int, GalponEntity> = emptyMap(),
    val filtroGalpon: Int? = null,
    val alertas: List<AlertaEntity> = emptyList(),   // ya filtradas
    val eventos: List<EventoEntity> = emptyList()    // ya filtrados
)

class HistorialViewModel(private val repo: RepositorioGalpones) : ViewModel() {
    private val filtro = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<HistorialUiState> = combine(
        repo.galpones, repo.alertas, repo.eventos, filtro
    ) { galpones, alertas, eventos, f ->
        HistorialUiState(
            galpones = galpones,
            galponesPorId = galpones.associateBy { it.id },
            filtroGalpon = f,
            alertas = alertas.filter { f == null || it.galponId == f },
            eventos = eventos.filter { f == null || it.galponId == f }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    /** null = todos los galpones. */
    fun filtrarPorGalpon(galponId: Int?) { filtro.value = galponId }

    companion object {
        val Factory = viewModelFactory {
            initializer { HistorialViewModel(contenedor().repositorio) }
        }
    }
}
