package com.example.caso1.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.local.MedicionEntity
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Umbrales
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class DetalleUiState(
    val galponId: Int = 0,
    val galpon: GalponEntity? = null,
    val mediciones: List<MedicionEntity> = emptyList(),   // de la más reciente a la más antigua
    val medicionesGrafico: List<MedicionEntity> = emptyList(),   // del período elegido, cronológicas
    val periodo: PeriodoGrafico = PeriodoGrafico.SEIS_HORAS,
    val estado: EstadoGalpon? = null,
    val umbrales: ConfigUmbrales = Umbrales.PREDETERMINADOS   // los configurados, para causa, colores y gráficos
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
    private val savedState: SavedStateHandle,
    private val umbrales: Flow<ConfigUmbrales> = flowOf(Umbrales.PREDETERMINADOS),
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val galponId = savedState.getStateFlow(CLAVE_GALPON, 0)
    private val periodo = MutableStateFlow(PeriodoGrafico.SEIS_HORAS)

    val uiState: StateFlow<DetalleUiState> = galponId
        .flatMapLatest { id ->
            combine(repo.mediciones(id), repo.galpones, umbrales, periodo) { lista, galpones, config, p ->
                DetalleUiState(
                    galponId = id,
                    galpon = galpones.firstOrNull { it.id == id },
                    mediciones = lista,
                    // Respuesta 11: el gráfico cubre el período elegido; la tabla sigue completa
                    medicionesGrafico = filtrarPorPeriodo(lista, reloj(), p).asReversed(),
                    periodo = p,
                    estado = lista.firstOrNull()?.let { config.evaluar(it.temperatura, it.humedad) },
                    umbrales = config
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    fun seleccionar(id: Int) { savedState[CLAVE_GALPON] = id }

    fun cambiarPeriodo(p: PeriodoGrafico) { periodo.value = p }

    companion object {
        private const val CLAVE_GALPON = "galponId"

        val Factory = viewModelFactory {
            initializer { DetalleViewModel(contenedor().repositorio, createSavedStateHandle(), contenedor().umbrales.umbrales) }
        }
    }
}
