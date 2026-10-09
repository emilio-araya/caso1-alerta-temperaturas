package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.repository.GalponRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val TIPOS_ACCION = listOf("Ventilación", "Revisión de equipos", "Notificación a supervisor", "Otro")
const val MIN_DESCRIPCION = 5

data class ErroresRegistro(
    val galpon: String? = null,
    val tipo: String? = null,
    val descripcion: String? = null
) {
    val hayErrores: Boolean get() = galpon != null || tipo != null || descripcion != null
}

/**
 * Reglas del formulario, sin dependencias de Android para poder probarlas con JUnit.
 * El galpón debe existir: antes se podía escribir cualquier número (p. ej. 99).
 */
fun validarRegistro(galponId: Int?, galponesExistentes: Set<Int>, tipo: String, descripcion: String) = ErroresRegistro(
    galpon = if (galponId == null || galponId !in galponesExistentes) "Elige el galpón donde hiciste la acción" else null,
    tipo = if (tipo !in TIPOS_ACCION) "Elige el tipo de acción" else null,
    descripcion = if (descripcion.trim().length < MIN_DESCRIPCION)
        "Describe qué hiciste (mínimo $MIN_DESCRIPCION caracteres)" else null
)

/** Lo que se guardó, para mostrarlo en la pantalla de confirmación. */
data class AccionRegistrada(val galpon: String, val tipo: String, val descripcion: String, val fechaHora: Long)

data class RegistroAccionState(
    val galponId: Int? = null,
    val tipoAccion: String = "",
    val descripcion: String = "",
    val errores: ErroresRegistro = ErroresRegistro(),
    val guardando: Boolean = false,
    val registrado: Boolean = false,
    val ultimoRegistro: AccionRegistrada? = null
)

class RegistroAccionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GalponRepository(DatabaseProvider.get(app))

    val galpones: StateFlow<List<GalponEntity>> =
        repo.galpones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(RegistroAccionState())
    val state: StateFlow<RegistroAccionState> = _state

    fun onGalponChange(id: Int) {
        _state.value = _state.value.copy(galponId = id, errores = _state.value.errores.copy(galpon = null))
    }

    fun onTipoChange(v: String) {
        _state.value = _state.value.copy(tipoAccion = v, errores = _state.value.errores.copy(tipo = null))
    }

    fun onDescripcionChange(v: String) {
        _state.value = _state.value.copy(descripcion = v, errores = _state.value.errores.copy(descripcion = null))
    }

    /** Al llegar desde el detalle de un galpón, el formulario ya viene con ese galpón elegido. */
    fun preseleccionar(galponId: Int) {
        if (_state.value.galponId == null) onGalponChange(galponId)
    }

    fun registrar() {
        val s = _state.value
        if (s.guardando) return
        val existentes = galpones.value
        val errores = validarRegistro(s.galponId, existentes.map { it.id }.toSet(), s.tipoAccion, s.descripcion)
        if (errores.hayErrores) {
            _state.value = s.copy(errores = errores)
            return
        }
        val id = s.galponId ?: return
        _state.value = s.copy(guardando = true)
        viewModelScope.launch {
            val descripcion = s.descripcion.trim()
            repo.registrarAccion(id, s.tipoAccion, descripcion)
            val galpon = existentes.first { it.id == id }
            // El formulario queda limpio: volver atrás no permite guardar la misma acción otra vez
            _state.value = RegistroAccionState(
                registrado = true,
                ultimoRegistro = AccionRegistrada(
                    galpon = "${galpon.granja} · ${galpon.nombre}",
                    tipo = s.tipoAccion,
                    descripcion = descripcion,
                    fechaHora = System.currentTimeMillis()
                )
            )
        }
    }

    /** La UI ya navegó a la confirmación. */
    fun navegacionRealizada() { _state.value = _state.value.copy(registrado = false) }
}
