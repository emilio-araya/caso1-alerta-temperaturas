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

    /** Dirección del sistema de monitoreo. Con un servidor real, solo cambia esta línea. */
    const val URL_BASE = "https://monitoreo.galpones.example/api/v1/"

    private val json = Json { ignoreUnknownKeys = true }   // tolera campos nuevos del servidor

    fun crear(servidor: Interceptor?): MonitorApi {
        val http = OkHttpClient.Builder()
            // Conectividad limitada: no esperar para siempre a una red que no responde
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .apply { servidor?.let(::addInterceptor) }
            .build()

        return Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(http)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MonitorApi::class.java)
    }
}
