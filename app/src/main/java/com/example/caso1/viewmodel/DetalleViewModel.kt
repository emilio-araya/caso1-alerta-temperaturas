package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Umbrales
import com.example.caso1.data.repository.GalponRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DetalleUiState(
    val mediciones: List<MedicionEntity> = emptyList(),   // de la más reciente a la más antigua
    val estado: EstadoGalpon? = null
) {
    val ultima: MedicionEntity? get() = mediciones.firstOrNull()
}

/** El id del galpón llega por el argumento de navegación "galponId" (SavedStateHandle). */
class DetalleViewModel(app: Application, savedState: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = GalponRepository(DatabaseProvider.get(app))

    val galponId: Int = savedState["galponId"] ?: 0

    val uiState: StateFlow<DetalleUiState> = repo.mediciones(galponId)
        .map { lista ->
            DetalleUiState(
                mediciones = lista,
                estado = lista.firstOrNull()?.let { Umbrales.evaluar(it.temperatura, it.humedad) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())
}
