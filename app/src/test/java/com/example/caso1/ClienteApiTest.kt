package com.example.caso1

import com.example.caso1.data.api.ClienteApi
import com.example.caso1.data.api.InterceptorConectividad
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/**
 * Cliente HTTP contra un servidor externo (json-server): la URL base es configurable
 * y la falta de red se detecta antes de intentar la petición.
 */
class ClienteApiTest {

    /** Hace de servidor: registra la URL pedida y responde una lista vacía. */
    private class ServidorQueRegistra : Interceptor {
        val urls = mutableListOf<String>()
        override fun intercept(chain: Interceptor.Chain): Response {
            urls += chain.request().url.toString()
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("[]".toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    @Test fun usaLaUrlBaseConfigurada() = runTest {
        val servidor = ServidorQueRegistra()
        val api = ClienteApi.crear(servidor, urlBase = "http://10.0.2.2:3000/api/v1/")

        api.mediciones(3)

        assertEquals(listOf("http://10.0.2.2:3000/api/v1/galpones/3/mediciones"), servidor.urls)
    }

    @Test fun sinRedFallaSinLlegarAlServidor() = runTest {
        val servidor = ServidorQueRegistra()
        val api = ClienteApi.crear(
            InterceptorConectividad(hayRed = { false }),
            servidor,
            urlBase = "http://10.0.2.2:3000/api/v1/"
        )

        try {
            api.galpones()
            fail("Sin red debería lanzar IOException")
        } catch (e: IOException) {
            assertEquals(emptyList<String>(), servidor.urls)
        }
    }

    @Test fun modoSinConexionForzadaFallaAunqueHayaRed() = runTest {
        val servidor = ServidorQueRegistra()
        val api = ClienteApi.crear(
            InterceptorConectividad(hayRed = { true }, sinConexionForzada = { true }),
            servidor,
            urlBase = "http://10.0.2.2:3000/api/v1/"
        )

        try {
            api.alertasActivas()
            fail("Con el modo sin conexión activo debería lanzar IOException")
        } catch (e: IOException) {
            assertEquals(emptyList<String>(), servidor.urls)
        }
    }

    @Test fun conRedLaPeticionLlegaAlServidor() = runTest {
        val servidor = ServidorQueRegistra()
        val api = ClienteApi.crear(
            InterceptorConectividad(hayRed = { true }),
            servidor,
            urlBase = "http://10.0.2.2:3000/api/v1/"
        )

        api.galpones()

        assertEquals(listOf("http://10.0.2.2:3000/api/v1/galpones"), servidor.urls)
    }
}
