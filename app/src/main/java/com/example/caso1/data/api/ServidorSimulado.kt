package com.example.caso1.data.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * Servidor del sistema de monitoreo, simulado (punto 3.4: "API y datos simulados").
 *
 * Es un Interceptor de OkHttp: recibe la petición HTTP que arma Retrofit, la
 * responde con JSON generado por [MockMonitorApi] y nunca sale a internet.
 * Todo lo demás es real: Retrofit, HTTP, serialización JSON y errores de red.
 * Para conectarse a un servidor de verdad basta con quitar este interceptor y
 * cambiar [ClienteApi.URL_BASE].
 *
 * @param hayRed si el teléfono tiene conexión. Sin red, falla como fallaría un servidor real.
 * @param sinConexionForzada modo demo para mostrar el comportamiento offline sin tocar el teléfono.
 */
class ServidorSimulado(
    private val hayRed: () -> Boolean,
    private val sinConexionForzada: () -> Boolean = { false },
    private val latenciaMs: Long = 300
) : Interceptor {

    private val json = Json

    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        if (!hayRed() || sinConexionForzada()) throw IOException("Sin conexión con el sistema de monitoreo")
        if (latenciaMs > 0) Thread.sleep(latenciaMs)   // una red real nunca responde al instante

        val ruta = peticion.url.encodedPath.removePrefix(RUTA_BASE)
        val metodo = peticion.method
        val cuerpo: String? = when {
            metodo == "GET" && ruta == "galpones" ->
                json.encodeToString(MockMonitorApi.getGalpones().map { GalponDto(it.id, it.granja, it.nombre, it.estado.name) })

            metodo == "GET" && RUTA_MEDICIONES.matches(ruta) -> {
                val id = RUTA_MEDICIONES.find(ruta)!!.groupValues[1].toInt()
                if (MockMonitorApi.getGalpones().none { it.id == id }) null
                else json.encodeToString(
                    MockMonitorApi.getMediciones(id).map { MedicionDto(it.galponId, it.temperatura, it.humedad, it.fechaHora) }
                )
            }

            metodo == "GET" && ruta == "alertas/activas" ->
                json.encodeToString(MockMonitorApi.getAlertasActivas().map { AlertaDto(it.galponId, it.tipo, it.nivel.name, it.fechaHora) })

            metodo == "POST" && ruta == "simulaciones/pico-calor" ->
                json.encodeToString(SimulacionDto(MockMonitorApi.simularEventoCritico()))

            else -> null
        }

        val (codigo, texto, contenido) = if (cuerpo != null) Triple(200, "OK", cuerpo)
            else Triple(404, "Not Found", """{"error":"Ruta no encontrada: $metodo /$ruta"}""")

        return Response.Builder()
            .request(peticion)
            .protocol(Protocol.HTTP_1_1)
            .code(codigo)
            .message(texto)
            .body(contenido.toResponseBody(TIPO_JSON))
            .build()
    }

    private companion object {
        const val RUTA_BASE = "/api/v1/"
        val RUTA_MEDICIONES = Regex("""galpones/(\d+)/mediciones""")
        val TIPO_JSON = "application/json; charset=utf-8".toMediaType()
    }
}
