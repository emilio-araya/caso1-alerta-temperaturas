package com.example.caso1.data

import com.example.caso1.data.model.Rol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Una lectura simulada por minuto (respuesta 10 del docente). */
const val INTERVALO_MINUTO_MS = 60_000L

/** Solo se simula con una sesión iniciada: sin rol la app está en el login. */
fun debeSimular(rol: Rol?): Boolean = rol != null

/**
 * Genera mediciones automáticas mientras el proceso de la app está vivo. WorkManager no sirve
 * para esto: su intervalo periódico mínimo es de 15 minutos. Por eso el ciclo de un minuto corre
 * en un [CoroutineScope] de la aplicación (MVP académico, sin servicio en segundo plano).
 */
class SimuladorAutomatico(
    private val sincronizador: Sincronizador,
    private val sesion: SessionManager,
    private val intervaloMs: Long = INTERVALO_MINUTO_MS
) {
    fun iniciar(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            delay(intervaloMs)
            if (debeSimular(sesion.rol.first())) sincronizador.generarMedicionesAutomaticas()
        }
    }
}
