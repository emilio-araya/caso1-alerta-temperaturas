package com.example.caso1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
import com.example.caso1.data.Sincronizador
import com.example.caso1.data.local.GalponEntity
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Umbrales
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimulacionUiState(
    val galponId: Int? = null,
    val temperatura: String = "",
    val humedad: String = "",
    val errores: Map<CampoSimulacion, Int> = emptyMap(),   // id del mensaje en strings.xml
    val umbrales: ConfigUmbrales = Umbrales.PREDETERMINADOS, // los guardados en DataStore, para la vista previa
    val guardando: Boolean = false,
    val resultado: EstadoGalpon? = null                     // estado tras simular, para el Snackbar
) {
    /** Cómo quedaría el galpón con lo escrito; null mientras falte un número válido. */
    val vistaPrevia: EstadoGalpon? get() = estadoSimulado(temperatura, humedad, umbrales)
}

/**
 * Formulario "Simular medición" (respuesta 5 del docente). Lo escrito vive en memoria
 * (en [state]); la medición se guarda en DataStore solo al simular con datos válidos.
 */
class SimulacionViewModel(
    private val repo: RepositorioGalpones,
    private val sincronizador: Sincronizador,
    umbrales: Flow<ConfigUmbrales>
) : ViewModel() {

    val galpones: StateFlow<List<GalponEntity>> =
        repo.galpones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(SimulacionUiState())
    val state: StateFlow<SimulacionUiState> = _state

    init {
        // Si cambian los umbrales, la vista previa se actualiza sin tocar lo escrito
        viewModelScope.launch { umbrales.collect { c -> _state.update { it.copy(umbrales = c) } } }
    }

    fun onGalponChange(id: Int) = _state.update { it.copy(galponId = id, errores = it.errores - CampoSimulacion.GALPON) }

    fun onTemperaturaChange(v: String) = _state.update { it.copy(temperatura = v, errores = it.errores - CampoSimulacion.TEMPERATURA) }

    fun onHumedadChange(v: String) = _state.update { it.copy(humedad = v, errores = it.errores - CampoSimulacion.HUMEDAD) }

    fun simular() {
        val s = _state.value
        if (s.guardando) return   // evita simular dos veces con un doble toque
        _state.value = s.copy(guardando = true, resultado = null)
        viewModelScope.launch {
            val existentes = repo.galpones.first().map { it.id }.toSet()
            val errores = validarSimulacion(s.galponId, existentes, s.temperatura, s.humedad)
            val t = leerNumero(s.temperatura)
            val h = leerNumero(s.humedad)
            if (errores.isNotEmpty() || s.galponId == null || t == null || h == null) {
                _state.update { it.copy(errores = errores, guardando = false) }
                return@launch
            }
            sincronizador.simularMedicion(s.galponId, t, h)
            _state.update { it.copy(guardando = false, resultado = it.umbrales.evaluar(t, h)) }
        }
    }

    /** La UI ya mostró el resultado. */
    fun resultadoMostrado() = _state.update { it.copy(resultado = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                SimulacionViewModel(c.repositorio, c.sincronizador, c.umbrales.umbrales)
            }
        }
    }
}
