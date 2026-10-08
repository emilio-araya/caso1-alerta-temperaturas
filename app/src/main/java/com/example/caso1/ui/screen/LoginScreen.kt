package com.example.caso1.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.caso1.data.model.Rol

@Composable
fun LoginScreen(onLogin: (Rol) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Alerta Temprana", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Selecciona tu perfil para continuar")
        Spacer(Modifier.height(32.dp))

        Rol.entries.forEach { rol ->
            Button(
                onClick = { onLogin(rol) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(rol.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}
