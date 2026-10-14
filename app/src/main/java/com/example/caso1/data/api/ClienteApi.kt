package com.example.caso1.data.api

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Construye el cliente Retrofit de [MonitorApi]. */
object ClienteApi {

    /** Dirección usada con el servidor simulado dentro de la app ([ServidorSimulado]). */
    const val URL_BASE = "https://monitoreo.galpones.example/api/v1/"

    private val json = Json { ignoreUnknownKeys = true }   // tolera campos nuevos del servidor

    /**
     * @param interceptores se ejecutan en orden: primero [InterceptorConectividad] (si se usa),
     *   después, opcionalmente, un servidor simulado que responde sin salir a la red.
     * @param urlBase dirección del servidor; con json-server viene de BuildConfig.
     */
    fun crear(vararg interceptores: Interceptor, urlBase: String = URL_BASE): MonitorApi {
        val http = OkHttpClient.Builder()
            // Conectividad limitada: no esperar para siempre a una red que no responde
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .apply { interceptores.forEach(::addInterceptor) }
            .build()

        return Retrofit.Builder()
            .baseUrl(urlBase)
            .client(http)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MonitorApi::class.java)
    }
}
