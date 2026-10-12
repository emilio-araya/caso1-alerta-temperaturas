package com.example.caso1.work

import android.content.Context
import androidx.work.*
import com.example.caso1.AlertaTemperaturasApp
import com.example.caso1.data.repository.ResultadoSincronizacion
import java.util.concurrent.TimeUnit

/**
 * Consulta la API en segundo plano (aunque la app esté cerrada), guarda en Room
 * y notifica las alertas críticas NUEVAS.
 */
class AlertasWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val contenedor = (applicationContext as AlertaTemperaturasApp).contenedor
        // El Sincronizador ya notifica las alertas críticas nuevas
        return when (contenedor.sincronizador.sincronizar()) {
            is ResultadoSincronizacion.Correcto -> Result.success()
            // WorkManager lo reintentará más tarde (con espera creciente), cuando haya red
            ResultadoSincronizacion.SinConexion -> Result.retry()
        }
    }

    companion object {
        private const val TRABAJO_PERIODICO = "monitoreo_periodico"
        private const val TRABAJO_DEMO = "monitoreo_demo"

        /** Solo se ejecuta con conexión: sin red, Android espera a que vuelva en vez de gastar batería. */
        private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /**
         * Revisión cada 15 min (el mínimo de Android). UPDATE: conserva el calendario
         * del trabajo existente pero aplica la configuración nueva (las restricciones de red).
         */
        fun programar(context: Context) {
            val peticion = PeriodicWorkRequestBuilder<AlertasWorker>(15, TimeUnit.MINUTES)
                .setConstraints(conRed)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.UPDATE, peticion)
        }

        /** Modo demo: revisa dentro de [segundos], para poder cerrar la app y ver llegar la notificación. */
        fun programarRevision(context: Context, segundos: Long = 10) {
            val peticion = OneTimeWorkRequestBuilder<AlertasWorker>()
                .setInitialDelay(segundos, TimeUnit.SECONDS)
                .setConstraints(conRed)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(TRABAJO_DEMO, ExistingWorkPolicy.REPLACE, peticion)
        }
    }
}
