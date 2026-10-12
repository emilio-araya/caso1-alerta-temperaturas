package com.example.caso1.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Conectividad del teléfono (punto 3.3: conectividad limitada o intermitente).
 * Se usa para fallar rápido sin red y para volver a sincronizar cuando la conexión regresa.
 */
class MonitorConectividad(contexto: Context) {
    private val cm = contexto.getSystemService(ConnectivityManager::class.java)

    fun hayRed(): Boolean {
        val capacidades = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Emite true/false cada vez que el teléfono gana o pierde la conexión. */
    val cambios: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(hayRed()) }
        }
        trySend(hayRed())
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
