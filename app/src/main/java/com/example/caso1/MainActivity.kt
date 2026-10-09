package com.example.caso1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.caso1.ui.theme.AlertaTemperaturasTheme
import com.example.caso1.work.AlertasWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirPermisoNotificaciones()
        AlertasWorker.programar(this)
        setContent {
            AlertaTemperaturasTheme {
                com.example.caso1.ui.navigation.AppNavigation()
            }
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

@Preview(showBackground = true)
@Composable
fun PreviewApp() {
    AlertaTemperaturasTheme {
        Text("Preview")
    }
}
