package com.example.caso1.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.caso1.viewmodel.RegistroAccionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmacionScreen(viewModel: RegistroAccionViewModel, onVolver: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Acción registrada") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("✅ La acción se guardó correctamente", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Text("Galpón: ${state.galponId}")
            Text("Tipo: ${state.tipoAccion}")
            Text("Descripción: ${state.descripcion}")
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                viewModel.limpiar()
                onVolver()
            }) { Text("Volver al inicio") }
        }
    }
}
