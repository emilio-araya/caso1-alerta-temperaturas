package com.example.caso1.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Corta la petición antes de salir a la red cuando el teléfono no tiene conexión
 * o cuando está activo el modo demo "sin conexión". Así el repositorio recibe el
 * mismo error que daría una red caída y la app sigue mostrando los datos guardados en DataStore.
 */
class InterceptorConectividad(
    private val hayRed: () -> Boolean,
    private val sinConexionForzada: () -> Boolean = { false }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!hayRed() || sinConexionForzada()) throw IOException("Sin conexión con el sistema de monitoreo")
        return chain.proceed(chain.request())
    }
}
