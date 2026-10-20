package com.example.caso1.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.viewmodel.CampoSimulacion
import com.example.caso1.viewmodel.SimulacionViewModel

/**
 * Formulario "Simular medición" (todos los perfiles). La validación vive en el ViewModel
 * ([com.example.caso1.viewmodel.validarSimulacion]); aquí solo se muestra cada error junto a su campo.
 */
@Composable
fun SimulacionScreen(
    onBack: () -> Unit,
    viewModel: SimulacionViewModel = viewModel(factory = SimulacionViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val galpones by viewModel.galpones.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val recursos = LocalResources.current

    LaunchedEffect(state.resultado) {
        state.resultado?.let {
            snackbar.showSnackbar(recursos.getString(R.string.simulacion_resultado, recursos.getString(it.etiquetaRes())))
            viewModel.resultadoMostrado()
        }
    }

    Scaffold(
        topBar = { BarraSecundaria(stringResource(R.string.simulacion_titulo), onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        // Desplazable y respetando el teclado, igual que los otros formularios
        Column(
            Modifier.padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 640.dp)
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.simulacion_explicacion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Selector de galpón: mismo patrón que el registro de acciones
            Spacer(Modifier.height(24.dp))
            val errorGalpon = state.errores[CampoSimulacion.GALPON]
            Text(stringResource(R.string.simulacion_galpon), style = MaterialTheme.typography.titleMedium)
            errorGalpon?.let {
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icono(R.drawable.ic_warning, null, Modifier.size(16.dp), MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(
                    1.dp,
                    if (errorGalpon != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
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

            Spacer(Modifier.height(16.dp))
            CampoMedicion(
                R.string.simulacion_temperatura, R.string.umbrales_unidad_temperatura,
                state.temperatura, state.errores[CampoSimulacion.TEMPERATURA], viewModel::onTemperaturaChange
            )
            CampoMedicion(
                R.string.simulacion_humedad, R.string.umbrales_unidad_humedad,
                state.humedad, state.errores[CampoSimulacion.HUMEDAD], viewModel::onHumedadChange
            )

            // Vista previa con los umbrales guardados: se ve el resultado antes de simular
            state.vistaPrevia?.let { e ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarcaEstado(e)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.simulacion_vista_previa, e.etiqueta()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorCifra(e)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::simular,
                enabled = !state.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.guardando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Text(stringResource(R.string.simulacion_boton))
                }
            }
        }
    }
}

/** Campo numérico con unidad; en rojo, con ícono y mensaje cuando no es válido. */
@Composable
private fun CampoMedicion(
    @StringRes etiqueta: Int, @StringRes unidad: Int, valor: String, @StringRes error: Int?, onCambio: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(stringResource(etiqueta)) },
        suffix = { Text(stringResource(unidad)) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        trailingIcon = error?.let {
            { Icono(R.drawable.ic_warning, stringResource(R.string.umbrales_error_icono), tint = MaterialTheme.colorScheme.error) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}
