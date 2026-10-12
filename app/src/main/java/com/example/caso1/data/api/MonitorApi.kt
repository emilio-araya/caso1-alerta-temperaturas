package com.example.caso1.data.api

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints del sistema de monitoreo (punto 3.4 del caso), consumidos con Retrofit.
 * Cada función es una petición HTTP; Retrofit convierte la respuesta JSON en los DTO.
 */
interface MonitorApi {

    @GET("galpones")
    suspend fun galpones(): List<GalponDto>

    @GET("galpones/{id}/mediciones")
    suspend fun mediciones(@Path("id") galponId: Int): List<MedicionDto>

    @GET("alertas/activas")
    suspend fun alertasActivas(): List<AlertaDto>

    /** Solo para la demo: el servidor simulado fuerza un pico de calor en un galpón. */
    @POST("simulaciones/pico-calor")
    suspend fun simularPicoDeCalor(): SimulacionDto
}
