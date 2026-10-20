package com.example.caso1.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.R
import com.example.caso1.contenedor
import com.example.caso1.data.Sincronizador
import com.example.caso1.data.local.RepositorioUmbrales
import com.example.caso1.data.model.Umbrales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UmbralesUiState(
    val textos: Map<CampoUmbral, String> = emptyMap(),
    val errores: Map<CampoUmbral, Int> = emptyMap(),   // id del mensaje en strings.xml
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    @param:StringRes val mensaje: Int? = null           // confirmación para el Snackbar
)

/**
 * Formulario "Configurar umbrales". Lo escrito vive en memoria (en [state]); DataStore
 * se lee una sola vez al abrir y se escribe solo al guardar datos válidos.
 */
class UmbralesViewModel(
    private val repo: RepositorioUmbrales,
    private val sincronizador: Sincronizador
) : ViewModel() {

    private val _state = MutableStateFlow(UmbralesUiState())
    val state: StateFlow<UmbralesUiState> = _state

    init {
        viewModelScope.launch {
            val guardados = repo.umbrales.first()
            _state.value = _state.value.copy(textos = textosDe(guardados), cargando = false)
        }
    }

    fun onCambio(campo: CampoUmbral, valor: String) {
        val s = _state.value
        _state.value = s.copy(textos = s.textos + (campo to valor), errores = s.errores - campo)
    }

    fun guardar() {
        val s = _state.value
        if (s.guardando || s.cargando) return   // evita guardar dos veces con un doble toque
        val errores = validarUmbrales(s.textos)
        if (errores.isNotEmpty()) {
            _state.value = s.copy(errores = errores)
            return
        }
        _state.value = s.copy(guardando = true)
        viewModelScope.launch {
            repo.guardar(configDe(s.textos))
            // Los galpones se recalculan con los umbrales nuevos (sin esperar a la red)
            sincronizador.reevaluar()
            _state.value = _state.value.copy(guardando = false, mensaje = R.string.umbrales_guardados)
        }
    }

    fun restaurarPredeterminados() {
        if (_state.value.guardando) return
        _state.value = _state.value.copy(guardando = true)
        viewModelScope.launch {
            repo.restaurar()
            sincronizador.reevaluar()
            _state.value = UmbralesUiState(
                textos = textosDe(Umbrales.PREDETERMINADOS), cargando = false, mensaje = R.string.umbrales_restaurados
            )
        }
    }

    /** La UI ya mostró el mensaje. */
    fun mensajeMostrado() { _state.value = _state.value.copy(mensaje = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                UmbralesViewModel(c.umbrales, c.sincronizador)
            }
        }
    }
}
