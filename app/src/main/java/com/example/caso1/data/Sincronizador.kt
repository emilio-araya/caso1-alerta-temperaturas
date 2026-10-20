package com.example.caso1.data

import com.example.caso1.data.local.AlertaEntity
import com.example.caso1.data.repository.RepositorioGalpones
import com.example.caso1.data.repository.ResultadoSincronizacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Único punto que sincroniza con la API: lo usan la pantalla de inicio (deslizar
 * para actualizar), la barra de navegación (reintentar / vuelve la red) y el
 * trabajo en segundo plano. Notifica solo las alertas críticas nuevas.
 */
class Sincronizador(
    private val repo: RepositorioGalpones,
    private val notificarCritico: (AlertaEntity) -> Unit
) {
    private val mutex = Mutex()
    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando

    /** Si ya hay una sincronización en curso, espera a que termine en vez de lanzar otra en paralelo. */
    suspend fun sincronizar(): ResultadoSincronizacion = mutex.withLock {
        _sincronizando.value = true
        try {
            repo.refrescar().also { r ->
                if (r is ResultadoSincronizacion.Correcto) r.nuevasCriticas.forEach(notificarCritico)
            }
        } finally {
            _sincronizando.value = false
        }
    }
}
