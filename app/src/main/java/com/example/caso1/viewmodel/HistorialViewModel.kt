package com.example.caso1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.EventoEntity
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.flow.*

data class HistorialUiState(
    val galpones: List<GalponEntity> = emptyList(),
    val galponesPorId: Map<Int, GalponEntity> = emptyMap(),
    val filtro: FiltroHistorial = FiltroHistorial(),
    val tiposDisponibles: List<String> = emptyList(),   // tipos de alerta ya guardados
    val alertas: List<AlertaEntity> = emptyList(),      // ya filtradas
    val eventos: List<EventoEntity> = emptyList()       // ya filtrados
)

class HistorialViewModel(
    private val repo: RepositorioGalpones,
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroHistorial())

    val uiState: StateFlow<HistorialUiState> = combine(
        repo.galpones, repo.alertas, repo.eventos, filtro
    ) { galpones, alertas, eventos, f ->
        val ahora = reloj()
        HistorialUiState(
            galpones = galpones,
            galponesPorId = galpones.associateBy { it.id },
            filtro = f,
            tiposDisponibles = alertas.map { it.tipo }.distinct(),
            alertas = filtrarAlertas(alertas, f, ahora),
            eventos = filtrarEventos(eventos, f, ahora)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    fun aplicarFiltro(filtro: FiltroHistorial) { this.filtro.value = filtro }

    companion object {
        val Factory = viewModelFactory {
            initializer { HistorialViewModel(contenedor().repositorio) }
        }
    }
}
