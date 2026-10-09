package com.example.caso1.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.viewmodel.MIN_DESCRIPCION
import com.example.caso1.viewmodel.RegistroAccionViewModel
import com.example.caso1.viewmodel.TIPOS_ACCION

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistroAccionScreen(
    viewModel: RegistroAccionViewModel = viewModel(),
    onBack: () -> Unit,
    onRegistrado: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val galpones by viewModel.galpones.collectAsStateWithLifecycle()
    val errores = state.errores

    LaunchedEffect(state.registrado) {
        if (state.registrado) {
            viewModel.navegacionRealizada()
            onRegistrado()
        }
    }

    Scaffold(topBar = { BarraSecundaria("Registrar acción", onBack) }) { padding ->
        // Desplazable y respetando el teclado: en pantallas pequeñas el botón quedaba tapado
        Column(
            Modifier.padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 640.dp)
                .padding(16.dp)
        ) {
            Seccion("Galpón", errores.galpon)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(
                    1.dp,
                    if (errores.galpon != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(Modifier.selectableGroup()) {
                    galpones.forEachIndexed { i, g ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(
                                    selected = state.galponId == g.id,
                                    onClick = { viewModel.onGalponChange(g.id) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.galponId == g.id, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.nombre, style = MaterialTheme.typography.titleSmall)
                                Text(g.granja, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            MarcaEstado(g.estado)
                            Spacer(Modifier.width(6.dp))
                            Text(g.estado.etiqueta(), style = MaterialTheme.typography.labelMedium,
                                color = colorCifra(g.estado, MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Seccion("Tipo de acción", errores.tipo)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TIPOS_ACCION.forEach { tipo ->
                    val elegido = state.tipoAccion == tipo
                    FilterChip(
                        selected = elegido,
                        onClick = { viewModel.onTipoChange(tipo) },
                        label = { Text(tipo) },
                        leadingIcon = if (elegido) {
                            { Icono(R.drawable.ic_check_circle, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Seccion("Qué hiciste", null)
            OutlinedTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                placeholder = { Text("Ej.: Encendí los ventiladores del sector norte") },
                isError = errores.descripcion != null,
                supportingText = {
                    Text(errores.descripcion ?: "${state.descripcion.trim().length} caracteres · mínimo $MIN_DESCRIPCION")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::registrar,
                enabled = !state.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.guardando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Text("Registrar acción")
                }
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, error: String?) {
    Text(titulo, style = MaterialTheme.typography.titleMedium)
    if (error != null) {
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icono(R.drawable.ic_warning, null, Modifier.size(16.dp), MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(4.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
    Spacer(Modifier.height(8.dp))
}
