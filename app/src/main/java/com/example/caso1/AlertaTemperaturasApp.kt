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
import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.local.RepositorioUmbrales
import com.example.caso1.data.local.UmbralesRepository
import com.example.caso1.data.local.almacenUmbrales
import com.example.caso1.data.local.datosMonitoreo
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.data.repository.RepositorioGalpones
import com.example.caso1.notifications.NotificationHelper
import com.example.caso1.notifications.debeNotificar
import com.example.caso1.work.AlertasWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

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

    /** Umbrales configurables (DataStore "umbrales"); el repositorio los usa para evaluar cada galpón. */
    val umbrales: RepositorioUmbrales by lazy { UmbralesRepository(app.almacenUmbrales) }

    val repositorio: RepositorioGalpones by lazy { GalponRepository(app.datosMonitoreo, api, umbrales.umbrales) }
    val sesion: SessionManager by lazy { SessionManager(app) }

    /**
     * Acciones que necesitan Context, envueltas para que el ViewModel no lo conozca.
     * Solo se notifica si la sesión es de operario o supervisor ([debeNotificar]); también en segundo plano.
     */
    val notificarCritico: suspend (AlertaEntity) -> Unit = { alerta ->
        if (debeNotificar(sesion.rol.first())) NotificationHelper.notificarCritico(app, alerta)
    }
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
