package com.example.caso1.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.viewmodel.RegistroAccionViewModel

val TIPOS_ACCION = listOf("Ventilación", "Revisión de equipos", "Notificación a supervisor", "Otro")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroAccionScreen(
    viewModel: RegistroAccionViewModel = viewModel(),
    onRegistrado: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.registrado) {
        if (state.registrado) {
            viewModel.navegacionRealizada()
            onRegistrado()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Registrar acción") }) }) { padding ->
        // Desplazable y respetando el teclado: en pantallas pequeñas el botón quedaba tapado
        Column(
            Modifier.padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.galponId,
                onValueChange = viewModel::onGalponIdChange,
                label = { Text("ID del galpón") },
                isError = state.errorGalpon != null,
                modifier = Modifier.fillMaxWidth()
            )
            state.errorGalpon?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Spacer(Modifier.height(12.dp))

            Text("Tipo de acción", style = MaterialTheme.typography.labelLarge)
            TIPOS_ACCION.forEach { tipo ->
                // Toda la fila es seleccionable (no solo el círculo)
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = state.tipoAccion == tipo,
                        onClick = { viewModel.onTipoChange(tipo) },
                        role = Role.RadioButton
                    ).padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = state.tipoAccion == tipo, onClick = null)
                    Text(tipo)
                }
            }
            state.errorTipo?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = { Text("Descripción de la acción") },
                isError = state.errorDescripcion != null,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            state.errorDescripcion?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Spacer(Modifier.height(16.dp))

            Button(onClick = viewModel::registrar, modifier = Modifier.fillMaxWidth()) {
                Text("Registrar")
            }
        }
    }
}
