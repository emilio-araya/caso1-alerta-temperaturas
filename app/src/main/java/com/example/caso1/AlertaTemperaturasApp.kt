package com.example.caso1

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.caso1.data.SessionManager
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.data.repository.RepositorioGalpones
import com.example.caso1.notifications.NotificationHelper
import com.example.caso1.work.AlertasWorker

/**
 * Inyección de dependencias manual: cada dependencia se crea UNA vez aquí y se
 * comparte. Los ViewModels las reciben por constructor, así en los tests se
 * pueden reemplazar por versiones falsas.
 */
class ContenedorApp(contexto: Context) {
    private val app = contexto.applicationContext

    val repositorio: RepositorioGalpones by lazy { GalponRepository(DatabaseProvider.get(app)) }
    val sesion: SessionManager by lazy { SessionManager(app) }

    /** Acciones que necesitan Context, envueltas para que el ViewModel no lo conozca. */
    val notificarCritico: (AlertaEntity) -> Unit = { NotificationHelper.notificarCritico(app, it) }
    val simularEventoCritico: () -> Int = { AlertasWorker.simularEventoCritico(app) }
}

class AlertaTemperaturasApp : Application() {
    lateinit var contenedor: ContenedorApp
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = ContenedorApp(this)
    }
}

/** Atajo para las fábricas de ViewModel: `initializer { HomeViewModel(contenedor().repositorio) }`. */
fun CreationExtras.contenedor(): ContenedorApp =
    (this[APPLICATION_KEY] as AlertaTemperaturasApp).contenedor
