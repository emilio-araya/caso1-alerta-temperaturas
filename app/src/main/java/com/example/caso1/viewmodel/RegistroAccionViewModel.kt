package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.repository.GalponRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegistroAccionState(
    val galponId: String = "",
    val tipoAccion: String = "",
    val descripcion: String = "",
    val errorGalpon: String? = null,
    val errorTipo: String? = null,
    val errorDescripcion: String? = null,
    val registrado: Boolean = false
)

class RegistroAccionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GalponRepository(DatabaseProvider.get(app))

    private val _state = MutableStateFlow(RegistroAccionState())
    val state: StateFlow<RegistroAccionState> = _state

    fun onGalponIdChange(v: String) { _state.value = _state.value.copy(galponId = v, errorGalpon = null, registrado = false) }
    fun onTipoChange(v: String) { _state.value = _state.value.copy(tipoAccion = v, errorTipo = null, registrado = false) }
    fun onDescripcionChange(v: String) { _state.value = _state.value.copy(descripcion = v, errorDescripcion = null, registrado = false) }

    fun registrar() {
        val s = _state.value
        val id = s.galponId.toIntOrNull()
        val eGalpon = if (id == null || id <= 0) "Ingresa un ID de galpón válido" else null
        val eTipo = if (s.tipoAccion.isBlank()) "Selecciona un tipo de acción" else null
        val eDesc = if (s.descripcion.trim().length < 5) "Describe la acción (mín. 5 caracteres)" else null

        if (eGalpon != null || eTipo != null || eDesc != null) {
            _state.value = s.copy(errorGalpon = eGalpon, errorTipo = eTipo, errorDescripcion = eDesc)
            return
        }
        viewModelScope.launch {
            repo.registrarAccion(id!!, s.tipoAccion, s.descripcion.trim())
            _state.value = s.copy(registrado = true)
        }
    }

    fun limpiar() { _state.value = RegistroAccionState() }
}
