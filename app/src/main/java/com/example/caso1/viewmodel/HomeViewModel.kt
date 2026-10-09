package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.notifications.NotificationHelper
import com.example.caso1.work.AlertasWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val cargando: Boolean = true,
    val galpones: List<GalponEntity> = emptyList(),
    val ultimaPorGalpon: Map<Int, MedicionEntity> = emptyMap(),
    val alertasActivas: Int = 0,
    val alertasHoy: Int = 0
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GalponRepository(DatabaseProvider.get(app))

    val uiState: StateFlow<HomeUiState> = combine(
        repo.galpones, repo.ultimasMediciones, repo.alertasActivas, repo.alertas
    ) { galpones, ultimas, activas, todas ->
        val hoy = inicioDeHoy()
        HomeUiState(
            cargando = galpones.isEmpty(),
            galpones = galpones,
            ultimaPorGalpon = ultimas.associateBy { it.galponId },
            alertasActivas = activas.size,
            alertasHoy = todas.count { it.fechaHora >= hoy }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Mensajes de una sola vez para el Snackbar. */
    private val _mensajes = MutableSharedFlow<String>()
    val mensajes: SharedFlow<String> = _mensajes

    init { refrescar() }

    fun refrescar() {
        viewModelScope.launch {
            repo.refrescar().forEach { a ->
                NotificationHelper.notificarCritico(getApplication(), a.galponId, "${a.tipo} en galpón ${a.galponId}")
            }
        }
    }

    fun simularEventoCritico() {
        val id = AlertasWorker.simularEventoCritico(getApplication())
        viewModelScope.launch { _mensajes.emit("Evento crítico en Galpón $id dentro de 10 s. Puedes cerrar la app.") }
    }

    private fun inicioDeHoy(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
