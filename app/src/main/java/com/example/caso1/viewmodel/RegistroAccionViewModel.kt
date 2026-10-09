package com.example.caso1.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.R
import com.example.caso1.contenedor
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Tipos de acción. El texto visible de cada uno está en strings.xml (accion_tipo_*). */
enum class TipoAccion(@param:StringRes val etiqueta: Int) {
    VENTILACION(R.string.accion_tipo_ventilacion),
    REVISION_EQUIPOS(R.string.accion_tipo_revision),
    NOTIFICACION_SUPERVISOR(R.string.accion_tipo_notificacion),
    OTRO(R.string.accion_tipo_otro)
}

const val MIN_DESCRIPCION = 5

/** Cada error es el id del mensaje en strings.xml, o null si el campo está bien. */
data class ErroresRegistro(
    @param:StringRes val galpon: Int? = null,
    @param:StringRes val tipo: Int? = null,
    @param:StringRes val descripcion: Int? = null
) {
    val hayErrores: Boolean get() = galpon != null || tipo != null || descripcion != null
}

/**
 * Reglas del formulario, sin dependencias de Android para poder probarlas con JUnit.
 * El galpón debe existir: antes se podía escribir cualquier número (p. ej. 99).
 */
fun validarRegistro(galponId: Int?, galponesExistentes: Set<Int>, tipo: TipoAccion?, descripcion: String) = ErroresRegistro(
    galpon = if (galponId == null || galponId !in galponesExistentes) R.string.registro_error_galpon else null,
    tipo = if (tipo == null) R.string.registro_error_tipo else null,
    descripcion = if (descripcion.trim().length < MIN_DESCRIPCION) R.string.registro_error_descripcion else null
)

/** Lo que se guardó, para mostrarlo en la pantalla de confirmación. */
data class AccionRegistrada(
    val granja: String,
    val galpon: String,
    val tipo: TipoAccion,
    val descripcion: String,
    val fechaHora: Long
)

data class RegistroAccionState(
    val galponId: Int? = null,
    val tipoAccion: TipoAccion? = null,
    val descripcion: String = "",
    val errores: ErroresRegistro = ErroresRegistro(),
    val guardando: Boolean = false,
    val registrado: Boolean = false,
    val ultimoRegistro: AccionRegistrada? = null
)

class RegistroAccionViewModel(
    private val repo: RepositorioGalpones,
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {

    val galpones: StateFlow<List<GalponEntity>> =
        repo.galpones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(RegistroAccionState())
    val state: StateFlow<RegistroAccionState> = _state

    fun onGalponChange(id: Int) {
        _state.value = _state.value.copy(galponId = id, errores = _state.value.errores.copy(galpon = null))
    }

    fun onTipoChange(tipo: TipoAccion) {
        _state.value = _state.value.copy(tipoAccion = tipo, errores = _state.value.errores.copy(tipo = null))
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
        if (s.guardando) return   // evita guardar dos veces con un doble toque
        _state.value = s.copy(guardando = true)
        viewModelScope.launch {
            // Se leen los galpones del repositorio (no de `galpones.value`, que está vacío si nadie lo observa)
            val existentes = repo.galpones.first()
            val errores = validarRegistro(s.galponId, existentes.map { it.id }.toSet(), s.tipoAccion, s.descripcion)
            val galpon = existentes.firstOrNull { it.id == s.galponId }
            val tipo = s.tipoAccion
            if (errores.hayErrores || galpon == null || tipo == null) {
                _state.value = s.copy(errores = errores, guardando = false)
                return@launch
            }
            val descripcion = s.descripcion.trim()
            repo.registrarAccion(galpon.id, tipo.name, descripcion)
            // El formulario queda limpio: volver atrás no permite guardar la misma acción otra vez
            _state.value = RegistroAccionState(
                registrado = true,
                ultimoRegistro = AccionRegistrada(galpon.granja, galpon.nombre, tipo, descripcion, reloj())
            )
        }
    }

    /** La UI ya navegó a la confirmación. */
    fun navegacionRealizada() { _state.value = _state.value.copy(registrado = false) }

    companion object {
        val Factory = viewModelFactory {
            initializer { RegistroAccionViewModel(contenedor().repositorio) }
        }
    }
}
