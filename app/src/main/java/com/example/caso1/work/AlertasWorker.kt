package com.example.caso1.work

import android.content.Context
import androidx.work.*
import com.example.caso1.data.api.MockMonitorApi
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.notifications.NotificationHelper
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Consulta la API en segundo plano (aunque la app esté cerrada), guarda en Room
 * y notifica las alertas críticas NUEVAS. Es la "simulación en tiempo real" del plan (4.2).
 */
class AlertasWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val repo = GalponRepository(DatabaseProvider.get(applicationContext))
        repo.refrescar().forEach { NotificationHelper.notificarCritico(applicationContext, it) }
        Result.success()
    } catch (e: CancellationException) {
        throw e   // WorkManager detuvo el trabajo: no es un error que deba reintentarse
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val TRABAJO_PERIODICO = "monitoreo_periodico"
        private const val TRABAJO_DEMO = "monitoreo_demo"

        /** Revisión cada 15 min (el mínimo de Android). KEEP: no se reprograma en cada arranque. */
        fun programar(context: Context) {
            // Con una API real se añadiría: Constraints(requiredNetworkType = NetworkType.CONNECTED)
            val peticion = PeriodicWorkRequestBuilder<AlertasWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, peticion)
        }

        /**
         * Modo demo: fuerza un evento crítico y lanza una revisión dentro de [segundos],
         * para poder cerrar la app y ver llegar la notificación. Devuelve el galpón afectado.
         */
        fun simularEventoCritico(context: Context, segundos: Long = 10): Int {
            val galponId = MockMonitorApi.simularEventoCritico()
            val peticion = OneTimeWorkRequestBuilder<AlertasWorker>()
                .setInitialDelay(segundos, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(TRABAJO_DEMO, ExistingWorkPolicy.REPLACE, peticion)
            return galponId
        }
    }
}
