package com.example.caso1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.caso1.notifications.NotificationHelper
import com.example.caso1.ui.navigation.AppNavigation
import com.example.caso1.ui.theme.AlertaTemperaturasTheme
import com.example.caso1.work.AlertasWorker

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {

    /** true cuando la app se abrió desde una notificación de alerta crítica. */
    private val abrirAlertas = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirPermisoNotificaciones()
        AlertasWorker.programar(this)
        leerIntent(intent)
        setContent {
            AlertaTemperaturasTheme {
                // Se recalcula al rotar o redimensionar la ventana
                val tamano = calculateWindowSizeClass(this)
                AppNavigation(
                    anchoVentana = tamano.widthSizeClass,
                    abrirAlertas = abrirAlertas.value,
                    onAlertasAbiertas = { abrirAlertas.value = false }
                )
            }
        }
    }

    // La app ya estaba abierta y se tocó una notificación (launchMode singleTop)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerIntent(intent)
    }

    private fun leerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_ABRIR_ALERTAS, false) == true) {
            abrirAlertas.value = true
            intent.removeExtra(NotificationHelper.EXTRA_ABRIR_ALERTAS)
        }
    }

    /** Desde Android 13 (API 33) el permiso de notificaciones se pide en tiempo de ejecución. */
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val concedido = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!concedido) permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val permisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
}
