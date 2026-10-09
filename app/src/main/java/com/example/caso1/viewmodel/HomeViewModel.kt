package com.example.caso1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.notifications.NotificationHelper
import com.example.caso1.work.AlertasWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/** La franja superior de Home: el peor estado y el galpón que lo provoca. */
data class Boletin(
    val estado: EstadoGalpon,
    val masGrave: GalponEntity?,
    val medicionMasGrave: MedicionEntity?,
    val afectados: Int,
    val total: Int,
    val actualizado: Long?
)

data class HomeUiState(
    val cargando: Boolean = true,
    val galpones: List<GalponEntity> = emptyList(),
    val ultimaPorGalpon: Map<Int, MedicionEntity> = emptyMap(),
    val alertasActivas: Int = 0,
    val alertasHoy: Int = 0,
    val boletin: Boletin? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GalponRepository(DatabaseProvider.get(app))

    val uiState: StateFlow<HomeUiState> = combine(
        repo.galpones, repo.ultimasMediciones, repo.alertasActivas, repo.alertas
    ) { galpones, ultimas, activas, todas ->
        val hoy = inicioDeHoy()
        val porGalpon = ultimas.associateBy { it.galponId }
        HomeUiState(
            cargando = galpones.isEmpty(),
            galpones = galpones,
            ultimaPorGalpon = porGalpon,
            alertasActivas = activas.size,
            alertasHoy = todas.count { it.fechaHora >= hoy },
            boletin = armarBoletin(galpones, porGalpon)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Mensajes de una sola vez para el Snackbar. */
    private val _mensajes = MutableSharedFlow<String>()
    val mensajes: SharedFlow<String> = _mensajes

    init { refrescar() }

    fun refrescar() {
        viewModelScope.launch {
            repo.refrescar().forEach { NotificationHelper.notificarCritico(getApplication(), it) }
        }
    }

    fun simularEventoCritico() {
        val id = AlertasWorker.simularEventoCritico(getApplication())
        viewModelScope.launch { _mensajes.emit("Pico de calor simulado en el Galpón $id. La alerta llega en 10 s, aunque cierres la app.") }
    }

    private fun armarBoletin(galpones: List<GalponEntity>, ultimas: Map<Int, MedicionEntity>): Boletin? {
        if (galpones.isEmpty()) return null
        val peor = galpones.maxOf { it.estado }
        // Entre los del peor estado, el más caluroso
        val masGrave = galpones.filter { it.estado == peor }.maxByOrNull { ultimas[it.id]?.temperatura ?: 0.0 }
        return Boletin(
            estado = peor,
            masGrave = masGrave.takeIf { peor != EstadoGalpon.NORMAL },
            medicionMasGrave = masGrave?.let { ultimas[it.id] },
            afectados = galpones.count { it.estado != EstadoGalpon.NORMAL },
            total = galpones.size,
            actualizado = ultimas.values.maxOfOrNull { it.fechaHora }
        )
    }

    private fun inicioDeHoy(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
