package com.example.caso1.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Umbrales
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class DetalleUiState(
    val galponId: Int = 0,
    val galpon: GalponEntity? = null,
    val mediciones: List<MedicionEntity> = emptyList(),   // de la más reciente a la más antigua
    val estado: EstadoGalpon? = null
) {
    val ultima: MedicionEntity? get() = mediciones.firstOrNull()
}

/**
 * El galpón llega por el argumento de navegación "galponId" (SavedStateHandle).
 * En pantallas expandidas el panel de detalle vive dentro de Home y cambia de
 * galpón con [seleccionar].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViewModel(
    private val repo: RepositorioGalpones,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val galponId = savedState.getStateFlow(CLAVE_GALPON, 0)

    val uiState: StateFlow<DetalleUiState> = galponId
        .flatMapLatest { id ->
            combine(repo.mediciones(id), repo.galpones) { lista, galpones ->
                DetalleUiState(
                    galponId = id,
                    galpon = galpones.firstOrNull { it.id == id },
                    mediciones = lista,
                    estado = lista.firstOrNull()?.let { Umbrales.evaluar(it.temperatura, it.humedad) }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    fun seleccionar(id: Int) { savedState[CLAVE_GALPON] = id }

    companion object {
        private const val CLAVE_GALPON = "galponId"

        val Factory = viewModelFactory {
            initializer { DetalleViewModel(contenedor().repositorio, createSavedStateHandle()) }
        }
    }
}
