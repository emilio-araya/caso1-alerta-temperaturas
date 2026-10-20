package com.example.caso1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
import com.example.caso1.data.Sincronizador
import com.example.caso1.data.repository.EstadoConexion
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la conexión con el sistema de monitoreo, para el aviso "Sin conexión". */
data class EstadoRed(
    val conexion: EstadoConexion = EstadoConexion.SIN_DATOS,
    val ultimaActualizacion: Long? = null,   // lectura más reciente guardada en DataStore
    val sincronizando: Boolean = false
)

/**
 * Vive mientras la app está abierta (a nivel de la navegación): sincroniza al
 * abrir, vuelve a sincronizar cuando regresa la red y entrega a la barra de
 * navegación el contador de alertas y el estado de la conexión.
 */
class NavegacionViewModel(
    repo: RepositorioGalpones,
    private val sincronizador: Sincronizador,
    cambiosDeRed: Flow<Boolean>
) : ViewModel() {

    val alertasPendientes: StateFlow<Int> = repo.alertasActivas
        .map { lista -> lista.count { it.confirmadaPor == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Lo que dice Android sobre la red del teléfono, compartido por todos los que lo observan. */
    private val hayRed: StateFlow<Boolean> = cambiosDeRed.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val red: StateFlow<EstadoRed> = combine(
        repo.estadoConexion, repo.ultimasMediciones, sincronizador.sincronizando, hayRed
    ) { conexion, ultimas, sincronizando, telefonoConRed ->
        EstadoRed(
            // Si el teléfono perdió la red, se avisa de inmediato, sin esperar a que falle una sincronización
            conexion = if (!telefonoConRed) EstadoConexion.SIN_CONEXION else conexion,
            ultimaActualizacion = ultimas.maxOfOrNull { it.fechaHora },
            sincronizando = sincronizando
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoRed())

    init {
        reintentar()
        viewModelScope.launch {
            // Conectividad intermitente: al volver la red, actualizar solo si la última vez falló
            hayRed.drop(1)
                .filter { conRed -> conRed && repo.estadoConexion.value != EstadoConexion.EN_LINEA }
                .collect { sincronizador.sincronizar() }
        }
    }

    fun reintentar() {
        viewModelScope.launch { sincronizador.sincronizar() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                NavegacionViewModel(c.repositorio, c.sincronizador, c.conectividad.cambios)
            }
        }
    }
}
