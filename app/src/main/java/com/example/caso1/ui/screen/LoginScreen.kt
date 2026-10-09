package com.example.caso1.ui.screen

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.caso1.R
import com.example.caso1.data.model.Rol
import com.example.caso1.ui.theme.LocalPaletaEstados
import com.example.caso1.ui.theme.Marino

@Composable
fun LoginScreen(onLogin: (Rol) -> Unit) {
    val paleta = LocalPaletaEstados.current
    // El encabezado es azul marino en ambos temas: íconos claros en la barra de estado mientras se ve el login
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        DisposableEffect(Unit) {
            val ventana = (vista.context as Activity).window
            val control = WindowCompat.getInsetsController(ventana, vista)
            val antes = control.isAppearanceLightStatusBars
            control.isAppearanceLightStatusBars = false
            onDispose { control.isAppearanceLightStatusBars = antes }
        }
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState())
    ) {
        // Encabezado institucional con la escala de alerta como marca
        // Azul marino en ambos temas: es la identidad de la app, no un color de superficie
        Surface(color = Marino, contentColor = Color.White) {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 28.dp)
            ) {
                Icono(R.drawable.ic_thermostat, null, Modifier.size(40.dp))
                Spacer(Modifier.height(20.dp))
                Text("Alerta Temperaturas", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(4.dp))
                Text("Monitoreo de galpones y alerta temprana por calor", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(Modifier.fillMaxWidth().height(6.dp)) {
            listOf(paleta.normal, paleta.advertencia, paleta.critico).forEach {
                Box(Modifier.weight(1f).fillMaxHeight().background(it.franja))
            }
        }

        Column(
            Modifier.widthIn(max = 560.dp).align(Alignment.CenterHorizontally).padding(24.dp)
        ) {
            Text("¿Con qué perfil ingresas?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Rol.entries.forEach { rol ->
                OutlinedCard(
                    onClick = { onLogin(rol) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icono(rol.icono(), null, Modifier.size(24.dp), MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rol.etiqueta(), style = MaterialTheme.typography.titleMedium)
                            Text(rol.descripcion(), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icono(R.drawable.ic_chevron_right, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Datos simulados · Proyecto académico DSY1105, Duoc UC",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}
