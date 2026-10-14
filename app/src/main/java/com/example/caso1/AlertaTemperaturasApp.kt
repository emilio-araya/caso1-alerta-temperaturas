package com.example.caso1

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.caso1.data.MonitorConectividad
import com.example.caso1.data.SessionManager
import com.example.caso1.data.Sincronizador
import com.example.caso1.data.api.ClienteApi
import com.example.caso1.data.api.InterceptorConectividad
import com.example.caso1.data.api.MonitorApi
import com.example.caso1.data.api.ServidorSimulado
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.data.repository.RepositorioGalpones
import com.example.caso1.notifications.NotificationHelper
import com.example.caso1.work.AlertasWorker
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Inyección de dependencias manual: cada dependencia se crea UNA vez aquí y se
 * comparte. Los ViewModels las reciben por constructor, así en los tests se
 * pueden reemplazar por versiones falsas.
 */
class ContenedorApp(contexto: Context) {
    private val app = contexto.applicationContext

    val conectividad: MonitorConectividad by lazy { MonitorConectividad(app) }

    /** Modo demo: simula que el servidor no responde, para mostrar el funcionamiento sin conexión. */
    val sinConexionSimulada = MutableStateFlow(false)

    /**
     * Cliente HTTP (Retrofit) del sistema de monitoreo: json-server en el PC por defecto,
     * o el servidor simulado interno si se compiló con caso1.apiSimulada=true.
     */
    private val api: MonitorApi by lazy {
        if (BuildConfig.API_SIMULADA) {
            ClienteApi.crear(ServidorSimulado(hayRed = conectividad::hayRed, sinConexionForzada = { sinConexionSimulada.value }))
        } else {
            ClienteApi.crear(
                InterceptorConectividad(hayRed = conectividad::hayRed, sinConexionForzada = { sinConexionSimulada.value }),
                urlBase = BuildConfig.API_URL_BASE
            )
        }
    }

    val repositorio: RepositorioGalpones by lazy { GalponRepository(DatabaseProvider.get(app), api) }
    val sesion: SessionManager by lazy { SessionManager(app) }

    /** Acciones que necesitan Context, envueltas para que el ViewModel no lo conozca. */
    val notificarCritico: (AlertaEntity) -> Unit = { NotificationHelper.notificarCritico(app, it) }
    val programarRevisionDemo: () -> Unit = { AlertasWorker.programarRevision(app) }

    val sincronizador: Sincronizador by lazy { Sincronizador(repositorio, notificarCritico) }
}

class AlertaTemperaturasApp : Application() {
    lateinit var contenedor: ContenedorApp
        private set

    override fun onCreate() {
        super.onCreate()
        // The channel must exist before any notification, including those posted by WorkManager
        NotificationHelper.crearCanal(this)
        contenedor = ContenedorApp(this)
    }
}

/** Atajo para las fábricas de ViewModel: `initializer { HomeViewModel(contenedor().repositorio) }`. */
fun CreationExtras.contenedor(): ContenedorApp =
    (this[APPLICATION_KEY] as AlertaTemperaturasApp).contenedor
