package com.example.caso1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caso1.contenedor
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.model.Rol
import com.example.caso1.data.repository.RepositorioGalpones
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lo que la pantalla de Alertas necesita, y nada más. La UI no conoce AlertaEntity:
 * si cambia la base de datos, solo cambia [aAlertaUi].
 */
data class AlertaUi(
    val id: Int,
    val galponId: Int,
    val granja: String?,      // null si el galpón aún no está en la base de datos
    val galpon: String?,
    val tipo: String,         // código de la causa, p. ej. "TEMPERATURA_ALTA"
    val nivel: NivelAlerta,
    val desde: Long,
    val confirmadaPor: Rol?,
    val confirmadaEn: Long?
) {
    val confirmada: Boolean get() = confirmadaPor != null
}

fun AlertaEntity.aAlertaUi(galpon: GalponEntity?) = AlertaUi(
    id = id,
    galponId = galponId,
    granja = galpon?.granja,
    galpon = galpon?.nombre,
    tipo = tipo,
    nivel = nivel,
    desde = fechaHora,
    confirmadaPor = confirmadaPor?.let { runCatching { Rol.valueOf(it) }.getOrNull() },
    confirmadaEn = confirmadaEn
)

data class AlertasUiState(
    val alertas: List<AlertaUi> = emptyList()   // pendientes primero, y dentro de ellas las críticas
) {
    val pendientes: Int get() = alertas.count { !it.confirmada }
}

class AlertasViewModel(private val repo: RepositorioGalpones) : ViewModel() {

    val uiState: StateFlow<AlertasUiState> = combine(repo.alertasActivas, repo.galpones) { alertas, galpones ->
        val porId = galpones.associateBy { it.id }
        AlertasUiState(
            alertas = alertas
                .map { it.aAlertaUi(porId[it.galponId]) }
                .sortedWith(compareBy({ it.confirmada }, { it.nivel != NivelAlerta.CRITICO }))
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertasUiState())

    /** Operario y supervisor confirman; jefatura solo consulta (punto 3.1 del caso). */
    fun puedeConfirmar(rol: Rol?): Boolean = rol == Rol.OPERARIO || rol == Rol.SUPERVISOR

    /** [detalle] es el texto legible que queda en el historial, armado por la UI desde strings.xml. */
    fun confirmar(alerta: AlertaUi, rol: Rol, detalle: String) {
        if (!puedeConfirmar(rol)) return
        viewModelScope.launch { repo.confirmarAlerta(alerta.id, alerta.galponId, rol, detalle) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AlertasViewModel(contenedor().repositorio) }
        }
    }
}
